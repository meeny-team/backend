package com.meeny.infrastructure.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.meeny.common.exception.BusinessException;
import com.meeny.common.exception.ErrorCode;
import com.meeny.domain.places.Place;
import com.meeny.domain.places.PlaceSearchClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.List;

@Component
public class KakaoPlaceSearchClient implements PlaceSearchClient {

    private static final int PAGE_SIZE = 10;

    private final WebClient webClient;
    private final String restApiKey;
    private final String keywordSearchUri;

    public KakaoPlaceSearchClient(
            WebClient.Builder builder,
            @Value("${kakao.local.rest-api-key:}") String restApiKey,
            @Value("${kakao.local.keyword-search-uri:https://dapi.kakao.com/v2/local/search/keyword.json}")
            String keywordSearchUri
    ) {
        this.webClient = builder.build();
        this.restApiKey = restApiKey;
        this.keywordSearchUri = keywordSearchUri;
    }

    @Override
    public List<Place> searchByKeyword(String query, int page, Coordinate coordinate) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        URI uri = buildUri(query, page, coordinate);

        KakaoSearchResponse response = webClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + restApiKey)
                .retrieve()
                .bodyToMono(KakaoSearchResponse.class)
                .onErrorMap(WebClientResponseException.class, e -> new BusinessException(ErrorCode.PLACES_SEARCH_FAILED))
                .block();

        if (response == null || response.documents() == null) {
            return Collections.emptyList();
        }
        return response.documents().stream().map(KakaoPlaceSearchClient::toPlace).toList();
    }

    private URI buildUri(String query, int page, Coordinate coordinate) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(keywordSearchUri)
                .queryParam("query", query)
                .queryParam("page", Math.max(1, page))
                .queryParam("size", PAGE_SIZE);
        if (coordinate != null) {
            uriBuilder
                    .queryParam("x", coordinate.longitude())
                    .queryParam("y", coordinate.latitude())
                    .queryParam("radius", coordinate.radiusMeters());
        }
        return uriBuilder.build().encode().toUri();
    }

    private static Place toPlace(KakaoDocument doc) {
        return new Place(
                doc.id(),
                doc.placeName(),
                doc.categoryName(),
                doc.addressName(),
                doc.roadAddressName(),
                doc.phone(),
                parseDouble(doc.y()),
                parseDouble(doc.x())
        );
    }

    private static double parseDouble(String value) {
        if (value == null || value.isBlank()) return 0.0;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private record KakaoSearchResponse(List<KakaoDocument> documents) {}

    // 카카오 응답은 snake_case → @JsonProperty 로 명시 매핑. 좌표 x/y 는 문자열로 옴.
    private record KakaoDocument(
            String id,
            @JsonProperty("place_name") String placeName,
            @JsonProperty("category_name") String categoryName,
            @JsonProperty("address_name") String addressName,
            @JsonProperty("road_address_name") String roadAddressName,
            String phone,
            String x,
            String y
    ) {}
}
