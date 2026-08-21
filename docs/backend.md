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

### `compile(CompileRequest request) : List<OptimizationResult>`
- **경로**: `POST /api/compile`
- **입력**: `{ "code": "C 소스 코드 문자열" }`
- **동작**: `@Valid`로 `CompileRequest`를 검증한 뒤, `CompilerService.compareOptimizationLevels()`에 그대로 위임
- **출력**: O0~O3 각각의 결과를 담은 `OptimizationResult` 리스트 (JSON 배열)
- 검증 실패(코드가 비어있거나 20,000자 초과) 시 Spring이 자동으로 400 Bad Request를 반환하고,
  `GlobalExceptionHandler`가 그 예외를 가로채 `{ message: "..." }` 형태로 응답을 정리함

---

## `CompilerService`
**파일**: [`backend/src/main/java/.../service/CompilerService.java`](../backend/src/main/java/com/compareopt/optvisualizer/service/CompilerService.java)

이 프로젝트의 핵심 로직. `clang`을 서브프로세스로 호출해서 실제 컴파일을 수행합니다.

### `compareOptimizationLevels(String sourceCode) : List<OptimizationResult>`
전체 흐름을 총괄하는 메서드.
1. `Files.createTempDirectory()`로 임시 작업 디렉터리 생성
2. 사용자 코드를 `input.c`로 저장
3. `LEVELS = ["O0", "O1", "O2", "O3"]`를 순회하며 각각 `runOneLevel()` 호출
4. 예외 발생 시(임시 디렉터리 생성 실패 등) 4개 레벨 모두 실패 결과로 채움
5. `finally` 블록에서 `cleanup()`으로 임시 파일 반드시 삭제

### `runOneLevel(Path workDir, Path sourceFile, String level) : OptimizationResult` (private)
특정 최적화 레벨 하나에 대한 컴파일을 수행.
1. `clang -S -O{level} input.c -o out.s` 실행 → 어셈블리 생성
   - 실패 시 `sanitizeStderr()`로 서버 임시 경로를 `input.c`로 치환한 뒤 `OptimizationResult.failure()` 반환
2. `clang -c -O{level} input.c -o out.o` 실행 → 오브젝트 파일 생성 (크기 비교용, `main()` 없어도 동작)
3. 오브젝트 파일 크기(`Files.size()`)를 읽고, 실패해도 무시(0으로 처리)하고 계속 진행
4. 어셈블리 파일 내용을 문자열로 읽어서 `OptimizationResult.success()`로 반환

### `run(List<String> command) : ProcessResult` (private)
`ProcessBuilder`로 실제 외부 프로세스(clang)를 실행하는 저수준 헬퍼.
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

## `OptimizationResult` (DTO)
**파일**: [`backend/src/main/java/.../dto/OptimizationResult.java`](../backend/src/main/java/com/compareopt/optvisualizer/dto/OptimizationResult.java)

레벨 하나의 컴파일 결과를 담는 응답 객체. 생성자를 `private`으로 막고
`success()` / `failure()` 정적 팩토리 메서드로만 만들 수 있게 해서, 성공/실패 상태에 맞지 않는
필드 조합(예: 실패인데 assembly가 채워짐)이 생기지 않도록 강제합니다.

- `success(level, assembly, binarySizeBytes, compileTimeMs)`
- `failure(level, errorMessage, compileTimeMs)`
