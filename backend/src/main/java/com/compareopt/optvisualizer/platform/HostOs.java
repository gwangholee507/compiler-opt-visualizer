package com.compareopt.optvisualizer.platform;

import java.util.Locale;

/**
 * 이 서버가 떠 있는 운영체제.
 *
 * OS마다 달라지는 값(예: PATH에 등록되지 않는 컴파일러 기본 설치 위치)을 고를 때만 쓴다.
 * 컴파일/실행 흐름 자체는 OS와 무관하게 같은 코드로 돌아가도록 하고, OS별 분기는
 * 이 platform 패키지 안에만 두는 것이 원칙이다.
 */
public enum HostOs {
    WINDOWS, MACOS, LINUX, OTHER;

    private static final HostOs CURRENT = fromOsName(System.getProperty("os.name", ""));

    public static HostOs current() {
        return CURRENT;
    }

    /** JVM의 os.name 값(예: "Windows 11", "Mac OS X", "Linux")을 HostOs로 바꾼다. */
    static HostOs fromOsName(String osName) {
        String name = osName.toLowerCase(Locale.ROOT);
        // "darwin"에 "win"이 들어 있으므로 macOS를 먼저 확인하고, Windows는 접두사로만 판단한다.
        if (name.startsWith("mac") || name.contains("darwin")) {
            return MACOS;
        }
        if (name.startsWith("windows")) {
            return WINDOWS;
        }
        if (name.contains("linux")) {
            return LINUX;
        }
        return OTHER;
    }

    /** application.yml의 compiler.options[].os-candidates 에서 쓰는 키 ("windows", "macos", "linux"). */
    public String configKey() {
        return name().toLowerCase(Locale.ROOT);
    }
}
