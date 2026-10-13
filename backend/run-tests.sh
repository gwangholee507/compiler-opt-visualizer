#!/bin/bash
# 이 프로젝트는 Java 17 대상(pom.xml의 <java.version>)이지만,
# 환경에 따라 mvn이 기본으로 잡는 Java가 훨씬 최신 버전일 수 있다
# (예: Homebrew로 설치한 mvn은 최신 Homebrew Java를 쓰도록 고정되어 있음).
# 그러면 Mockito 인라인 목(mock)이 깨지는 문제가 있어서
# (Mockito가 아직 최신 Java의 바이트코드 조작 방식을 완전히 지원하지 않음)
# 테스트만큼은 설치된 JDK 17을 찾아서 명시적으로 고정해 실행한다.
#
# JDK 17을 못 찾으면 mvn 기본 Java로 그대로 실행한다. 아래 후보에 없는 위치에 설치했다면
# JAVA_HOME을 직접 지정하면 된다:  JAVA_HOME=/path/to/jdk-17 ./run-tests.sh
set -e

# 주어진 경로가 실제로 JDK 17인지 확인한다.
# (macOS의 `java_home -v 17`은 17이 없으면 더 높은 버전을 돌려주기도 해서 경로만 믿을 수 없음)
is_jdk17() {
    [ -x "$1/bin/java" ] && "$1/bin/java" -version 2>&1 | grep -Eq 'version "17[^0-9]'
}

# 후보 경로를 순서대로 확인해서 처음으로 JDK 17인 것을 출력한다. 없으면 실패(1)를 반환.
find_jdk17() {
    local candidate
    for candidate in \
        "$JAVA_HOME" \
        "$(/usr/libexec/java_home -v 17 2>/dev/null || true)" \
        /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
        /usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
        /usr/lib/jvm/*17*
    do
        # 순서: 사용자가 지정한 JAVA_HOME → macOS 등록 JDK → Homebrew(Apple Silicon, Intel) → Linux 패키지
        if [ -n "$candidate" ] && is_jdk17 "$candidate"; then
            echo "$candidate"
            return 0
        fi
    done
    return 1
}

if jdk17="$(find_jdk17)"; then
    export JAVA_HOME="$jdk17"
else
    echo "[run-tests] JDK 17을 찾지 못해 mvn 기본 Java로 실행합니다." \
         "테스트가 깨지면 JAVA_HOME을 JDK 17 경로로 지정해주세요." >&2
fi

cd "$(dirname "$0")"
mvn test "$@"
