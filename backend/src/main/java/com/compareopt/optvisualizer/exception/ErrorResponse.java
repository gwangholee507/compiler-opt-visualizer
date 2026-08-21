package com.compareopt.optvisualizer.exception;

/**
 * 프론트엔드(App.jsx의 handleCompare)가 기대하는 에러 응답 형태.
 * 항상 { "message": "사람이 읽을 수 있는 에러 설명" } 형태로 내려줍니다.
 */
public record ErrorResponse(String message) {
}
