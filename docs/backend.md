# 백엔드 (Spring Boot)

패키지 루트: `com.compareopt.optvisualizer`

## `OptVisualizerApplication`
**파일**: [`backend/src/main/java/.../OptVisualizerApplication.java`](../backend/src/main/java/com/compareopt/optvisualizer/OptVisualizerApplication.java)

Spring Boot 진입점. `main()`에서 `SpringApplication.run()`을 호출해 내장 톰캣 서버(포트 8080)를 띄웁니다.
특별한 로직은 없고, 애플리케이션을 부팅하는 역할만 합니다.

---

## `CompileController`
**파일**: [`backend/src/main/java/.../controller/CompileController.java`](../backend/src/main/java/com/compareopt/optvisualizer/controller/CompileController.java)

HTTP 요청을 받는 REST 컨트롤러. `@CrossOrigin`으로 프론트엔드 개발 서버(`http://localhost:5173`)의 요청만 허용합니다.

### `listCompilers() : List<CompilerOptionResponse>`
- **경로**: `GET /api/compilers`
- **동작**: `CompilerRegistry.listAll()`이 반환하는 내부 `ResolvedCompiler` 리스트를, 실행 파일 경로는 뺀
  `(id, label, available)` 형태로 변환해서 응답. 프론트엔드가 페이지 로드 시 이 목록으로 드롭다운을 채움.

### `compile(CompileRequest request) : List<OptimizationResult>`
- **경로**: `POST /api/compile`
- **입력**: `{ "code": "...", "compiler": "clang"|"gcc", "runBenchmark": true|false }`
  (`compiler` 기본값 `"clang"`, `runBenchmark` 기본값 `false`)
- **동작**: `@Valid`로 `CompileRequest`를 검증한 뒤, `CompilerService.compareOptimizationLevels()`에 세 값을 그대로 위임
- **출력**: O0~O3 각각의 결과를 담은 `OptimizationResult` 리스트 (JSON 배열)
- 검증 실패(코드가 비어있거나 20,000자 초과) 시 Spring이 자동으로 400 Bad Request를 반환하고,
  `GlobalExceptionHandler`가 그 예외를 가로채 `{ message: "..." }` 형태로 응답을 정리함
- 존재하지 않거나 서버에 설치되지 않은 `compiler` id를 보내면 `CompilerRegistry.resolve()`가
  `IllegalArgumentException`을 던지고, 역시 `GlobalExceptionHandler`가 400으로 변환함

---

## `CompilerProperties` / `CompilerRegistry`
**파일**: [`config/CompilerProperties.java`](../backend/src/main/java/com/compareopt/optvisualizer/config/CompilerProperties.java),
[`service/CompilerRegistry.java`](../backend/src/main/java/com/compareopt/optvisualizer/service/CompilerRegistry.java)

`application.yml`의 `compiler.options` 목록(컴파일러 id/label/후보 실행 파일 이름들)을
`CompilerProperties`가 바인딩하고, `CompilerRegistry`가 서버 시작 시 각 후보에 대해
`{candidate} --version`을 실행해보며 실제로 동작하는 첫 번째 후보를 채택합니다.

- id가 `"gcc"`인 옵션은 버전 출력에 `"clang"`이라는 문자열이 포함되면 건너뜁니다.
  macOS의 `/usr/bin/gcc`가 실제로는 Apple clang의 별칭이라, 그대로 두면 진짜 GCC가 없는데도
  "GNU GCC 사용 가능"으로 오탐되기 때문입니다.
- `resolve(id)`가 이 프로젝트에서 유일하게 사용자 입력을 실제 실행 파일 경로로 바꿔주는 지점입니다.
  등록되지 않았거나 사용 불가능한 id면 `IllegalArgumentException`을 던집니다 — 사용자가 보낸
  임의 문자열이 그대로 `ProcessBuilder` 커맨드로 흘러들어가는 것(명령어 인젝션)을 막는 화이트리스트 역할.

---

## `CompilerService`
**파일**: [`backend/src/main/java/.../service/CompilerService.java`](../backend/src/main/java/com/compareopt/optvisualizer/service/CompilerService.java)

이 프로젝트의 핵심 로직. 선택된 컴파일러(clang/gcc)를 서브프로세스로 호출해서 실제 컴파일(및 옵트인 시 실행)을 수행합니다.

### `compareOptimizationLevels(String sourceCode, String compilerId, boolean runBenchmark) : List<OptimizationResult>`
전체 흐름을 총괄하는 메서드.
1. `CompilerRegistry.resolve(compilerId)`로 사용할 컴파일러의 실제 경로를 얻음 (실패 시 예외)
2. `Files.createTempDirectory()`로 임시 작업 디렉터리 생성
3. 사용자 코드를 `input.c`로 저장
4. `LEVELS = ["O0", "O1", "O2", "O3"]`를 순회하며 각각 `runOneLevel()` 호출, 결과에 `setCompilerInfo()`로 어떤 컴파일러를 썼는지 기록
5. 예외 발생 시(임시 디렉터리 생성 실패 등) 4개 레벨 모두 실패 결과로 채움
6. `finally` 블록에서 `cleanup()`으로 임시 파일 반드시 삭제

### `runOneLevel(...) : OptimizationResult` (private)
특정 최적화 레벨 하나에 대한 컴파일(및 옵트인 시 실행 벤치마크)을 수행.
1. `{compiler} -S -O{level} input.c -o out.s` 실행 → 어셈블리 생성
   - 실패 시 `sanitizeStderr()`로 서버 임시 경로를 `input.c`로 치환한 뒤 `OptimizationResult.failure()` 반환
2. `{compiler} -c -O{level} input.c -o out.o` 실행 → 오브젝트 파일 생성 (크기 비교용, `main()` 없어도 동작)
3. 오브젝트 파일 크기(`Files.size()`)를 읽고, 실패해도 무시(0으로 처리)하고 계속 진행
4. 어셈블리 파일 내용을 문자열로 읽어서 `OptimizationResult.success()` 생성
5. `runBenchmark=true`면 `benchmarkExecution()`을 호출해 실행 시간을 결과에 붙임

### `benchmarkExecution(...) : ExecutionOutcome` (private)
`{compiler} -O{level} input.c -o out.exe`로 링크를 시도한 뒤(성공해야 `main()`이 있다는 뜻),
실행 파일을 `BENCHMARK_RUNS`(5)번 실행해서 걸린 시간(나노초) 중 최솟값을 채택합니다.
- 링크 실패(대개 `main()` 없음) 시 컴파일 자체는 성공으로 두고 `executionError`만 채움 — 순수 함수만
  있는 코드에서도 어셈블리/크기 비교는 그대로 볼 수 있게 하기 위함
- 한 번이라도 `EXECUTION_TIMEOUT_SECONDS`(5초) 안에 끝나지 않으면 즉시 타임아웃 에러로 처리

### `run(List<String> command) : ProcessResult` (private)
`ProcessBuilder`로 실제 외부 프로세스(컴파일러)를 실행하는 저수준 헬퍼.
- `process.waitFor(10, TimeUnit.SECONDS)`로 **10초 타임아웃** 적용 — 무한 루프 코드가 들어와도 서버가 멈추지 않도록 방지
- 타임아웃 시 `process.destroyForcibly()`로 강제 종료
- stderr을 문자열로 캡처해서 실패 원인을 그대로 프론트엔드에 전달

### `sanitizeStderr(String stderr, Path sourceFile) : String` (private)
clang stderr에 그대로 찍히는 `/var/folders/.../input.c` 같은 서버 임시 디렉터리 절대경로를
`input.c`로 치환. 사용자에게는 의미 없는 서버 내부 경로라 노출하지 않기 위함.

### `cleanup(Path workDir)` (private)
`Files.walk()`로 임시 디렉터리 내 모든 파일/디렉터리를 순회하며 삭제.
파일을 먼저 지우고 디렉터리를 나중에 지워야 하므로 경로를 역순 정렬(`b.compareTo(a)`) 후 삭제.

### `ProcessResult` (private record)
`(int exitCode, String stderr)` — 프로세스 실행 결과를 담는 내부 전용 값 객체.

---

## `GlobalExceptionHandler`
**파일**: [`backend/src/main/java/.../exception/GlobalExceptionHandler.java`](../backend/src/main/java/com/compareopt/optvisualizer/exception/GlobalExceptionHandler.java)

`@RestControllerAdvice`로 모든 컨트롤러의 예외를 가로채서, 프론트엔드(`App.jsx`의 `handleCompare`)가
기대하는 `{ message: "..." }` 형태의 JSON으로 통일해 응답한다. 이게 없으면 Spring 기본 에러 응답에는
`message` 필드가 없어서 프론트가 실패 원인을 사용자에게 보여줄 수 없었다.

### `handleValidation(MethodArgumentNotValidException e) : ResponseEntity<ErrorResponse>`
`CompileRequest`의 `@NotBlank`/`@Size` 검증 실패 시 호출. 첫 번째 필드 에러의 메시지(예: "C 코드를 입력해주세요.")를
뽑아 400 응답으로 반환.

### `handleUnexpected(Exception e) : ResponseEntity<ErrorResponse>`
그 외 처리되지 않은 모든 예외를 잡는 catch-all 핸들러. 로그를 남기고 500 응답으로 반환.

## `ErrorResponse` (DTO)
**파일**: [`backend/src/main/java/.../exception/ErrorResponse.java`](../backend/src/main/java/com/compareopt/optvisualizer/exception/ErrorResponse.java)

`(String message)` — 에러 응답 바디를 표현하는 record.

---

## `CompileRequest` (DTO)
**파일**: [`backend/src/main/java/.../dto/CompileRequest.java`](../backend/src/main/java/com/compareopt/optvisualizer/dto/CompileRequest.java)

요청 바디를 받는 객체. `@NotBlank`, `@Size(max = 20000)` 검증 애너테이션이 붙어있어
컨트롤러의 `@Valid`와 함께 자동으로 유효성 검사가 이루어집니다.
- `compiler` (기본값 `"clang"`): 사용할 컴파일러 id. 여기 담긴 문자열 자체는 검증하지 않고,
  `CompilerRegistry.resolve()`에서 화이트리스트 검증함
- `runBenchmark` (기본값 `false`): true면 실행 시간까지 측정 (옵트인)

## `OptimizationResult` (DTO)
**파일**: [`backend/src/main/java/.../dto/OptimizationResult.java`](../backend/src/main/java/com/compareopt/optvisualizer/dto/OptimizationResult.java)

레벨 하나의 컴파일 결과를 담는 응답 객체. 생성자를 `private`으로 막고
`success()` / `failure()` 정적 팩토리 메서드로만 만들 수 있게 해서, 성공/실패 상태에 맞지 않는
필드 조합(예: 실패인데 assembly가 채워짐)이 생기지 않도록 강제합니다.

- `success(level, assembly, binarySizeBytes, compileTimeMs)`
- `failure(level, errorMessage, compileTimeMs)`
- `setCompilerInfo(compilerId, compilerLabel)`: success/failure 여부와 무관하게 항상 호출되어
  어떤 컴파일러로 만든 결과인지 붙여줌
- `setExecutionResult(executionTimeMs, executionError)`: `runBenchmark=true`일 때만 채워짐.
  둘 다 상호 배타적 — 측정에 성공하면 `executionTimeMs`만, 실패/불가능하면 `executionError`만 채워짐

## `CompilerOptionResponse` (DTO)
**파일**: [`backend/src/main/java/.../dto/CompilerOptionResponse.java`](../backend/src/main/java/com/compareopt/optvisualizer/dto/CompilerOptionResponse.java)

`(String id, String label, boolean available)` — `GET /api/compilers` 응답 항목 하나.
실행 파일 경로는 서버 내부 정보라 일부러 포함하지 않음.
