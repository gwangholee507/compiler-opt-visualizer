# 컴파일러 최적화 옵션 비교기

[![CI](https://github.com/gwangholee507/compiler-opt-visualizer/actions/workflows/ci.yml/badge.svg)](https://github.com/gwangholee507/compiler-opt-visualizer/actions/workflows/ci.yml)

C 코드를 입력하면 **선택한 컴파일러**(Clang 또는 GNU GCC)의 최적화 레벨(`-O0`~`-O3`)을 각각 적용해서
**어셈블리 코드가 어떻게 달라지는지, 컴파일된 코드 크기와 컴파일 시간, (옵션으로) 실제 실행 시간이
얼마나 차이 나는지**를 네 개 열로 나란히 보여주는 웹앱입니다.

"컴파일러 최적화가 대체 뭘 하는 거지?"를 직접 눈으로 확인해보기 위한 학습용 도구입니다.

## 이런 걸 볼 수 있어요

- `-O0` 대비 `-O1`~`-O3`에서 바뀐 어셈블리 줄이 초록색으로 강조되어, 최적화가
  코드의 어느 부분을 어떻게 바꿨는지 바로 눈에 들어옵니다.
- 각 레벨별 **오브젝트 파일 크기**와 **컴파일 시간**을 함께 비교합니다.
- **컴파일러를 Clang/GCC 중에서 골라서** 같은 코드를 서로 다른 컴파일러가 어떻게 최적화하는지 비교할 수 있습니다.
- (옵션) **실행 시간 측정**을 켜면 `main()`이 있는 코드를 실제로 링크·실행해서 O0~O3 간 실행 속도 차이까지 볼 수 있습니다.
- 문법 오류가 있는 코드를 넣으면 컴파일러의 원본 에러 메시지를 그대로 보여줍니다.

예를 들어 `square(int x) { return x * x; }` 같은 함수는 `-O0`에서는 스택에 값을
저장했다가 다시 읽어 곱하는 비효율적인 어셈블리가 나오지만, `-O1`부터는
`mul w0, w0, w0` 한 줄로 줄어드는 걸 확인할 수 있습니다.

## 기술 스택

| 영역 | 구성 |
|---|---|
| Backend | Spring Boot (Java 17), `clang`/`gcc`를 서브프로세스로 호출해 컴파일·실행 수행 |
| Frontend | React + Vite |
| Compiler | 로컬 macOS의 clang (Xcode Command Line Tools) + 선택적으로 Homebrew GCC (`brew install gcc`) |
| CI | GitHub Actions ([워크플로 정의](.github/workflows/ci.yml)) |

## 구조

```
.
├── backend/   # Spring Boot API 서버 — 컴파일 실행, 결과 비교
├── frontend/  # React UI — 코드 입력, 결과 렌더링
└── docs/      # 문서 모음 (아래 참고)
```

## 빠르게 실행해보기

### 준비물
- macOS (Xcode Command Line Tools에 포함된 `clang`)
- Java 17 이상, Maven
- Node.js, npm

```bash
# 터미널 1 — 백엔드 (기본 포트 8080)
cd backend
mvn spring-boot:run
```

```bash
# 터미널 2 — 프론트엔드 (기본 포트 5173)
cd frontend
npm install
npm run dev
```

브라우저에서 프론트엔드 주소(보통 `http://localhost:5173`)를 열면 됩니다.
백엔드와 프론트엔드 둘 다 켜져 있어야 정상 동작합니다.

## 테스트

```bash
cd backend && ./run-tests.sh   # mvn test도 가능하지만, JDK 17로 고정 실행해줌
cd frontend && npm test
```

> 시스템 `mvn`이 최신 Java를 기본으로 쓰도록 고정되어 있으면 Mockito가 깨지는 환경이 있어
> `run-tests.sh`가 JDK 17로 고정해서 실행해줍니다.

## 더 알아보기 (문서)

처음 오셨다면 아래 순서로 읽어보시는 걸 추천합니다.

| 문서 | 내용 |
|---|---|
| [docs/user-guide.md](docs/user-guide.md) | **사용자 가이드** — 실행 방법, 화면 사용법, 결과 읽는 법, FAQ (코드 몰라도 됨) |
| [docs/architecture.md](docs/architecture.md) | 요청이 프론트엔드 → 백엔드 → clang까지 어떻게 흘러가는지 전체 흐름도 |
| [docs/backend.md](docs/backend.md) | Spring Boot 백엔드 클래스/함수별 설명 |
| [docs/frontend.md](docs/frontend.md) | React 프론트엔드 컴포넌트/함수별 설명 |
| [docs/README.md](docs/README.md) | 문서 전체 목차 |

기여하려는 경우 브랜치/커밋 규칙도 확인해주세요.

| 문서 | 내용 |
|---|---|
| [docs/branching-strategy.md](docs/branching-strategy.md) | Git 브랜치 전략 (GitHub Flow) |
| [docs/commit-convention.md](docs/commit-convention.md) | 커밋 메시지 규칙 (Conventional Commits) |
| [CLAUDE.md](CLAUDE.md) | Claude Code로 작업할 때 지켜야 할 프로젝트 규칙 |

## 참고 자료

- [Clang 공식 문서 — 최적화 옵션](https://clang.llvm.org/docs/CommandGuide/clang.html) — `-O0`~`-O3`이 실제로 어떤 옵션 집합인지
- [Compiler Explorer (godbolt.org)](https://godbolt.org/) — 더 다양한 컴파일러/아키텍처로 어셈블리를 비교하고 싶다면
- [LLVM 최적화 파이프라인 개요](https://llvm.org/docs/Passes.html) — clang이 내부적으로 어떤 최적화 패스를 거치는지 더 깊게 알고 싶다면

## 참고

- 로컬 macOS의 clang(Xcode Command Line Tools)을 그대로 사용합니다.
- 현재는 로컬 개발 단계라 별도 샌드박싱 없이 컴파일을 실행합니다.
  외부에 배포할 계획이 생기면 Docker 등으로 격리 + 리소스 제한을 추가해야 합니다.
