package com.meeny.presentation.places.dto;

import com.meeny.domain.places.Place;

public record PlaceResponse(
        String id,
        String name,
        String category,
        String address,
        String roadAddress,
        String phone,
        double latitude,
        double longitude
) {
    public static PlaceResponse from(Place place) {
        return new PlaceResponse(
                place.id(),
                place.name(),
                place.category(),
                place.address(),
                place.roadAddress(),
                place.phone(),
                place.latitude(),
                place.longitude()
        );
    }
}
