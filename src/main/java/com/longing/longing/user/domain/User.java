package com.longing.longing.user.domain;

import com.longing.longing.user.Provider;
import com.longing.longing.user.Role;
import lombok.Builder;
import lombok.Getter;

@Getter
public class User {

    private Long id;

    private String name;

    private String email;

    private String picture;

    private Provider provider;

    private String providerId;

    private Role role;

    @Builder
    public User(
            Long id,
            String name,
            String email,
            String picture,
            Provider provider,
            String providerId,
            Role role
    ) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.picture = picture;
        this.provider = provider;
        this.providerId = providerId;
        this.role = role;
    }

    public User update(String name, String picture) {
        this.name = name;
        this.picture = picture;
        return this;
    }
}
