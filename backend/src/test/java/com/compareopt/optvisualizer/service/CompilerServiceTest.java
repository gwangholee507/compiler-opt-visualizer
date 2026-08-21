package com.compareopt.optvisualizer.service;

import com.compareopt.optvisualizer.dto.OptimizationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CompilerService는 실제 clang을 서브프로세스로 호출하는 통합 성격의 테스트다.
 * (mock 없이 순수 단위 테스트로 만들기엔 핵심 로직이 전부 "외부 프로세스를 어떻게 다루는가"라서
 *  실제 clang 실행 결과를 검증하는 게 더 의미 있다고 판단함)
 *
 * 로컬(macOS)에 clang이 설치되어 있어야 통과한다. CI에서도 ubuntu-latest에 clang이 기본 설치되어 있음.
 */
class CompilerServiceTest {

    private CompilerService compilerService;

    @BeforeEach
    void setUp() {
        compilerService = new CompilerService();
        // @Value로 주입되는 필드라 Spring 컨텍스트 없이 테스트할 때는 직접 넣어줘야 한다.
        ReflectionTestUtils.setField(compilerService, "compilerPath", "clang");
    }

    @Test
    void 정상적인_C코드는_O0부터_O3까지_모두_컴파일에_성공한다() {
        String code = "int square(int x) { return x * x; }";

        List<OptimizationResult> results = compilerService.compareOptimizationLevels(code);

        assertThat(results).hasSize(4);
        assertThat(results).extracting(OptimizationResult::getLevel)
                .containsExactly("O0", "O1", "O2", "O3");
        assertThat(results).allSatisfy(r -> {
            assertThat(r.isSuccess()).isTrue();
            assertThat(r.getAssembly()).contains("_square");
            assertThat(r.getBinarySizeBytes()).isGreaterThan(0);
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

        List<OptimizationResult> results = compilerService.compareOptimizationLevels(code);
        long o0Size = results.get(0).getBinarySizeBytes();

        assertThat(results).allSatisfy(r ->
                assertThat(r.getBinarySizeBytes()).isLessThanOrEqualTo(o0Size)
        );
    }

    @Test
    void 문법_오류가_있는_코드는_모든_레벨에서_실패하고_에러메시지에_임시경로가_노출되지_않는다() {
        String brokenCode = "int f(int x) { retrun x + ; }";

        List<OptimizationResult> results = compilerService.compareOptimizationLevels(brokenCode);

        assertThat(results).hasSize(4);
        assertThat(results).allSatisfy(r -> {
            assertThat(r.isSuccess()).isFalse();
            assertThat(r.getErrorMessage()).contains("input.c");
            assertThat(r.getErrorMessage()).doesNotContain("/var/folders");
            assertThat(r.getErrorMessage()).doesNotContain("/tmp");
        });
    }
}
