package com.compareopt.optvisualizer.service;

import com.compareopt.optvisualizer.config.CompilerProperties;
import com.compareopt.optvisualizer.platform.ProcessRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * CI가 필수로 지정한 컴파일러(OPTVIS_REQUIRED_COMPILERS)를 실제 application.yml 설정으로 모두 찾을 수 있는지 확인한다.
 * CompilerServiceTest는 PATH의 이름만 후보로 쓰므로, os-candidates(Windows의 기본 설치 위치 등)까지 포함한
 * 실제 탐지 경로는 여기서 확인한다. 환경 변수가 없는 로컬에서는 건너뛴다.
 */
@SpringJUnitConfig(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(CompilerProperties.class)
class RequiredCompilersTest {

    @Autowired
    private CompilerProperties properties;

    @Test
    void CI가_필수로_지정한_컴파일러는_application_yml_설정으로_모두_찾을_수_있다() {
        Set<String> required = RequiredCompilers.fromEnv();
        assumeFalse(required.isEmpty(), RequiredCompilers.ENV + "가 없어 건너뜁니다 (CI 전용 검사).");

        CompilerRegistry registry = new CompilerRegistry(properties, new ProcessRunner());

        assertThat(required).allSatisfy(id ->
                assertThat(RequiredCompilers.isAvailable(registry, id))
                        .as("필수 컴파일러 '%s'를 찾았는지", id)
                        .isTrue());
    }

    @Test
    void 필수_목록은_쉼표로_구분하고_빈_값은_무시한다() {
        assertThat(RequiredCompilers.parse(" clang, gxx ,,")).containsExactlyInAnyOrder("clang", "gxx");
        assertThat(RequiredCompilers.parse("")).isEmpty();
        assertThat(RequiredCompilers.parse(null)).isEmpty();
    }
}
