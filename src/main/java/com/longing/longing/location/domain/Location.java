package com.longing.longing.location.domain;

import com.longing.longing.category.domain.Category;
import com.longing.longing.post.domain.Post;
import com.longing.longing.post.domain.PostCreate;
import com.longing.longing.user.domain.User;
import lombok.Builder;
import lombok.Getter;

@Getter
public class Location {

    private Long id;

    private String name;

    private String mapUrl;

    private String phoneNumber;

    private User user;

    private Category category;

    @Builder
    public Location(Long id, String name, String mapUrl, String phoneNumber, User user, Category category) {
        this.id = id;
        this.name = name;
        this.mapUrl = mapUrl;
        this.phoneNumber = phoneNumber;
        this.user = user;
        this.category = category;
    }

    public static Location from(User user, Category category, LocationCreate locationCreate) {
        return Location.builder()
                .name(locationCreate.getName())
                .mapUrl(locationCreate.getMapUrl())
                .phoneNumber(locationCreate.getPhoneNumber())
                .user(user)
                .category(category)
                .build();
    }
//    public static Location from() {
//        return Location.builder()
//                .
//                .title(postCreate.getTitle())
//                .content(postCreate.getContent())
//                .user(user)
//                .build();
//    }
}
