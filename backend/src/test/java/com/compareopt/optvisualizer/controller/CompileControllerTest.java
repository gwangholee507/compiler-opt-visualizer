package com.compareopt.optvisualizer.controller;

import com.compareopt.optvisualizer.dto.OptimizationResult;
import com.compareopt.optvisualizer.service.CompilerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 컨트롤러 계층만 검증한다 (실제 clang 호출은 CompilerService를 mock으로 대체).
 * 실제 컴파일 동작은 CompilerServiceTest에서 검증함.
 */
@WebMvcTest(CompileController.class)
class CompileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompilerService compilerService;

    @Test
    void 정상적인_요청은_200과_결과_리스트를_반환한다() throws Exception {
        when(compilerService.compareOptimizationLevels(anyString()))
                .thenReturn(List.of(OptimizationResult.success("O0", "ret", 100, 10)));

        mockMvc.perform(post("/api/compile")
                        .contentType("application/json")
                        .content("{\"code\":\"int f(){return 0;}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].level").value("O0"))
                .andExpect(jsonPath("$[0].success").value(true));
    }

    @Test
    void 코드가_비어있으면_400과_친절한_에러메시지를_반환한다() throws Exception {
        mockMvc.perform(post("/api/compile")
                        .contentType("application/json")
                        .content("{\"code\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("C 코드를 입력해주세요."));
    }

    @Test
    void code_필드가_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/compile")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
