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

    private Provider provider;

    private Role role;

    @Builder
    public User(Long id, String name, String email, Provider provider, Role role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.provider = provider;
        this.role = role;
    }
}
