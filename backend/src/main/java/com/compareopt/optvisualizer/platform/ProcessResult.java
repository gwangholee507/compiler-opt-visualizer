package com.compareopt.optvisualizer.platform;

/**
 * 외부 프로세스 한 번의 실행 결과.
 *
 * @param exitCode     종료 코드 (timedOut이면 의미 없는 값 -1)
 * @param output       stdout+stderr를 합친 출력 (줄바꿈은 \n으로 통일됨). 출력을 버리고 실행했으면 빈 문자열
 * @param timedOut     제한 시간 안에 끝나지 않아 강제 종료했는지
 * @param elapsedNanos 프로세스를 띄우기 직전부터 종료(또는 타임아웃)까지 걸린 시간
 */
public record ProcessResult(int exitCode, String output, boolean timedOut, long elapsedNanos) {

    public boolean succeeded() {
        return !timedOut && exitCode == 0;
    }
}
