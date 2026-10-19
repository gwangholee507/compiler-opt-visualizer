package com.compareopt.optvisualizer.toolchain;

/**
 * 비교할 최적화 레벨 하나. 레벨 목록과 설명은 Toolchain이 정하고, 화면은 결과에 담겨 온 값을 그대로 보여준다.
 *
 * @param id          컴파일러 인자와 출력 파일 이름에 쓰는 식별자 (예: "O2")
 * @param label       화면의 열 제목 (예: "-O2")
 * @param description 이 레벨이 무엇을 하는지 초보자용으로 풀어쓴 설명 (열 제목 옆 툴팁)
 */
public record OptLevel(String id, String label, String description) {
}
