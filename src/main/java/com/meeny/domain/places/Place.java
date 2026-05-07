package com.meeny.domain.places;

// 장소 검색 결과의 도메인 표현. 외부(카카오) 응답 필드명에 의존하지 않도록 분리.
public record Place(
        String id,
        String name,
        String category,
        String address,
        String roadAddress,
        String phone,
        double latitude,
        double longitude
) {}
