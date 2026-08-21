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
프론트엔드는 요청 시 `id`(예: `"gcc"`)만 넘기고, 실제 실행 파일 경로로의 변환은 항상 이 레지스트리를
거칩니다 — 사용자가 보낸 문자열이 그대로 서버 프로세스 커맨드에 들어가는 것(명령어 인젝션)을 막기 위한
화이트리스트 구조입니다. 자세한 내용은 [backend.md](./backend.md) 참고.

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
