package com.compareopt.optvisualizer.service;

import com.compareopt.optvisualizer.config.CompilerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * application.yml에 등록된 컴파일러 후보들 중 이 서버에서 실제로 실행 가능한 것을
 * 서버 시작 시 한 번 탐지해두는 레지스트리.
 *
 * 사용자가 요청 바디에 넘긴 컴파일러 id를 그대로 실행 파일 경로로 쓰지 않고
 * 반드시 이 레지스트리를 통해서만 실제 경로로 변환한다 — 임의 문자열이 그대로
 * ProcessBuilder 커맨드로 흘러들어가는 것(명령어 인젝션)을 막기 위한 화이트리스트.
 */
@Component
public class CompilerRegistry {

    private static final Logger log = LoggerFactory.getLogger(CompilerRegistry.class);
    private static final long PROBE_TIMEOUT_SECONDS = 3;

    private final List<ResolvedCompiler> compilers = new ArrayList<>();

    public CompilerRegistry(CompilerProperties properties) {
        for (CompilerProperties.Option option : properties.getOptions()) {
            String resolvedPath = probeCandidates(option.getId(), option.getCandidates());
            boolean available = resolvedPath != null;
            if (!available) {
                log.warn("컴파일러 '{}'({})를 찾을 수 없습니다. 후보: {}", option.getId(), option.getLabel(), option.getCandidates());
            }
            compilers.add(new ResolvedCompiler(option.getId(), option.getLabel(), resolvedPath, available));
        }
    }

    /** 후보 실행 파일 이름들을 순서대로 시도해서 처음으로 동작하는 것의 경로를 반환. 다 실패하면 null. */
    private String probeCandidates(String id, List<String> candidates) {
        for (String candidate : candidates) {
            if (canRun(id, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * `{executable} --version`을 실행해보고, 종료 코드가 0인지 + (id가 "gcc"일 때는) 출력이
     * 진짜 GNU GCC인지까지 확인한다.
     *
     * macOS는 `/usr/bin/gcc`가 사실 Apple clang의 별칭이라 `gcc --version`이 정상 종료되면서
     * "Apple clang version ..."을 출력한다. 그대로 두면 id="gcc"인데 실제로는 clang이 실행되는
     * 오탐이 생기므로, gcc 후보는 출력에 "clang"이 없어야만 인정한다.
     */
    private boolean canRun(String id, String executable) {
        try {
            Process process = new ProcessBuilder(executable, "--version")
                    .redirectErrorStream(true)
                    .start();
            String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            boolean finished = process.waitFor(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            if (process.exitValue() != 0) {
                return false;
            }
            if ("gcc".equals(id) && output.toLowerCase().contains("clang")) {
                log.warn("'{}'는 GNU GCC가 아니라 clang의 별칭으로 보여 건너뜁니다.", executable);
                return false;
            }
            return true;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public List<ResolvedCompiler> listAll() {
        return List.copyOf(compilers);
    }

    /** id로 사용 가능한 컴파일러를 찾는다. 없거나 사용 불가능하면 예외를 던진다 (컨트롤러에서 400으로 변환됨). */
    public ResolvedCompiler resolve(String id) {
        Optional<ResolvedCompiler> found = compilers.stream()
                .filter(c -> c.id().equals(id))
                .findFirst();

        if (found.isEmpty()) {
            throw new IllegalArgumentException("알 수 없는 컴파일러입니다: " + id);
        }
        ResolvedCompiler compiler = found.get();
        if (!compiler.available()) {
            throw new IllegalArgumentException("'" + compiler.label() + "'는 서버에 설치되어 있지 않습니다.");
        }
        return compiler;
    }

    public record ResolvedCompiler(String id, String label, String path, boolean available) {
    }
}
