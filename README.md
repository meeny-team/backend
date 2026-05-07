# meeny

## 프로젝트 개요
meeny는 Spring Boot 기반으로 개발한 백엔드 프로젝트 

---


## 기술 스택

| 분류 | 기술 |
|------|------|
| Backend | Spring Boot 3, Java 21, Gradle |
| Authentication | JWT |
| Infrastructure | AWS EC2, AWS S3, Docker, Kubernetes, GitHub Actions |
| Database | MySQL (Prod), H2 (Dev/Test) |


---
## 주요 기능

### 인증 (`/api/auth`)
- 소셜 로그인 (Kakao, Google, Apple) — 액세스/리프레시 토큰 발급
- 리프레시 토큰 회전, 재사용 감지 시 세션 무효화
- 로그아웃 — 리프레시 토큰 즉시 폐기
- 로컬 개발용 dev-login (운영 프로필에서 자동 비활성)

### 회원 (`/api/users`)
- 내 프로필 조회 / 수정 (닉네임, 프로필 이미지, 자기소개)
- 회원 탈퇴 — 정산 외래키 무결성을 위해 소프트 딜리트, 토큰만 즉시 폐기

### 크루 (`/api/crews`)
- 크루 생성 — 6자리 초대 코드 자동 발급
- 초대 코드로 가입 / 탈퇴
- 내가 속한 크루 목록 조회, 크루 상세 조회

### 플레이 (`/api/plays`, `/api/crews/{crewId}/plays`)
- 플레이 생성 / 수정 / 삭제 — 멤버는 크루 멤버 부분집합으로만 가능
- 크루의 플레이 목록 / 플레이 상세 조회
- 정산이 마감된 플레이는 mutation 차단

### 핀 (`/api/pins`, `/api/plays/{playId}/pins`)
- 핀 생성 / 수정 / 삭제 — 결제자·분담자가 플레이 멤버인지 검증, 분담 금액 합 = 총 금액
- 균등 분배(equal) / 사용자 지정 분배(custom) 두 정산 타입 지원
- 플레이의 핀 목록 / 핀 상세 조회

### 정산 (`/api/plays/{playId}/settlement`)
- 정산 계산 — 모든 핀의 결제/분배를 합산해 멤버별 잔액과 최적 송금 내역 산출
- 정산 마감 (`/close`) — 모든 멤버 잔액이 0일 때만 가능, 마감 후 mutation 차단

### 공통
- 표준 응답 포맷 `ApiResponse<T>` — 도메인 예외는 `BusinessException(ErrorCode)` 로 통일
- 무상태 JWT 인증 — Stateless 세션, `JwtAuthenticationFilter` 가 토큰 추출/검증
- 운영 스키마는 Flyway 로 관리 (`db/migration/V*__*.sql`), 로컬은 H2 + `ddl-auto: create-drop`

