# CLAUDE.md

## 프로젝트 개요
Meeny는 Spring Boot 3, Java 21, Gradle 기반의 백엔드 프로젝트입니다.
이 프로젝트는 DDD 스타일 아키텍처를 지향하며, React Native 클라이언트를 위한 API를 제공합니다.

## 아키텍처 원칙
도메인 중심으로 패키지를 구성합니다.

예시:
- member
- auth
- order
- payment

각 도메인은 다음 계층으로 구성할 수 있습니다:
- presentation
- application
- domain
- infrastructure

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