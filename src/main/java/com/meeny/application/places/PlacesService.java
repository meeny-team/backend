package com.meeny.application.places;

import com.meeny.domain.places.Place;
import com.meeny.domain.places.PlaceSearchClient;
import com.meeny.domain.places.PlaceSearchClient.Coordinate;
import com.meeny.presentation.places.dto.PlaceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlacesService {

    private final PlaceSearchClient placeSearchClient;

    // 키워드 기반 장소 검색: 좌표가 모두 주어지면 반경 기반, 아니면 전국 검색.
    public List<PlaceResponse> searchByKeyword(
            String query, int page, Double latitude, Double longitude, Integer radiusMeters) {
        Coordinate coordinate = (latitude != null && longitude != null)
                ? new Coordinate(latitude, longitude, radiusMeters != null ? radiusMeters : 1000)
                : null;
        List<Place> places = placeSearchClient.searchByKeyword(query, page, coordinate);
        return places.stream().map(PlaceResponse::from).toList();
    }
}
