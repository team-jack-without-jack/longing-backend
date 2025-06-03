package com.longing.longing.common.domain;

import com.longing.longing.location.domain.Location;
import com.longing.longing.post.domain.Post;
import com.longing.longing.user.domain.User;
import lombok.Builder;
import lombok.Getter;

@Getter
public class LocationImage {

    private Long id;
    private String address;
    private Location location;
    private User user;

    @Builder
    public LocationImage(Long id, String address, Location location, User user) {
        this.id = id;
        this.address = address;
        this.location = location;
        this.user = user;
    }

    public static LocationImage from(String address, Location location, User user) {
        return LocationImage.builder()
                .address(address)
                .location(location)
                .user(user)
                .build();
    }
}
