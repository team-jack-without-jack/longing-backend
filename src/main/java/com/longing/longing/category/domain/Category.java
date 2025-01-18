package com.longing.longing.category.domain;

import com.longing.longing.location.domain.Location;
import com.longing.longing.user.domain.User;
import lombok.Builder;
import lombok.Getter;

@Getter
public class Category {

    private Long id;

    private String name;

    private String description;

    private Location location;

    @Builder
    public Category(Long id, String name, String description, Location location) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.location = location;
    }
}
