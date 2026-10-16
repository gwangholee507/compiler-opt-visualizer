package com.compareopt.optvisualizer.controller;

import com.compareopt.optvisualizer.dto.OptimizationResult;
import com.compareopt.optvisualizer.service.CompilerRegistry;
import com.compareopt.optvisualizer.service.CompilerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

    @MockBean
    private CompilerRegistry compilerRegistry;

    @Test
    void 정상적인_요청은_200과_결과_리스트를_반환한다() throws Exception {
        when(compilerService.compareOptimizationLevels(anyString(), anyString(), anyBoolean()))
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

    @Test
    void 다른_웹사이트에서_보내는_요청은_사전_요청_단계에서_거부된다() throws Exception {
        // 이 API는 받은 코드를 컴파일/실행하므로, 사용자가 방문한 다른 사이트의 스크립트가 호출할 수 없어야 한다.
        // 브라우저는 JSON POST 전에 사전 요청(OPTIONS)을 보내는데, 여기서 허용 헤더를 주지 않으면 본 요청을 보내지 않는다.
        mockMvc.perform(options("/api/compile")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void 없는_주소는_500이_아니라_404를_반환한다() throws Exception {
        mockMvc.perform(get("/no-such-page"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }
}
