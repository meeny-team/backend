package com.meeny.domain.places;

import java.util.List;

// 장소 검색 외부 의존(카카오 Local 등). application 계층은 이 인터페이스에만 의존.
public interface PlaceSearchClient {

    // 키워드 검색. coordinate 가 null 이면 전국 검색, 아니면 좌표 기반 반경 검색.
    List<Place> searchByKeyword(String query, int page, Coordinate coordinate);

    record Coordinate(double latitude, double longitude, int radiusMeters) {}
}
