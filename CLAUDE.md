# CLAUDE.md

## 프로젝트 개요
Meeny는 Spring Boot 3, Java 21, Gradle 기반의 백엔드 프로젝트입니다.
이 프로젝트는 DDD 스타일 아키텍처를 지향하며, React Native 클라이언트를 위한 API를 제공합니다.

## 아키텍처 원칙
계층(layer) 중심으로 패키지를 구성합니다.

최상위 패키지(`com.meeny`) 아래에 다음 계층 패키지를 둡니다:
- presentation : Controller, Request/Response DTO
- application : Application Service (유스케이스), Application 단의 헬퍼
- domain : Entity, Value Object, 도메인 인터페이스(Repository 인터페이스, 도메인 서비스), 도메인 행위
- infrastructure : Repository 구현(JPA), 외부 시스템 어댑터(OAuth 클라이언트 등)
- security : 인증 필터, JWT 관련 컴포넌트
- config : Spring Configuration 클래스
- common : 공통 예외, 공통 응답 포맷 등

각 계층 안에서는 도메인(`auth`, `member`, ...)별로 하위 패키지를 나눕니다.

예시:
- presentation/auth, presentation/member
- application/auth, application/member
- domain/auth, domain/member
- infrastructure/auth, infrastructure/member

새 도메인을 추가할 때는 각 계층 아래에 같은 이름의 하위 패키지를 추가합니다.
도메인 중심 구조(`auth/presentation`, `auth/application` ...)로 재배치하지 않습니다.

## 서비스 클래스 원칙
Application Service는 유스케이스 흐름이 쉽게 읽혀야 합니다.

서비스 메서드는:
- 이름만 봐도 의도가 드러나야 합니다
- 과도한 상세 비즈니스 로직을 직접 담지 않습니다
- 도메인 객체를 조회하고, 도메인 행위를 호출하고, 저장하는 흐름이 보이도록 작성합니다

핵심 비즈니스 규칙은 다음 위치를 우선 고려합니다:
- Entity
- Value Object
- Domain Service

모든 비즈니스 로직을 Service 클래스에 몰아넣지 않습니다.

## API 설계 원칙
이 백엔드는 React Native 클라이언트를 대상으로 합니다.

따라서 API 설계 시:
- request/response 형식을 일관되게 유지합니다
- JSON 구조를 단순하고 명확하게 유지합니다
- 에러 응답 형식을 일관되게 설계합니다
- 모바일 클라이언트에서 사용하기 쉽게 설계합니다