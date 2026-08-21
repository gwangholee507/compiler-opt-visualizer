package com.compareopt.optvisualizer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CompileRequest {

    @NotBlank(message = "C 코드를 입력해주세요.")
    @Size(max = 20_000, message = "코드는 20,000자를 넘을 수 없습니다.")
    private String code;

    /** 사용할 컴파일러 id (예: "clang", "gcc"). CompilerRegistry에 등록된 값만 허용됨. */
    private String compiler = "clang";

    /**
     * true면 O0~O3 각각을 실제로 링크·실행해서 실행 시간을 측정한다.
     * main()이 없는 코드는 링크에 실패하므로 자동으로 건너뛴다.
     * 서버가 사용자 코드를 그대로 실행하는 기능이라 기본값은 false(옵트인).
     */
    private boolean runBenchmark = false;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCompiler() {
        return compiler;
    }

    public void setCompiler(String compiler) {
        this.compiler = compiler;
    }

    public boolean isRunBenchmark() {
        return runBenchmark;
    }

    public void setRunBenchmark(boolean runBenchmark) {
        this.runBenchmark = runBenchmark;
    }
}
