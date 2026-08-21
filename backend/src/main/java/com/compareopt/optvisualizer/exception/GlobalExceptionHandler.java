package com.compareopt.optvisualizer.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 컨트롤러에서 던져지는 예외를 모두 이 클래스에서 가로채서
 * 프론트엔드가 이해할 수 있는 { message: "..." } 형태의 JSON으로 통일해 응답합니다.
 * (안 이러면 Spring 기본 에러 응답에는 message 필드가 없어서 프론트에서 원인을 알 수 없음)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** CompileRequest의 @NotBlank, @Size 같은 검증 실패 시 (예: 코드가 비어있음) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getDefaultMessage())
                .orElse("입력값이 올바르지 않습니다.");
        return ResponseEntity.badRequest().body(new ErrorResponse(message));
    }

    /** 그 외 예상치 못한 서버 오류 (예: clang 실행 파일을 찾을 수 없음 등) */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("처리되지 않은 예외 발생", e);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse("서버 내부 오류가 발생했습니다: " + e.getMessage()));
    }
}
