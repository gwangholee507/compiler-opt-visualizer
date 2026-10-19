package com.compareopt.optvisualizer.toolchain;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * clang과 GCC처럼 `-S`, `-c`, `-O2` 같은 GCC식 명령줄을 쓰는 컴파일러.
 * 언어와 상관없이 같은 규칙이라, 실행 파일만 다른 C++(clang++, g++)도 이 구현을 함께 쓸 수 있다.
 */
final class GccStyleToolchain implements Toolchain {

    private static final List<String> LEVELS = List.of("O0", "O1", "O2", "O3");

    private final boolean requireGnu;

    /**
     * @param requireGnu true면 진짜 GNU GCC만 인정한다 (clang의 별칭인 gcc는 건너뜀)
     */
    GccStyleToolchain(boolean requireGnu) {
        this.requireGnu = requireGnu;
    }

    @Override
    public List<String> levels() {
        return LEVELS;
    }

    @Override
    public List<String> versionArgs() {
        return List.of("--version");
    }

    /**
     * GNU GCC여야 하는 계열이면 버전 출력에 "clang"이 없어야 인정한다.
     *
     * macOS는 `/usr/bin/gcc`가 사실 Apple clang의 별칭이라 `gcc --version`이 정상 종료되면서
     * "Apple clang version ..."을 출력한다. 그대로 두면 GCC를 골랐는데 실제로는 clang이 실행되는
     * 오탐이 생긴다.
     */
    @Override
    public Optional<String> versionMismatch(String versionOutput) {
        if (requireGnu && versionOutput.toLowerCase(Locale.ROOT).contains("clang")) {
            return Optional.of("GNU GCC가 아니라 clang의 별칭으로 보입니다.");
        }
        return Optional.empty();
    }

    /** {compiler} -S -O{n} input.c -o out.s */
    @Override
    public List<String> assemblyArgs(String level, String sourceFile, String outputFile) {
        return List.of("-S", flag(level), sourceFile, "-o", outputFile);
    }

    /** {compiler} -c -O{n} input.c -o out.o — 링크하지 않으므로 main()이 없어도 된다. */
    @Override
    public List<String> objectArgs(String level, String sourceFile, String outputFile) {
        return List.of("-c", flag(level), sourceFile, "-o", outputFile);
    }

    /** {compiler} -O{n} input.c -o out.exe — 링크까지 하므로 main()이 있어야 성공한다. */
    @Override
    public List<String> executableArgs(String level, String sourceFile, String outputFile) {
        return List.of(flag(level), sourceFile, "-o", outputFile);
    }

    private static String flag(String level) {
        return "-" + level;
    }
}
