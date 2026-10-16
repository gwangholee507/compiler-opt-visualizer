package com.compareopt.optvisualizer.toolchain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 컴파일러를 실행하지 않고 명령줄 조립과 버전 출력 판정만 검증한다.
 * 실제로 컴파일되는지는 CompilerServiceTest에서 확인한다.
 */
class GccStyleToolchainTest {

    private final Toolchain clang = ToolchainKind.CLANG.toolchain();
    private final Toolchain gcc = ToolchainKind.GCC.toolchain();

    @Test
    void 레벨은_O0부터_O3까지_순서대로다() {
        assertThat(clang.levels()).containsExactly("O0", "O1", "O2", "O3");
        assertThat(gcc.levels()).containsExactly("O0", "O1", "O2", "O3");
    }

    @Test
    void 어셈블리_오브젝트_실행파일_인자를_GCC식으로_만든다() {
        assertThat(clang.assemblyArgs("O2", "input.c", "out-O2.s"))
                .containsExactly("-S", "-O2", "input.c", "-o", "out-O2.s");
        assertThat(clang.objectArgs("O2", "input.c", "out-O2.o"))
                .containsExactly("-c", "-O2", "input.c", "-o", "out-O2.o");
        assertThat(clang.executableArgs("O2", "input.c", "out-O2.exe"))
                .containsExactly("-O2", "input.c", "-o", "out-O2.exe");
    }

    @Test
    void 설치_확인은_version_인자로_한다() {
        assertThat(clang.versionArgs()).containsExactly("--version");
        assertThat(gcc.versionArgs()).containsExactly("--version");
    }

    @Test
    void gcc_계열은_clang의_별칭을_GCC로_인정하지_않는다() {
        // macOS의 /usr/bin/gcc --version 출력
        String appleClang = "Apple clang version 17.0.0 (clang-1700.0.13.3)\nTarget: arm64-apple-darwin25.6.0";

        assertThat(gcc.versionMismatch(appleClang)).isPresent();
        assertThat(gcc.versionMismatch("gcc-16 (Homebrew GCC 16.1.0) 16.1.0")).isEmpty();
    }

    @Test
    void clang_계열은_clang_출력을_그대로_인정한다() {
        assertThat(clang.versionMismatch("Apple clang version 17.0.0 (clang-1700.0.13.3)")).isEmpty();
        assertThat(clang.versionMismatch("Ubuntu clang version 18.1.3")).isEmpty();
    }
}
