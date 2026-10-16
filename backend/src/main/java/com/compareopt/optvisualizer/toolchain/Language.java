package com.compareopt.optvisualizer.toolchain;

/**
 * 사용자가 입력한 코드의 언어. application.yml의 compiler.options[].language 값이다 (예: "c").
 *
 * 언어는 소스 파일 이름만 정한다. 어떤 명령줄로 컴파일할지는 Toolchain이 정한다
 * (같은 언어라도 clang과 gcc처럼 여러 컴파일러가 있을 수 있기 때문).
 */
public enum Language {
    C("input.c");

    private final String sourceFileName;

    Language(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    /** 임시 작업 디렉터리에 저장할 소스 파일 이름. 컴파일러가 확장자로 언어를 판단하므로 언어마다 다르다. */
    public String sourceFileName() {
        return sourceFileName;
    }
}
