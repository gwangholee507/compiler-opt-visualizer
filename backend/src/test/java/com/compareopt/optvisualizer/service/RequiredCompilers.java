package com.compareopt.optvisualizer.service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * CI가 환경 변수 OPTVIS_REQUIRED_COMPILERS로 지정한 "그 러너에 반드시 있어야 하는" 컴파일러 id 목록.
 *
 * 테스트는 보통 컴파일러가 없으면 건너뛰지만, 이 목록에 있는 컴파일러가 없으면 실패시킨다.
 * 러너 이미지가 바뀌어 컴파일러가 빠졌을 때 테스트가 조용히 건너뛰어져 CI가 통과하는 것을 막기 위함이다.
 * 로컬에서는 이 환경 변수가 없으므로 지금처럼 건너뛴다 (ci.yml의 required-compilers 참고).
 */
final class RequiredCompilers {

    static final String ENV = "OPTVIS_REQUIRED_COMPILERS";

    private RequiredCompilers() {
    }

    static Set<String> fromEnv() {
        return parse(System.getenv(ENV));
    }

    /** "clang, gcc,,gxx" → {clang, gcc, gxx}. 빈 값이나 null이면 빈 집합. */
    static Set<String> parse(String value) {
        if (value == null) {
            return Set.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    /** 그 컴파일러를 쓸 수 있으면 그대로 진행하고, 없으면 필수 목록에 있을 때만 실패, 아니면 테스트를 건너뛴다. */
    static void assumeAvailable(CompilerRegistry registry, String id) {
        boolean available = isAvailable(registry, id);
        if (!available && fromEnv().contains(id)) {
            fail("CI에서 필수로 지정한 컴파일러 '" + id + "'를 찾지 못했습니다 (" + ENV + ").");
        }
        assumeTrue(available, "컴파일러 '" + id + "'가 설치되어 있지 않아 건너뜁니다.");
    }

    static boolean isAvailable(CompilerRegistry registry, String id) {
        return registry.listAll().stream().anyMatch(c -> c.id().equals(id) && c.available());
    }
}
