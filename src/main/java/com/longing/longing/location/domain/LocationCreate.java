package com.longing.longing.location.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
public class LocationCreate {
    private final String name;

    private final String mapUrl;

    private final String phoneNumber;
    private final String address;

    private final Long categoryId;

    @Builder
    public LocationCreate(
            @JsonProperty("name") String name,
            @JsonProperty("mapUrl") String mapUrl,
            @JsonProperty("phoneNumber") String phoneNumber,
            @JsonProperty("address") String address,
            @JsonProperty("categoryId") Long categoryId
    ) {
        this.name = name;
        this.mapUrl = mapUrl;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.categoryId = categoryId;
    }
}
