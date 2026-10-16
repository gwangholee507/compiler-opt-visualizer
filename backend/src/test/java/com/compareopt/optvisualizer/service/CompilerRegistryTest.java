package com.compareopt.optvisualizer.service;

import com.compareopt.optvisualizer.config.CompilerProperties;
import com.compareopt.optvisualizer.platform.ProcessRunner;
import com.compareopt.optvisualizer.toolchain.Language;
import com.compareopt.optvisualizer.toolchain.ToolchainKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 설정 검증만 확인한다. 후보를 실행해보기 전에 실패해야 하므로 컴파일러가 없어도 돈다.
 */
class CompilerRegistryTest {

    @Test
    void language가_없는_컴파일러_설정은_서버_시작_단계에서_거부한다() {
        CompilerProperties.Option option = option("no-language");
        option.setToolchain(ToolchainKind.CLANG);

        assertThatThrownBy(() -> new CompilerRegistry(properties(option), new ProcessRunner()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no-language");
    }

    @Test
    void toolchain이_없는_컴파일러_설정은_서버_시작_단계에서_거부한다() {
        CompilerProperties.Option option = option("no-toolchain");
        option.setLanguage(Language.C);

        assertThatThrownBy(() -> new CompilerRegistry(properties(option), new ProcessRunner()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no-toolchain");
    }

    private static CompilerProperties.Option option(String id) {
        CompilerProperties.Option option = new CompilerProperties.Option();
        option.setId(id);
        option.setLabel(id);
        option.setCandidates(List.of("clang"));
        return option;
    }

    private static CompilerProperties properties(CompilerProperties.Option option) {
        CompilerProperties properties = new CompilerProperties();
        properties.setOptions(List.of(option));
        return properties;
    }
}
