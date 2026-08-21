package com.compareopt.optvisualizer.dto;

public class OptimizationResult {

    private String level;          // "O0", "O1", "O2", "O3"
    private boolean success;
    private String assembly;       // -S 결과 (실패 시 null)
    private Long binarySizeBytes;  // 실행 파일 크기 (실패 시 null)
    private long compileTimeMs;
    private String errorMessage;   // 실패 시 컴파일러 stderr

    public static OptimizationResult success(String level, String assembly, long binarySizeBytes, long compileTimeMs) {
        OptimizationResult r = new OptimizationResult();
        r.level = level;
        r.success = true;
        r.assembly = assembly;
        r.binarySizeBytes = binarySizeBytes;
        r.compileTimeMs = compileTimeMs;
        return r;
    }

    public static OptimizationResult failure(String level, String errorMessage, long compileTimeMs) {
        OptimizationResult r = new OptimizationResult();
        r.level = level;
        r.success = false;
        r.errorMessage = errorMessage;
        r.compileTimeMs = compileTimeMs;
        return r;
    }

    public String getLevel() {
        return level;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getAssembly() {
        return assembly;
    }

    public Long getBinarySizeBytes() {
        return binarySizeBytes;
    }

    public long getCompileTimeMs() {
        return compileTimeMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
