package com.compareopt.optvisualizer.controller;

import com.compareopt.optvisualizer.dto.CompileRequest;
import com.compareopt.optvisualizer.dto.CompilerOptionResponse;
import com.compareopt.optvisualizer.dto.OptimizationResult;
import com.compareopt.optvisualizer.service.CompilerRegistry;
import com.compareopt.optvisualizer.service.CompilerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173") // Vite 개발 서버
public class CompileController {

    private final CompilerService compilerService;
    private final CompilerRegistry compilerRegistry;

    public CompileController(CompilerService compilerService, CompilerRegistry compilerRegistry) {
        this.compilerService = compilerService;
        this.compilerRegistry = compilerRegistry;
    }

    /** 프론트엔드 드롭다운에 채울 수 있는, 이 서버에서 실제로 사용 가능한 컴파일러 목록. */
    @GetMapping("/api/compilers")
    public List<CompilerOptionResponse> listCompilers() {
        return compilerRegistry.listAll().stream()
                .map(c -> new CompilerOptionResponse(c.id(), c.label(), c.available()))
                .toList();
    }

    @PostMapping("/api/compile")
    public List<OptimizationResult> compile(@Valid @RequestBody CompileRequest request) {
        return compilerService.compareOptimizationLevels(request.getCode(), request.getCompiler(), request.isRunBenchmark());
    }
}
