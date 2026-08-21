package com.compareopt.optvisualizer.dto;

public class OptimizationResult {

    private String level;          // "O0", "O1", "O2", "O3"
    private boolean success;
    private String assembly;       // -S 결과 (실패 시 null)
    private Long binarySizeBytes;  // 실행 파일 크기 (실패 시 null)
    private long compileTimeMs;
    private String errorMessage;   // 실패 시 컴파일러 stderr

    private String compilerId;     // 이 결과를 만든 컴파일러 id (예: "gcc")
    private String compilerLabel;  // 화면 표시용 이름 (예: "GNU GCC")

    private Double executionTimeMs; // runBenchmark=true일 때 측정된 실행 시간(최솟값). 측정 안 했으면 null
    private String executionError;  // 실행 측정을 시도했지만 실패/불가능했던 이유 (예: main() 없음)

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

    public String getCompilerId() {
        return compilerId;
    }

    public String getCompilerLabel() {
        return compilerLabel;
    }

    public Double getExecutionTimeMs() {
        return executionTimeMs;
    }

    public String getExecutionError() {
        return executionError;
    }

    /** 컴파일에 어떤 컴파일러를 썼는지 붙여준다. success/failure 팩토리와 별개로 항상 채워짐. */
    public void setCompilerInfo(String compilerId, String compilerLabel) {
        this.compilerId = compilerId;
        this.compilerLabel = compilerLabel;
    }

    /** 실행 시간 측정 결과를 붙여준다. runBenchmark=false였다면 호출되지 않아 null로 남는다. */
    public void setExecutionResult(Double executionTimeMs, String executionError) {
        this.executionTimeMs = executionTimeMs;
        this.executionError = executionError;
    }
}
