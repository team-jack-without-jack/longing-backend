package com.longing.longing.config.auth.dto;

import com.longing.longing.user.infrastructure.UserEntity;
import lombok.Getter;

@Getter
public class SessionUser {
//    private Long id;

    private String name;
    private String email;
    private String picture;

    public SessionUser(UserEntity userEntity) {
//        this.id = userEntity.getId();
        this.name = userEntity.getName();
        this.email = userEntity.getEmail();
        this.picture = userEntity.getPicture();
    }
}
