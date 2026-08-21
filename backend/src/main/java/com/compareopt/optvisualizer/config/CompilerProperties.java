package com.compareopt.optvisualizer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * application.yml의 compiler.options 목록을 바인딩하는 설정 클래스.
 *
 * 컴파일러 하나(예: gcc)당 여러 후보 실행 파일 이름(candidates)을 등록해둘 수 있다.
 * Homebrew로 설치한 GCC는 "gcc-16"처럼 버전이 붙은 이름으로 설치되는 등
 * 환경마다 실제 실행 파일 이름이 달라지기 때문에, 후보를 순서대로 시도해서
 * 처음 성공하는 것을 사용한다 (CompilerRegistry 참고).
 */
@Component
@ConfigurationProperties(prefix = "compiler")
public class CompilerProperties {

    private List<Option> options = new ArrayList<>();

    public List<Option> getOptions() {
        return options;
    }

    public void setOptions(List<Option> options) {
        this.options = options;
    }

    public static class Option {
        /** 프론트엔드가 요청 시 넘기는 식별자 (예: "clang", "gcc") */
        private String id;
        /** 프론트엔드 드롭다운에 보여줄 이름 */
        private String label;
        /** 실제로 시도해볼 실행 파일 이름 후보들 (순서대로 시도) */
        private List<String> candidates = new ArrayList<>();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public List<String> getCandidates() {
            return candidates;
        }

        public void setCandidates(List<String> candidates) {
            this.candidates = candidates;
        }
    }
}
