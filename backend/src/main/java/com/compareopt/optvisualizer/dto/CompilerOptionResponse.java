package com.compareopt.optvisualizer.dto;

/**
 * GET /api/compilers 응답 항목. 프론트엔드의 언어·컴파일러 드롭다운에 필요한 정보만 노출한다
 * (실제 실행 파일 경로는 서버 내부 정보라 응답에 포함하지 않음).
 *
 * @param language      이 컴파일러가 컴파일하는 언어의 id (예: "cpp"). 화면은 이 값으로 컴파일러를 언어별로 묶는다
 * @param languageLabel 화면에 보여줄 언어 이름 (예: "C++")
 */
public record CompilerOptionResponse(String id, String label, boolean available,
                                     String language, String languageLabel) {
}
