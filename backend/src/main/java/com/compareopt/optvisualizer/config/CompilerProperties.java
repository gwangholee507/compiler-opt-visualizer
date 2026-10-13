package com.compareopt.optvisualizer.config;

import com.compareopt.optvisualizer.platform.HostOs;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * application.yml의 compiler.options 목록을 바인딩하는 설정 클래스.
 *
 * 컴파일러 하나(예: gcc)당 여러 후보 실행 파일(candidates)을 등록해둘 수 있다.
 * Homebrew로 설치한 GCC는 "gcc-16"처럼 버전이 붙은 이름으로 설치되고, Windows의 MSYS2는
 * PATH에 등록되지 않는 C:\msys64 아래에 설치되는 등 환경마다 실제 실행 파일 위치가 달라지기 때문에,
 * 후보를 순서대로 시도해서 처음 성공하는 것을 사용한다 (CompilerRegistry 참고).
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
        /** 모든 OS에서 시도해볼 후보들 (순서대로 시도). PATH에 있는 실행 파일 이름 또는 절대 경로. */
        private List<String> candidates = new ArrayList<>();
        /** 특정 OS에서만 추가로 시도할 후보들. 키는 HostOs.configKey() ("windows", "macos", "linux"). */
        private Map<String, List<String>> osCandidates = new HashMap<>();

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

        public Map<String, List<String>> getOsCandidates() {
            return osCandidates;
        }

        public void setOsCandidates(Map<String, List<String>> osCandidates) {
            this.osCandidates = osCandidates;
        }

        /**
         * 주어진 OS에서 시도할 후보를 순서대로 반환한다: 공통 후보 → 그 OS 전용 후보.
         * 빈 값은 건너뛴다 (application.yml의 "${OPTVIS_CLANG:}"처럼 환경 변수가 없을 때 빈 문자열이 되는 후보).
         */
        public List<String> candidatesFor(HostOs os) {
            List<String> result = new ArrayList<>();
            for (String candidate : candidates) {
                if (candidate != null && !candidate.isBlank()) {
                    result.add(candidate.trim());
                }
            }
            for (String candidate : osCandidates.getOrDefault(os.configKey(), List.of())) {
                if (candidate != null && !candidate.isBlank()) {
                    result.add(candidate.trim());
                }
            }
            return result;
        }
    }
}
