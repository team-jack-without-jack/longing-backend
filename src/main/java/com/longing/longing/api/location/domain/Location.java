package com.longing.longing.api.location.domain;

import com.longing.longing.api.user.domain.User;
import com.longing.longing.api.category.domain.Category;
import lombok.Builder;
import lombok.Getter;

@Getter
public class Location {

    private Long id;

    private String name;

    private String mapUrl;

    private String phoneNumber;

    private String address;
    private User user;

    private Category category;

    @Builder
    public Location(Long id, String name, String mapUrl, String phoneNumber, String address, User user, Category category) {
        this.id = id;
        this.name = name;
        this.mapUrl = mapUrl;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.user = user;
        this.category = category;
    }

    public static Location from(User user, Category category, LocationCreate locationCreate) {
        return Location.builder()
                .name(locationCreate.getName())
                .mapUrl(locationCreate.getMapUrl())
                .phoneNumber(locationCreate.getPhoneNumber())
                .address(locationCreate.getAddress())
                .user(user)
                .category(category)
                .build();
    }

    public Location update(Category category, LocationUpdate locationUpdate) {
        if (locationUpdate.getName() != null) {
            this.name = locationUpdate.getName();
        }
        if (locationUpdate.getMapUrl() != null) {
            this.mapUrl= locationUpdate.getMapUrl();
        }
        if (locationUpdate.getPhoneNumber() != null) {
            this.phoneNumber = locationUpdate.getPhoneNumber();
        }
        if (locationUpdate.getCategoryId() != null) {
            this.category = category;
        }
        return this;
    }
}
