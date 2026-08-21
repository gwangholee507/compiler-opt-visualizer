# 전체 흐름도

```
[브라우저]
   │  1. 사용자가 C 코드 입력 후 "비교하기" 클릭
   ▼
[frontend/src/App.jsx] handleCompare()
   │  2. POST http://localhost:8080/api/compile  { code: "..." }
   ▼
[backend] CompileController.compile()
   │  3. @Valid 로 코드 검증 (비어있음/20,000자 초과 시 400 에러)
   ▼
[backend] CompilerService.compareOptimizationLevels()
   │  4. 임시 디렉터리 생성 → input.c 로 저장
   │  5. O0, O1, O2, O3 각각에 대해 runOneLevel() 반복 호출
   │       - clang -S -O{n} → 어셈블리(.s) 생성
   │       - clang -c -O{n} → 오브젝트 파일(.o) 생성 (크기 비교용)
   │  6. 임시 디렉터리 정리
   ▼
[backend] List<OptimizationResult> 를 JSON으로 응답
   │
   ▼
[frontend/src/App.jsx] setResults(data)
   │  7. O0 결과를 기준(baseline)으로 삼아
   ▼
[frontend/src/OptimizationColumn + AssemblyView]
   │  8. diff.js 의 diffLines() 로 O0 대비 달라진 줄 계산
   ▼
[화면] O0~O3 컬럼을 나란히 렌더링, 바뀐 줄은 초록색 하이라이트
```

## 왜 오브젝트 파일(.o)로 크기를 재나요?

사용자가 입력하는 C 코드에는 보통 `main()` 함수가 없습니다 (`square`, `sum_loop` 같은 순수 함수만 있음).
`main()`이 없으면 실행 파일로 **링크**할 수 없어서(`ld: symbol(s) not found for "_main"`),
링크 단계 없이 컴파일만 하는 `-c` 옵션으로 오브젝트 파일을 만들어 크기를 비교합니다.
이 방식은 링크 오버헤드가 섞이지 않아서 오히려 "순수 코드젠 크기" 비교에는 더 적합합니다.

## 보안 관련 주의사항

`CompilerService`는 사용자가 입력한 코드를 서버 프로세스 권한으로 그대로 컴파일합니다.
지금은 로컬 개인 프로젝트 단계라 별도 샌드박싱이 없습니다. 외부에 배포할 계획이 생기면
Docker 컨테이너 격리 + CPU/메모리 제한을 반드시 추가해야 합니다.
