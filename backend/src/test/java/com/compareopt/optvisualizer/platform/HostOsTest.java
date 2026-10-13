package com.compareopt.optvisualizer.platform;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HostOsTest {

    @Test
    void JVM의_os_name을_운영체제로_구분한다() {
        assertThat(HostOs.fromOsName("Windows 11")).isEqualTo(HostOs.WINDOWS);
        assertThat(HostOs.fromOsName("Windows Server 2025")).isEqualTo(HostOs.WINDOWS);
        assertThat(HostOs.fromOsName("Mac OS X")).isEqualTo(HostOs.MACOS);
        assertThat(HostOs.fromOsName("Linux")).isEqualTo(HostOs.LINUX);
        assertThat(HostOs.fromOsName("FreeBSD")).isEqualTo(HostOs.OTHER);
    }

    @Test
    void Darwin은_win이_들어있어도_Windows로_오인하지_않는다() {
        assertThat(HostOs.fromOsName("Darwin")).isEqualTo(HostOs.MACOS);
    }

    @Test
    void 설정_파일에서_쓰는_키는_소문자_이름이다() {
        assertThat(HostOs.WINDOWS.configKey()).isEqualTo("windows");
        assertThat(HostOs.MACOS.configKey()).isEqualTo("macos");
        assertThat(HostOs.LINUX.configKey()).isEqualTo("linux");
    }
}
