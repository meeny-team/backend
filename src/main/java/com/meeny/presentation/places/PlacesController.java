package com.meeny.presentation.places;

import com.meeny.application.places.PlacesService;
import com.meeny.common.response.ApiResponse;
import com.meeny.presentation.places.dto.PlaceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlacesController {

    private final PlacesService placesService;

    // 카카오 Local 키워드 검색 프록시 — 클라이언트에 API 키를 노출하지 않기 위한 백엔드 경유 호출
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<PlaceResponse>>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Integer radius) {
        List<PlaceResponse> places = placesService.searchByKeyword(query, page, latitude, longitude, radius);
        return ResponseEntity.ok(ApiResponse.ok(places));
    }
}
