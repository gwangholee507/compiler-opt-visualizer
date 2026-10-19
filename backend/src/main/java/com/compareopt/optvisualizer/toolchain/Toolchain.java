package com.compareopt.optvisualizer.toolchain;

import java.util.List;
import java.util.Optional;

/**
 * 컴파일러 계열마다 달라지는 "명령줄 만드는 법".
 *
 * CompilerService는 여기서 받은 인자로 컴파일러를 실행하기만 하고, 임시 디렉터리, 시간 제한,
 * 실행 시간 측정처럼 모든 컴파일러에 공통인 부분은 CompilerService가 그대로 맡는다.
 * 돌려주는 인자 목록에는 실행 파일 경로를 넣지 않는다 (경로는 CompilerRegistry가 찾아서 맨 앞에 붙인다).
 * 파일 이름은 모두 작업 디렉터리 기준의 상대 경로다.
 */
public interface Toolchain {

    /** 비교할 최적화 레벨 목록. 결과도 이 순서대로 만들어지고, 화면은 첫 번째 레벨을 비교 기준으로 삼는다. */
    List<OptLevel> levels();

    /** 설치 여부를 확인할 때 실행 파일 뒤에 붙일 인자. 종료 코드가 0이면 일단 실행 가능한 것으로 본다. */
    List<String> versionArgs();

    /**
     * 버전 출력을 보고 이 계열의 컴파일러가 맞는지 확인한다.
     * 맞으면 empty, 아니면 그 후보를 건너뛰는 이유를 돌려준다 (서버 시작 로그에 남는다).
     */
    Optional<String> versionMismatch(String versionOutput);

    /** 어셈블리 파일을 만드는 인자. */
    List<String> assemblyArgs(OptLevel level, String sourceFile, String outputFile);

    /** 크기 비교용 오브젝트 파일을 만드는 인자. main() 같은 진입점이 없는 코드에서도 성공해야 한다. */
    List<String> objectArgs(OptLevel level, String sourceFile, String outputFile);

    /** 실행 시간 측정용 실행 파일을 만드는 인자. 진입점이 없으면 실패해도 된다 (실행 측정만 건너뜀). */
    List<String> executableArgs(OptLevel level, String sourceFile, String outputFile);
}
