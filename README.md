# 컴파일러 최적화 옵션 비교기

C 코드를 clang의 `-O0`~`-O3` 최적화 레벨로 각각 컴파일해서
어셈블리, 오브젝트 파일 크기, 컴파일 시간을 나란히 비교해보는 웹앱.

## 구조
- `backend/` — Spring Boot (Java). `clang`을 서브프로세스로 호출해 컴파일 수행
- `frontend/` — React (Vite). 코드 입력 + 결과 비교 UI

## 실행 방법

### 백엔드
```bash
cd backend
mvn spring-boot:run
```
기본 포트: 8080

### 프론트엔드
```bash
cd frontend
npm install
npm run dev
```
기본 포트: 5173

## 참고
- 로컬 macOS의 clang(Xcode Command Line Tools)을 그대로 사용합니다.
- 현재는 로컬 개발 단계라 별도 샌드박싱 없이 컴파일을 실행합니다.
  외부에 배포할 계획이 생기면 Docker 등으로 격리 + 리소스 제한을 추가해야 합니다.
