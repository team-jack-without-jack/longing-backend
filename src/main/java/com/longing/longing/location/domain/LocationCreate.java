package com.longing.longing.location.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
public class LocationCreate {
    private final String name;

    private final String mapUrl;

    private final String phoneNumber;

    private final Long categoryId;

    @Builder
    public LocationCreate(
            String name,
            String mapUrl,
            String phoneNumber,
            Long categoryId
    ) {
        this.name = name;
        this.mapUrl = mapUrl;
        this.phoneNumber = phoneNumber;
        this.categoryId = categoryId;
    }
}
