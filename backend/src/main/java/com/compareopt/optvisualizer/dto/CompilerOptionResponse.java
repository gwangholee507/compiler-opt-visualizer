package com.compareopt.optvisualizer.dto;

/**
 * GET /api/compilers 응답 항목. 프론트엔드 드롭다운에 필요한 정보만 노출한다
 * (실제 실행 파일 경로는 서버 내부 정보라 응답에 포함하지 않음).
 */
public record CompilerOptionResponse(String id, String label, boolean available) {
}
