package com.compareopt.optvisualizer.service;

import com.compareopt.optvisualizer.dto.OptimizationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * clang을 서브프로세스로 호출해 최적화 레벨(O0~O3)별로
 * 어셈블리와 바이너리 크기를 뽑아내는 서비스.
 *
 * 주의: 지금은 로컬 개인 프로젝트 단계라 별도 샌드박싱(Docker 등) 없이
 * 서버 프로세스 권한으로 직접 컴파일을 실행합니다.
 * 외부에 배포할 계획이 생기면 반드시 컨테이너 격리 + 리소스 제한을 추가해야 합니다.
 */
@Service
public class CompilerService {

    private static final Logger log = LoggerFactory.getLogger(CompilerService.class);
    private static final List<String> LEVELS = List.of("O0", "O1", "O2", "O3");
    private static final long TIMEOUT_SECONDS = 10;

    @Value("${compiler.path:clang}")
    private String compilerPath;

    public List<OptimizationResult> compareOptimizationLevels(String sourceCode) {
        List<OptimizationResult> results = new ArrayList<>();
        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("opt-visualizer-");
            Path sourceFile = workDir.resolve("input.c");
            Files.writeString(sourceFile, sourceCode, StandardCharsets.UTF_8);

            for (String level : LEVELS) {
                results.add(runOneLevel(workDir, sourceFile, level));
            }
        } catch (IOException e) {
            log.error("임시 작업 디렉터리 생성 실패", e);
            for (String level : LEVELS) {
                results.add(OptimizationResult.failure(level, "서버 내부 오류: " + e.getMessage(), 0));
            }
        } finally {
            cleanup(workDir);
        }
        return results;
    }

    private OptimizationResult runOneLevel(Path workDir, Path sourceFile, String level) {
        long start = System.currentTimeMillis();
        Path asmFile = workDir.resolve("out-" + level + ".s");
        Path objFile = workDir.resolve("out-" + level + ".o");

        // 1) 어셈블리 생성: clang -S -O{n} input.c -o out.s
        ProcessResult asmResult = run(List.of(
                compilerPath, "-S", "-" + level, sourceFile.toString(), "-o", asmFile.toString()
        ));
        long elapsed = System.currentTimeMillis() - start;

        if (asmResult.exitCode != 0) {
            return OptimizationResult.failure(level, asmResult.stderr, elapsed);
        }

        // 2) 오브젝트 파일 생성 (크기 비교용): clang -c -O{n} input.c -o out.o
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
        return OptimizationResult.success(level, assembly, binarySize, totalElapsed);
    }

    private ProcessResult run(List<String> command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);
            Process process = pb.start();

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new ProcessResult(-1, "컴파일 시간 초과 (" + TIMEOUT_SECONDS + "초)");
            }

            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            return new ProcessResult(process.exitValue(), stderr);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ProcessResult(-1, "프로세스 실행 실패: " + e.getMessage());
        }
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
}
