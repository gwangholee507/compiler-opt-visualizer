# 전체 흐름도

```
[브라우저]
   │  0. 페이지 로드 시 GET /api/compilers 로 사용 가능한 컴파일러 목록을 받아 드롭다운에 채움
   │  1. 사용자가 C 코드 입력, 컴파일러 선택(clang/gcc), 실행 시간 측정 여부 체크 후 "비교하기" 클릭
   ▼
[frontend/src/App.jsx] handleCompare()
   │  2. POST http://localhost:8080/api/compile
   │       { code: "...", compiler: "clang"|"gcc", runBenchmark: true|false }
   ▼
[backend] CompileController.compile()
   │  3. @Valid 로 코드 검증 (비어있음/20,000자 초과 시 400 에러)
   ▼
[backend] CompilerService.compareOptimizationLevels(code, compilerId, runBenchmark)
   │  4. CompilerRegistry.resolve(compilerId) 로 요청받은 id를 실제 실행 파일 경로로 변환
   │       (모르는 id거나 서버에 설치 안 돼 있으면 400 에러 — 사용자 입력을 커맨드에 그대로 쓰지 않음)
   │  5. 임시 디렉터리 생성 → input.c 로 저장
   │  6. O0, O1, O2, O3 각각에 대해 runOneLevel() 반복 호출
   │       (컴파일러 실행은 전부 platform.ProcessRunner를 거침 — 임시 디렉터리를 작업 디렉터리로 두고 실행)
   │       - {compiler} -S -O{n} → 어셈블리(.s) 생성
   │       - {compiler} -c -O{n} → 오브젝트 파일(.o) 생성 (크기 비교용)
   │       - runBenchmark=true면 {compiler} -O{n} → 실행 파일(.exe) 링크 후 5회 실행,
   │         최솟값을 실행 시간으로 채택 (main() 없어서 링크 실패하면 조용히 건너뜀)
   │  7. 임시 디렉터리 정리
   ▼
[backend] List<OptimizationResult> 를 JSON으로 응답 (컴파일러 id/label, 실행 시간 포함)
   │
   ▼
[frontend/src/App.jsx] setResults(data)
   │  8. O0 결과를 기준(baseline)으로 삼아
   ▼
[frontend/src/OptimizationColumn + AssemblyView]
   │  9. diff.js 의 diffLines() 로 O0 대비 달라진 줄 계산
   ▼
[화면] O0~O3 컬럼을 나란히 렌더링, 바뀐 줄은 초록색 하이라이트, 실행 시간은 ⏱ 배지로 표시
```

## 컴파일러는 어떻게 선택 가능한 목록으로 관리되나요?

`CompilerRegistry`가 `application.yml`의 `compiler.options`에 정의된 후보(예: gcc는 `gcc-16`, `gcc-15`, ...)를
서버 시작 시 한 번씩 `--version` 실행해보고 실제로 동작하는 것만 "사용 가능"으로 표시합니다.
후보는 PATH에 있는 실행 파일 이름일 수도, 절대 경로일 수도 있고, 특정 OS에서만 시도하는 후보
(`os-candidates`, 예: Windows의 `C:\msys64\ucrt64\bin\gcc.exe`)도 따로 둘 수 있습니다.
프론트엔드는 요청 시 `id`(예: `"gcc"`)만 넘기고, 실제 실행 파일 경로로의 변환은 항상 이 레지스트리를
거칩니다 — 사용자가 보낸 문자열이 그대로 서버 프로세스 커맨드에 들어가는 것(명령어 인젝션)을 막기 위한
화이트리스트 구조입니다. 자세한 내용은 [backend.md](./backend.md) 참고.

## OS마다 다른 부분은 어디서 처리하나요?

이 프로그램은 사용자가 자기 PC(macOS, Linux, Windows)에 설치해서 쓰는 것을 목표로 합니다.
Java와 React 코드 자체는 OS와 무관하지만, **"그 PC에 설치된 컴파일러를 찾아서 실행한다"** 는 부분은
OS마다 사정이 다릅니다. 이런 차이는 아래 세 곳에만 모아두고, 컴파일 흐름을 담당하는
`CompilerService`에는 OS별 분기를 두지 않습니다.

| OS마다 다른 것 | 처리하는 곳 | 방법 |
|---|---|---|
| 컴파일러가 설치되는 위치 | `application.yml`의 `os-candidates` + `CompilerRegistry` | PATH의 이름을 먼저 시도하고, 그 OS의 기본 설치 위치(Windows의 MSYS2, LLVM)를 이어서 시도. 그래도 못 찾으면 `OPTVIS_CLANG`/`OPTVIS_GCC` 환경 변수로 직접 지정 |
| 프로세스 실행/종료 방식 | `platform.ProcessRunner` | 출력은 파일로 받고(파이프 버퍼가 차서 멈추는 문제 방지), 타임아웃 시 자식 프로세스까지 종료, PATH 환경 변수 이름의 대소문자 차이 흡수 |
| 지금 어떤 OS인지 판단 | `platform.HostOs` | JVM의 `os.name`으로 판단. `os-candidates`의 키(`windows`/`macos`/`linux`)를 고르는 데만 사용 |

그 밖에 OS 차이를 **분기 없이** 피해 가는 장치들:

- 컴파일러에는 `input.c`, `out-O0.s` 같은 **상대 경로**만 넘기고, 작업 디렉터리를 임시 디렉터리로 지정합니다.
  경로 구분자(`/` vs `\`)나 임시 디렉터리 위치, 경로에 섞인 공백·한글(Windows 사용자 이름 등)이 컴파일러
  인자와 에러 메시지에 나타나지 않습니다.
- 실행 파일 이름은 어디서나 `out-O{n}.exe`입니다. Windows에서는 확장자가 필요하고, 다른 OS에서는 있어도 무해합니다.
- 어셈블리와 컴파일러 출력의 줄바꿈은 `\n`으로 통일해서 응답합니다 (Windows의 컴파일러는 `\r\n`으로 씀).
- 빌드/실행 명령은 Maven Wrapper(`mvnw` / `mvnw.cmd`)와 npm 스크립트만 사용하고, 특정 셸에서만 도는 스크립트는 두지 않습니다.
- `.gitattributes`로 줄바꿈을 관리해서 Windows에서 체크아웃해도 `mvnw`가 깨지지 않습니다.

CI([ci.yml](../.github/workflows/ci.yml))는 Ubuntu, macOS, Windows 세 곳에서 같은 테스트를 돌려 이 구조가
실제로 지켜지는지 확인합니다. 새 기능을 추가할 때 OS에 따라 달라지는 동작이 필요해지면
`CompilerService`에 `if (windows)`를 넣는 대신 `platform` 패키지에 넣어주세요.

> 생성되는 **어셈블리 자체**는 OS마다 다른 것이 정상입니다. 실행 파일 형식(Mach-O / ELF / PE)과
> 함수 호출 규약이 OS마다 다르기 때문이며, 이건 숨기지 않고 그대로 보여줍니다.

## 왜 오브젝트 파일(.o)로 크기를 재나요?

사용자가 입력하는 C 코드에는 보통 `main()` 함수가 없습니다 (`square`, `sum_loop` 같은 순수 함수만 있음).
`main()`이 없으면 실행 파일로 **링크**할 수 없어서(`ld: symbol(s) not found for "_main"`),
링크 단계 없이 컴파일만 하는 `-c` 옵션으로 오브젝트 파일을 만들어 크기를 비교합니다.
이 방식은 링크 오버헤드가 섞이지 않아서 오히려 "순수 코드젠 크기" 비교에는 더 적합합니다.

## 보안 관련 주의사항

`CompilerService`는 사용자가 입력한 코드를 서버 프로세스 권한으로 그대로 컴파일합니다.
지금은 로컬 개인 프로젝트 단계라 별도 샌드박싱이 없습니다. 외부에 배포할 계획이 생기면
Docker 컨테이너 격리 + CPU/메모리 제한을 반드시 추가해야 합니다.

`runBenchmark=true`로 요청하면 컴파일뿐 아니라 사용자 코드로 만든 실행 파일을 **서버가 직접
실행**하므로 위험도가 한 단계 더 높습니다. 그래서:
- 기본값은 `false`(옵트인) — 프론트엔드에서 사용자가 체크박스를 명시적으로 켜야만 동작합니다.
- 컴파일러 선택은 `CompilerRegistry`의 화이트리스트를 통해서만 실제 경로로 바뀝니다 (사용자가
  임의 문자열을 커맨드로 주입할 수 없음).
- 실행마다 5초 타임아웃(`EXECUTION_TIMEOUT_SECONDS`)이 걸려 있어 무한 루프 코드가 서버를 막지 않습니다.
- 그럼에도 CPU/메모리 제한이나 네트워크 격리는 없으므로, 외부에 배포하기 전에는 반드시
  Docker 등으로 실행 환경 자체를 격리해야 합니다.
