package com.compareopt.optvisualizer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CompileRequest {

    @NotBlank(message = "C 코드를 입력해주세요.")
    @Size(max = 20_000, message = "코드는 20,000자를 넘을 수 없습니다.")
    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
