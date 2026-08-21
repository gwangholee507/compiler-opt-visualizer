package com.compareopt.optvisualizer.service;

import com.compareopt.optvisualizer.config.CompilerProperties;
import com.compareopt.optvisualizer.dto.OptimizationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * CompilerService는 실제 컴파일러를 서브프로세스로 호출하는 통합 성격의 테스트다.
 * (mock 없이 순수 단위 테스트로 만들기엔 핵심 로직이 전부 "외부 프로세스를 어떻게 다루는가"라서
 *  실제 컴파일 결과를 검증하는 게 더 의미 있다고 판단함)
 *
 * 로컬(macOS)에 clang이 설치되어 있어야 통과한다. CI에서도 ubuntu-latest에 clang/gcc를 설치함.
 * gcc 관련 테스트는 서버에 실제 GNU GCC가 없으면 (macOS의 gcc는 clang 별칭이라 CompilerRegistry가
 * 걸러냄) assumeTrue로 건너뛴다.
 */
class CompilerServiceTest {

    private CompilerService compilerService;
    private CompilerRegistry registry;

    @BeforeEach
    void setUp() {
        CompilerProperties properties = new CompilerProperties();
        CompilerProperties.Option clang = new CompilerProperties.Option();
        clang.setId("clang");
        clang.setLabel("Clang (LLVM)");
        clang.setCandidates(List.of("clang"));

        CompilerProperties.Option gcc = new CompilerProperties.Option();
        gcc.setId("gcc");
        gcc.setLabel("GNU GCC");
        gcc.setCandidates(List.of("gcc-16", "gcc-15", "gcc-14", "gcc-13", "gcc-12", "gcc"));

        properties.setOptions(List.of(clang, gcc));
        registry = new CompilerRegistry(properties);
        compilerService = new CompilerService(registry);
    }

    @Test
    void 정상적인_C코드는_O0부터_O3까지_모두_컴파일에_성공한다() {
        String code = "int square(int x) { return x * x; }";

        List<OptimizationResult> results = compilerService.compareOptimizationLevels(code, "clang", false);

        assertThat(results).hasSize(4);
        assertThat(results).extracting(OptimizationResult::getLevel)
                .containsExactly("O0", "O1", "O2", "O3");
        assertThat(results).allSatisfy(r -> {
            assertThat(r.isSuccess()).isTrue();
            assertThat(r.getCompilerId()).isEqualTo("clang");
            // macOS(Mach-O)는 심볼 앞에 '_'가 붙지만(_square) Linux(ELF)는 안 붙는다(square).
            // 플랫폼에 상관없이 통과해야 하므로 언더스코어 없는 쪽으로 검증한다.
            assertThat(r.getAssembly()).contains("square");
            assertThat(r.getBinarySizeBytes()).isGreaterThan(0);
            assertThat(r.getExecutionTimeMs()).isNull(); // runBenchmark=false
        });
    }

    @Test
    void 최적화가_적용되면_오브젝트_파일_크기가_O0보다_커지지_않는다() {
        String code = """
                int square(int x) { return x * x; }
                int sum_loop(int n) {
                    int total = 0;
                    for (int i = 0; i < n; i++) { total += square(i); }
                    return total;
                }
                """;

        List<OptimizationResult> results = compilerService.compareOptimizationLevels(code, "clang", false);
        long o0Size = results.get(0).getBinarySizeBytes();

        assertThat(results).allSatisfy(r ->
                assertThat(r.getBinarySizeBytes()).isLessThanOrEqualTo(o0Size)
        );
    }

    @Test
    void 문법_오류가_있는_코드는_모든_레벨에서_실패하고_에러메시지에_임시경로가_노출되지_않는다() {
        String brokenCode = "int f(int x) { retrun x + ; }";

        List<OptimizationResult> results = compilerService.compareOptimizationLevels(brokenCode, "clang", false);

        assertThat(results).hasSize(4);
        assertThat(results).allSatisfy(r -> {
            assertThat(r.isSuccess()).isFalse();
            assertThat(r.getErrorMessage()).contains("input.c");
            assertThat(r.getErrorMessage()).doesNotContain("/var/folders");
            assertThat(r.getErrorMessage()).doesNotContain("/tmp");
        });
    }

    @Test
    void 존재하지_않는_컴파일러를_요청하면_예외가_발생한다() {
        assertThatThrownBy(() -> compilerService.compareOptimizationLevels("int f(){return 0;}", "msvc", false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void main이_있고_runBenchmark가_true면_실행_시간이_측정된다() {
        assumeTrue(registry.resolve("clang").available());
        String code = """
                int add(int a, int b) { return a + b; }
                int main(void) { return add(1, 2); }
                """;

        List<OptimizationResult> results = compilerService.compareOptimizationLevels(code, "clang", true);

        assertThat(results).allSatisfy(r -> {
            assertThat(r.isSuccess()).isTrue();
            assertThat(r.getExecutionTimeMs()).isNotNull();
            assertThat(r.getExecutionTimeMs()).isGreaterThanOrEqualTo(0.0);
            assertThat(r.getExecutionError()).isNull();
        });
    }

    @Test
    void main이_없는_코드는_runBenchmark가_true여도_컴파일은_성공하고_실행만_건너뛴다() {
        String code = "int square(int x) { return x * x; }";

        List<OptimizationResult> results = compilerService.compareOptimizationLevels(code, "clang", true);

        assertThat(results).allSatisfy(r -> {
            assertThat(r.isSuccess()).isTrue(); // 컴파일/어셈블리 자체는 성공
            assertThat(r.getExecutionTimeMs()).isNull();
            assertThat(r.getExecutionError()).isNotNull(); // 링크 실패 사유가 채워짐
        });
    }

    @Test
    void gcc가_설치되어_있으면_gcc로도_컴파일이_성공한다() {
        assumeTrue(isAvailable("gcc"));
        String code = "int square(int x) { return x * x; }";

        List<OptimizationResult> results = compilerService.compareOptimizationLevels(code, "gcc", false);

        assertThat(results).allSatisfy(r -> {
            assertThat(r.isSuccess()).isTrue();
            assertThat(r.getCompilerId()).isEqualTo("gcc");
            assertThat(r.getAssembly()).contains("square");
        });
    }

    private boolean isAvailable(String id) {
        try {
            return registry.resolve(id).available();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
