package com.longing.longing.api.location.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
public class LocationCreate {

    @NotBlank
    private final String name;

    @Size(max = 100)
    private final String mapUrl;

    @Size(max = 20)
    private final String phoneNumber;

    @Size(max = 100)
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
