package com.compareopt.optvisualizer.controller;

import com.compareopt.optvisualizer.dto.CompileRequest;
import com.compareopt.optvisualizer.dto.OptimizationResult;
import com.compareopt.optvisualizer.service.CompilerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173") // Vite 개발 서버
public class CompileController {

    private final CompilerService compilerService;

    public CompileController(CompilerService compilerService) {
        this.compilerService = compilerService;
    }

    @PostMapping("/api/compile")
    public List<OptimizationResult> compile(@Valid @RequestBody CompileRequest request) {
        return compilerService.compareOptimizationLevels(request.getCode());
    }
}
