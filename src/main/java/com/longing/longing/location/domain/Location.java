package com.longing.longing.location.domain;

import com.longing.longing.category.domain.Category;
import com.longing.longing.common.domain.LocationImage;
import com.longing.longing.post.domain.Post;
import com.longing.longing.post.domain.PostCreate;
import com.longing.longing.user.domain.User;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
public class Location {

    private Long id;

    private String name;

    private String mapUrl;

    private String phoneNumber;

    private String address;
    private User user;

    private Category category;
    private List<LocationImage> locationImageList;

    @Builder
    public Location(Long id, String name, String mapUrl, String phoneNumber, String address, User user, Category category, List<LocationImage> locationImageList) {
        this.id = id;
        this.name = name;
        this.mapUrl = mapUrl;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.user = user;
        this.category = category;
        this.locationImageList = locationImageList;
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
