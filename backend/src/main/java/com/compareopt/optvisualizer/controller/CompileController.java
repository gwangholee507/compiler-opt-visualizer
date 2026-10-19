package com.compareopt.optvisualizer.controller;

import com.compareopt.optvisualizer.dto.CompileRequest;
import com.compareopt.optvisualizer.dto.CompilerOptionResponse;
import com.compareopt.optvisualizer.dto.OptimizationResult;
import com.compareopt.optvisualizer.service.CompilerRegistry;
import com.compareopt.optvisualizer.service.CompilerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 화면(프론트엔드)은 항상 이 서버와 같은 출처에서 /api 를 호출한다 — 배포된 jar에서는 이 서버가
 * 화면도 함께 내보내고, 개발 중에는 Vite 개발 서버가 /api 요청을 이쪽으로 넘겨준다.
 * 그래서 CORS 허용 설정(@CrossOrigin)을 일부러 두지 않는다: 이 API는 받은 코드를 컴파일하고
 * 실행까지 하므로, 사용자가 방문한 다른 웹사이트의 스크립트가 이 서버를 호출할 수 없어야 한다.
 */
@RestController
public class CompileController {

    private final CompilerService compilerService;
    private final CompilerRegistry compilerRegistry;

    public CompileController(CompilerService compilerService, CompilerRegistry compilerRegistry) {
        this.compilerService = compilerService;
        this.compilerRegistry = compilerRegistry;
    }

    /**
     * 프론트엔드 드롭다운에 채울 컴파일러 목록 (설치되지 않은 것도 available=false로 포함).
     * 순서는 application.yml에 적은 순서 그대로이고, 화면은 이 순서를 유지한 채 언어별로 묶는다.
     */
    @GetMapping("/api/compilers")
    public List<CompilerOptionResponse> listCompilers() {
        return compilerRegistry.listAll().stream()
                .map(c -> new CompilerOptionResponse(
                        c.id(), c.label(), c.available(), c.language().id(), c.language().label()))
                .toList();
    }

    @PostMapping("/api/compile")
    public List<OptimizationResult> compile(@Valid @RequestBody CompileRequest request) {
        return compilerService.compareOptimizationLevels(request.getCode(), request.getCompiler(), request.isRunBenchmark());
    }
}
