package com.longing.longing.common.domain;

import com.longing.longing.api.location.domain.Location;
import com.longing.longing.api.user.domain.User;
import lombok.Builder;
import lombok.Getter;

@Getter
public class LocationImage {

    private Long id;
    private String address;
    private Location location;
    private User user;
    private Boolean isThumbnail;

    @Builder
    public LocationImage(Long id, String address, Location location, User user, Boolean isThumbnail) {
        this.id = id;
        this.address = address;
        this.location = location;
        this.user = user;
        this.isThumbnail = isThumbnail;
    }

    public static LocationImage from(String address, Location location, User user, Boolean isThumbnail) {
        return LocationImage.builder()
                .address(address)
                .location(location)
                .user(user)
                .isThumbnail(isThumbnail)
                .build();
    }
}
