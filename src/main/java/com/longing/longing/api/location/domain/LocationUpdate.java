package com.longing.longing.api.location.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
public class LocationUpdate {
    private final String name;

    private final String mapUrl;

    private final String phoneNumber;

    private final Long categoryId;

    @Builder
    public LocationUpdate(
            @JsonProperty("name") String name,
            @JsonProperty("mapUrl") String mapUrl,
            @JsonProperty("phoneNumber") String phoneNumber,
            @JsonProperty("categoryId") Long categoryId
    ) {
        this.name = name;
        this.mapUrl = mapUrl;
        this.phoneNumber = phoneNumber;
        this.categoryId = categoryId;
    }
}
