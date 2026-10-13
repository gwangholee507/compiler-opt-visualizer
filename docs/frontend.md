# 프론트엔드 (React + Vite)

## `diff.js`
**파일**: [`frontend/src/diff.js`](../frontend/src/diff.js)

외부 라이브러리 없이 직접 구현한 줄 단위 diff 유틸리티.

### `diffLines(base, target) : { lineStates, addedCount, removedCount }`
- **입력**: `base`(기준이 되는 줄 배열, 보통 O0 어셈블리), `target`(비교 대상 줄 배열)
- **알고리즘**: LCS(최장 공통 부분 수열)를 DP 테이블(`dp[i][j]`)로 계산한 뒤, 뒤에서부터 되짚어가며(backtrack)
  - `base[i] === target[j]`면 `'same'`
  - 아니고 `base`에서 삭제하는 게 유리하면(`dp[i+1][j] >= dp[i][j+1]`) `removedCount` 증가
  - 그 외에는 `target`에 새로 추가된 줄이므로 `'added'`
- **출력**:
  - `lineStates`: `target`의 각 줄에 대응하는 `'same' | 'added'` 배열 (인덱스가 `target` 줄 번호와 1:1 대응)
  - `addedCount`: `target`에만 있는(새로 생기거나 바뀐) 줄 수
  - `removedCount`: `base`에만 있고 `target`에는 없는(사라진) 줄 수
- **복잡도**: O(n·m) — 어셈블리가 보통 수백 줄 이내라 브라우저에서도 충분히 빠름

---

## `App.jsx`
**파일**: [`frontend/src/App.jsx`](../frontend/src/App.jsx)

### 상수
- `DEFAULT_CODE`: 텍스트 에디터에 처음 채워지는 예시 C 코드 (`square`, `sum_loop`)
- `BACKEND_UNREACHABLE`: 서버에 연결하지 못했을 때 보여주는 안내 문구
- 백엔드 주소는 상수로 두지 않습니다. API는 항상 화면을 내려준 서버의 상대 경로(`/api/compilers`,
  `/api/compile`)로 호출합니다 — 배포된 jar에서는 백엔드가 화면도 함께 내보내고, 개발 중에는 Vite 개발 서버가
  `/api`를 백엔드로 넘겨주기 때문입니다 (아래 `vite.config.js` 참고).
- `OPTIMIZATION_INFO`: `O0`~`O3` 각 레벨이 실제로 어떤 최적화를 하는지 초보자용으로 풀어쓴 설명 맵.
  `InfoTooltip`에 전달되는 텍스트의 원본

### `InfoTooltip({ text })` (컴포넌트)
컬럼 헤더의 ⓘ 아이콘. 마우스 호버 또는 `tabIndex={0}`을 통한 키보드 포커스 시
`text`를 말풍선(`tooltip-bubble`)으로 보여준다. CSS의 `:hover`/`:focus` 선택자로만 동작하며
별도 JS 상태(useState) 없이 구현됨.

### `formatBytes(bytes) : string`
바이트 숫자를 사람이 읽기 좋은 문자열로 변환.
- `null`이면 `'-'`
- 1024 미만이면 `"712 B"`처럼 그대로
- 그 이상이면 `"0.6 KB"`처럼 소수점 1자리 KB로 변환

### `formatMs(ms) : string`
밀리초 숫자(`OptimizationResult.executionTimeMs`)를 사람이 읽기 좋은 문자열로 변환.
- `null`이면 `'-'`
- 1ms 미만이면 마이크로초로 바꿔 `"420µs"`처럼 표시 (짧은 함수는 실행 시간이 1ms를 안 넘는 경우가 많아서)
- 그 이상이면 `"3.42ms"`처럼 소수점 2자리 ms로 표시

### `AssemblyView({ assembly, baseAssembly })` (컴포넌트)
어셈블리 텍스트를 줄 단위 `<div>`로 쪼개서 렌더링.
- `baseAssembly`가 없으면(= O0 자신이거나 기준이 없는 경우) 그냥 평문으로 표시
- `baseAssembly`가 있으면 `diffLines()`로 각 줄이 `'added'`인지 계산해서, 새로 생긴/바뀐 줄에
  `diff-added` CSS 클래스를 붙여 초록색으로 강조

### `OptimizationColumn({ result, baseAssembly })` (컴포넌트)
결과 화면의 컬럼 하나(예: "-O2" 박스)를 그리는 컴포넌트.
- 헤더에 레벨 이름(`-O0`) + `InfoTooltip`, 바이너리(오브젝트) 크기, 컴파일 시간(ms) 표시
- O0이 아니고 컴파일에 성공했으면 `diffLines()`로 `(+added / -removed vs O0)` 배지를 헤더에 추가
- `result.executionTimeMs`가 있으면(= `runBenchmark=true`였고 측정 성공) `⏱ 실행 {formatMs(...)}` 배지 추가
- 측정을 시도했지만 실행 파일을 못 만들었으면(주로 `main()` 없음) `executionError`를 툴팁으로 붙인
  회색 "⏱ 실행 불가" 배지를 대신 표시 — 컴파일 자체는 성공이므로 에러로 취급하지 않음
- 컴파일 실패 시 헤더에 빨간 "컴파일 실패" 배지를 띄우고, 본문에는
  `"⚠ {compilerLabel}가 이 코드를 컴파일하지 못했습니다"` 안내와 함께 `errorMessage`(정리된 컴파일러 stderr)를 표시

### `App()` (최상위 컴포넌트)
페이지 전체를 구성하는 루트 컴포넌트. 상태 7개를 관리:
- `code`: 텍스트 에디터에 입력된 C 코드
- `results`: 백엔드 응답(`OptimizationResult` 배열), 아직 요청 전이면 `null`
- `loading`: 요청 진행 중 여부 (버튼 비활성화 + "컴파일 중..." 텍스트에 사용)
- `error`: 요청 실패 시 에러 메시지
- `compilers`: `GET /api/compilers` 응답(`{id, label, available}` 배열) — 드롭다운 옵션 목록
- `selectedCompiler`: 드롭다운에서 선택된 컴파일러 id (기본 `'clang'`)
- `runBenchmark`: "실행 시간 측정" 체크박스 상태 (기본 `false`, 옵트인)

#### `useEffect` (마운트 시 1회)
`GET /api/compilers`를 호출해 `compilers`를 채우고, 사용 가능한(`available: true`) 첫 컴파일러를
`selectedCompiler`로 자동 선택한다. 요청이 실패해도 조용히 빈 배열로 두고(드롭다운에 "Clang (LLVM)"
기본값만 남음) 화면이 깨지지 않게 한다.

#### `handleCompare()` (내부 함수, "비교하기" 버튼의 onClick)
1. `loading = true`, 이전 `results`/`error` 초기화
2. `POST /api/compile`로 `{ code, compiler: selectedCompiler, runBenchmark }`를 JSON으로 전송
3. 응답이 실패(`!res.ok`)면 에러 바디(`{ message }`, 백엔드의 `GlobalExceptionHandler`가 내려줌)를 파싱해
   메시지를 뽑고, 없으면 HTTP 상태코드로 대체 메시지 생성
   - 상태코드가 502/503/504면 `BACKEND_UNREACHABLE` 문구를 씀. 개발 서버의 프록시는 백엔드가 꺼져 있으면 502를 돌려줌
4. 성공하면 `setResults(data)`
5. `fetch` 자체가 실패(`TypeError`)하면 서버에 연결하지 못한 것이므로 `BACKEND_UNREACHABLE` 문구를 표시
   (jar로 실행하다 서버를 끈 경우)
6. `finally`에서 `loading = false` (성공/실패 모두)

#### 렌더링 흐름
1. 상단에 제목/설명, 코드 에디터(`<textarea>`)
2. 컴파일러 선택 `<select>`(사용 불가능한 항목은 `disabled`) + "실행 시간 측정" 체크박스(+ 설명 툴팁)
3. "비교하기" 버튼, 에러 메시지(있으면)
4. `results`가 있으면:
   - 어떤 컴파일러 기준 결과인지 안내 문구(`results[0].compilerLabel`) 표시
   - `results`에서 `level === 'O0'`인 항목을 찾아 `baseAssembly`로 지정 (O0이 실패했으면 `null`)
   - 각 결과를 `OptimizationColumn`으로 렌더링하며 `baseAssembly`를 함께 전달

---

## `vite.config.js`
**파일**: [`frontend/vite.config.js`](../frontend/vite.config.js)

- `server.proxy`: 개발 서버(5173)로 온 `/api` 요청을 백엔드(`http://127.0.0.1:8080`)로 넘깁니다.
  덕분에 `App.jsx`는 개발 중에도, 배포된 jar에서도 똑같이 `/api/...`만 호출하면 됩니다.
  - `localhost`가 아니라 `127.0.0.1`을 쓰는 이유: 백엔드는 `127.0.0.1`에서만 듣는데, 환경에 따라
    `localhost`가 IPv6 주소(`::1`)로 먼저 풀려 연결이 거부될 수 있기 때문입니다.
- `npm run build`의 결과물(`frontend/dist`)은 백엔드의 `bundle` Maven 프로필이 jar 안에 넣습니다
  ([backend.md](./backend.md#bundle-프로필--실행-가능한-jar-하나-만들기) 참고).

---

## `main.jsx`
**파일**: [`frontend/src/main.jsx`](../frontend/src/main.jsx)

Vite 기본 진입점. `#root` DOM에 `<App />`을 `ReactDOM.createRoot()`로 마운트합니다. (Vite 템플릿 기본 생성 파일, 프로젝트 고유 로직 없음)
