package com.compareopt.optvisualizer.config;

import com.compareopt.optvisualizer.platform.HostOs;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 application.yml이 CompilerProperties로 제대로 바인딩되는지, 그리고 OS별 후보 선택이
 * 의도대로 되는지 검증한다. 컴파일러를 실행하지 않으므로 어느 OS에서 돌려도 Windows용 설정까지 확인할 수 있다.
 */
@SpringJUnitConfig(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(CompilerProperties.class)
// 테스트를 돌리는 사람의 환경 변수(OPTVIS_CLANG 등)에 결과가 좌우되지 않도록 비워둔다.
@TestPropertySource(properties = {"OPTVIS_CLANG=", "OPTVIS_GCC="})
class CompilerPropertiesTest {

    @Autowired
    private CompilerProperties properties;

    @Test
    void application_yml의_컴파일러_목록이_바인딩된다() {
        assertThat(properties.getOptions()).extracting(CompilerProperties.Option::getId)
                .containsExactly("clang", "gcc");
    }

    @Test
    void 환경_변수로_경로를_지정하지_않으면_빈_후보는_건너뛴다() {
        // candidates의 첫 항목 "${OPTVIS_CLANG:}"는 환경 변수가 없으면 빈 문자열이 된다.
        assertThat(option("clang").candidatesFor(HostOs.LINUX)).containsExactly("clang");
        assertThat(option("gcc").candidatesFor(HostOs.MACOS)).doesNotContain("").endsWith("gcc");
    }

    @Test
    void Windows에서는_PATH에_없는_기본_설치_위치까지_후보에_포함한다() {
        List<String> clang = option("clang").candidatesFor(HostOs.WINDOWS);
        List<String> gcc = option("gcc").candidatesFor(HostOs.WINDOWS);

        // PATH에 있는 이름을 먼저 시도하고, 그다음에 기본 설치 위치를 시도한다.
        assertThat(clang).startsWith("clang");
        assertThat(clang).contains("C:\\msys64\\ucrt64\\bin\\clang.exe", "C:\\Program Files\\LLVM\\bin\\clang.exe");
        assertThat(gcc).contains("C:\\msys64\\ucrt64\\bin\\gcc.exe");
    }

    @Test
    void 다른_OS에서는_Windows_전용_후보를_시도하지_않는다() {
        assertThat(option("clang").candidatesFor(HostOs.MACOS)).noneMatch(c -> c.startsWith("C:"));
        assertThat(option("gcc").candidatesFor(HostOs.LINUX)).noneMatch(c -> c.startsWith("C:"));
    }

    @Test
    void 공통_후보_뒤에_그_OS_전용_후보를_붙인다() {
        CompilerProperties.Option option = new CompilerProperties.Option();
        option.setCandidates(List.of("", "  ", "cc"));
        option.setOsCandidates(Map.of("windows", List.of("C:\\tools\\cc.exe"), "linux", List.of("/opt/cc/bin/cc")));

        assertThat(option.candidatesFor(HostOs.WINDOWS)).containsExactly("cc", "C:\\tools\\cc.exe");
        assertThat(option.candidatesFor(HostOs.LINUX)).containsExactly("cc", "/opt/cc/bin/cc");
        assertThat(option.candidatesFor(HostOs.MACOS)).containsExactly("cc");
    }

    private CompilerProperties.Option option(String id) {
        return properties.getOptions().stream()
                .filter(o -> o.getId().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
