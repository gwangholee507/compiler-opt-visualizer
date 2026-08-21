package com.compareopt.optvisualizer.service;

import com.compareopt.optvisualizer.dto.OptimizationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 선택된 컴파일러(clang/gcc 등)를 서브프로세스로 호출해 최적화 레벨(O0~O3)별로
 * 어셈블리와 바이너리 크기, (옵트인 시) 실행 시간을 뽑아내는 서비스.
 *
 * 주의: 지금은 로컬 개인 프로젝트 단계라 별도 샌드박싱(Docker 등) 없이
 * 서버 프로세스 권한으로 직접 컴파일/실행을 수행합니다.
 * 특히 runBenchmark=true인 경우 사용자가 입력한 코드로 만들어진 바이너리를
 * 그대로 실행하므로 컴파일만 할 때보다 위험도가 높습니다.
 * 외부에 배포할 계획이 생기면 반드시 컨테이너 격리 + 리소스 제한을 추가해야 합니다.
 */
@Service
public class CompilerService {

    private static final Logger log = LoggerFactory.getLogger(CompilerService.class);
    private static final List<String> LEVELS = List.of("O0", "O1", "O2", "O3");
    private static final long COMPILE_TIMEOUT_SECONDS = 10;
    private static final long EXECUTION_TIMEOUT_SECONDS = 5;
    private static final int BENCHMARK_RUNS = 5;

    private final CompilerRegistry compilerRegistry;

    public CompilerService(CompilerRegistry compilerRegistry) {
        this.compilerRegistry = compilerRegistry;
    }

    public List<OptimizationResult> compareOptimizationLevels(String sourceCode, String compilerId, boolean runBenchmark) {
        CompilerRegistry.ResolvedCompiler compiler = compilerRegistry.resolve(compilerId);

        List<OptimizationResult> results = new ArrayList<>();
        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("opt-visualizer-");
            Path sourceFile = workDir.resolve("input.c");
            Files.writeString(sourceFile, sourceCode, StandardCharsets.UTF_8);

            for (String level : LEVELS) {
                OptimizationResult result = runOneLevel(workDir, sourceFile, level, compiler, runBenchmark);
                result.setCompilerInfo(compiler.id(), compiler.label());
                results.add(result);
            }
        } catch (IOException e) {
            log.error("임시 작업 디렉터리 생성 실패", e);
            for (String level : LEVELS) {
                OptimizationResult failure = OptimizationResult.failure(level, "서버 내부 오류: " + e.getMessage(), 0);
                failure.setCompilerInfo(compiler.id(), compiler.label());
                results.add(failure);
            }
        } finally {
            cleanup(workDir);
        }
        return results;
    }

    private OptimizationResult runOneLevel(Path workDir, Path sourceFile, String level,
                                            CompilerRegistry.ResolvedCompiler compiler, boolean runBenchmark) {
        long start = System.currentTimeMillis();
        Path asmFile = workDir.resolve("out-" + level + ".s");
        Path objFile = workDir.resolve("out-" + level + ".o");
        String compilerPath = compiler.path();

        // 1) 어셈블리 생성: {compiler} -S -O{n} input.c -o out.s
        ProcessResult asmResult = run(List.of(
                compilerPath, "-S", "-" + level, sourceFile.toString(), "-o", asmFile.toString()
        ));
        long elapsed = System.currentTimeMillis() - start;

        if (asmResult.exitCode != 0) {
            return OptimizationResult.failure(level, sanitizeStderr(asmResult.stderr, sourceFile), elapsed);
        }

        // 2) 오브젝트 파일 생성 (크기 비교용): {compiler} -c -O{n} input.c -o out.o
        //    main() 유무와 무관하게 컴파일만 하면 되므로 링크(-o 실행 파일)가 아니라 -c를 사용.
        ProcessResult objResult = run(List.of(
                compilerPath, "-c", "-" + level, sourceFile.toString(), "-o", objFile.toString()
        ));

        long binarySize = 0;
        try {
            if (objResult.exitCode == 0 && Files.exists(objFile)) {
                binarySize = Files.size(objFile);
            }
        } catch (IOException e) {
            log.warn("오브젝트 파일 크기 조회 실패: {}", e.getMessage());
        }

        String assembly;
        try {
            assembly = Files.readString(asmFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return OptimizationResult.failure(level, "어셈블리 파일 읽기 실패: " + e.getMessage(), elapsed);
        }

        long totalElapsed = System.currentTimeMillis() - start;
        OptimizationResult result = OptimizationResult.success(level, assembly, binarySize, totalElapsed);

        if (runBenchmark) {
            ExecutionOutcome outcome = benchmarkExecution(workDir, sourceFile, level, compilerPath);
            result.setExecutionResult(outcome.timeMs, outcome.error);
        }

        return result;
    }

    /**
     * 소스를 실행 파일로 링크해서 여러 번 실행하고 최솟값(가장 노이즈가 적은 값)을 실행 시간으로 채택한다.
     * main()이 없는 코드는 링크에 실패하므로 이 경우 에러 메시지만 채우고 조용히 넘어간다
     * (컴파일 자체는 성공했으므로 전체 결과를 실패로 처리하지 않음).
     */
    private ExecutionOutcome benchmarkExecution(Path workDir, Path sourceFile, String level, String compilerPath) {
        Path exeFile = workDir.resolve("out-" + level + ".exe");

        ProcessResult linkResult = run(List.of(
                compilerPath, "-" + level, sourceFile.toString(), "-o", exeFile.toString()
        ));

        if (linkResult.exitCode != 0 || !Files.exists(exeFile)) {
            return new ExecutionOutcome(null, "실행 파일을 만들 수 없습니다 (main 함수가 없을 수 있습니다)");
        }

        try {
            long bestNanos = Long.MAX_VALUE;
            for (int i = 0; i < BENCHMARK_RUNS; i++) {
                Long elapsedNanos = runOnceAndTime(exeFile);
                if (elapsedNanos == null) {
                    return new ExecutionOutcome(null, "실행 시간 초과 (" + EXECUTION_TIMEOUT_SECONDS + "초)");
                }
                bestNanos = Math.min(bestNanos, elapsedNanos);
            }
            return new ExecutionOutcome(bestNanos / 1_000_000.0, null);
        } catch (IOException e) {
            return new ExecutionOutcome(null, "실행 실패: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ExecutionOutcome(null, "실행이 중단되었습니다.");
        }
    }

    /** 실행 파일을 한 번 실행하고 걸린 시간(나노초)을 반환. 타임아웃이면 null. */
    private Long runOnceAndTime(Path exeFile) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(exeFile.toString());
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);

        long t0 = System.nanoTime();
        Process process = pb.start();
        boolean finished = process.waitFor(EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        long elapsed = System.nanoTime() - t0;

        if (!finished) {
            process.destroyForcibly();
            return null;
        }
        return elapsed;
    }

    private ProcessResult run(List<String> command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);
            Process process = pb.start();

            boolean finished = process.waitFor(COMPILE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new ProcessResult(-1, "컴파일 시간 초과 (" + COMPILE_TIMEOUT_SECONDS + "초)");
            }

            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            return new ProcessResult(process.exitValue(), stderr);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ProcessResult(-1, "프로세스 실행 실패: " + e.getMessage());
        }
    }

    /**
     * 컴파일러의 에러 메시지에 그대로 찍히는 서버 임시 디렉터리의 절대 경로를
     * 사용자에게 의미 없는 정보이므로 "input.c"로 바꿔서 보여준다.
     */
    private String sanitizeStderr(String stderr, Path sourceFile) {
        if (stderr == null) return stderr;
        return stderr.replace(sourceFile.toString(), "input.c");
    }

    private void cleanup(Path workDir) {
        if (workDir == null) return;
        try (var stream = Files.walk(workDir)) {
            stream.sorted((a, b) -> b.compareTo(a)) // 파일 먼저, 디렉터리 나중에 삭제
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException e) {
            log.warn("임시 디렉터리 정리 실패: {}", e.getMessage());
        }
    }

    private record ProcessResult(int exitCode, String stderr) {
    }

    private record ExecutionOutcome(Double timeMs, String error) {
    }
}
