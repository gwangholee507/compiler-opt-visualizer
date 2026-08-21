# 커밋 메시지 규칙 (Conventional Commits)

이 프로젝트는 [Conventional Commits](https://www.conventionalcommits.org/) 형식을 사용합니다.

```
<type>(<scope>): <설명>

[본문 - 선택]

[footer - 선택]
```

- **scope**는 생략 가능합니다. 붙일 땐 `backend`, `frontend`, `docs`, `ci` 처럼 영향 범위를 씁니다.
- **설명**은 한글로 써도 됩니다. 명령형("~한다")보다는 "~함/~수정" 식으로 간결하게.
- 제목은 50자 내외를 넘지 않도록, 본문이 필요하면 빈 줄 하나 띄우고 자유롭게 씁니다.

## type 목록

| type | 용도 |
|---|---|
| `feat` | 새 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서만 변경 (코드 변경 없음) |
| `refactor` | 동작은 그대로, 코드 구조/가독성 개선 |
| `test` | 테스트 추가/수정 |
| `chore` | 빌드, 의존성, 설정 등 잡무 |
| `ci` | CI 워크플로 변경 |
| `perf` | 성능 개선 |
| `style` | 포맷팅 등 코드 의미에 영향 없는 변경 |

## 예시

```
feat(backend): O2 컴파일 시 심볼 테이블 비교 추가
fix(frontend): 코드 입력창에서 탭 키 눌렀을 때 포커스 이탈되는 문제 수정
docs: 브랜치 전략 문서 추가
chore: mvn wrapper 버전 업데이트
refactor(backend): CompilerService의 중복 로직을 함수로 추출
```

## Breaking Change

호환성이 깨지는 변경이면 footer에 `BREAKING CHANGE:`를 명시합니다.

```
feat(backend): 컴파일 결과 API 응답 형식 변경

BREAKING CHANGE: /api/compile 응답의 `size` 필드가 `objectSize`로 이름 변경됨
```

## 커밋 단위

- 커밋 하나는 하나의 논리적 변경만 담습니다 (기능 추가와 무관한 포맷팅 변경을 섞지 않기).
- PR을 올리기 전 커밋이 지저분하면 `git rebase -i`로 정리해도 되지만, 필수는 아닙니다 —
  머지 방식이 Squash merge([branching-strategy.md](./branching-strategy.md) 참고)라
  PR 안 커밋 히스토리가 `main`에 그대로 남지 않습니다.
