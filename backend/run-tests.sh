#!/bin/bash
# 이 프로젝트는 Java 17 대상(pom.xml의 <java.version>)이지만,
# 시스템에 설치된 mvn 스크립트가 기본적으로 최신 Homebrew Java(26)를 사용하도록
# 고정되어 있어서 Mockito 인라인 목(mock)이 깨지는 문제가 있다.
# (Mockito가 아직 최신 Java의 바이트코드 조작 방식을 완전히 지원하지 않음)
# 그래서 테스트만큼은 JDK 17로 명시적으로 고정해서 실행한다.
set -e
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
cd "$(dirname "$0")"
mvn test "$@"
