package com.longing.longing.user.domain;

import com.longing.longing.user.Provider;
import com.longing.longing.user.Role;
import lombok.Builder;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

@Getter
public class User {

    private Long id;

    private String name;

    private String email;

    private String picture;

    private Provider provider;

    private String providerId;

    private Role role;

    private String nationality;
    private String introduction;

    @Builder
    public User(
            Long id,
            String name,
            String email,
            String picture,
            Provider provider,
            String providerId,
            Role role,
            String nationality,
            String introduction
    ) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.picture = picture;
        this.provider = provider;
        this.providerId = providerId;
        this.role = role;
        this.nationality = nationality;
        this.introduction = introduction;
    }

    public User update(UserUpdate userUpdate, String profileImageAddress) {
        if (userUpdate.getName() != null) {
            this.name = userUpdate.getName();
        }
        if (userUpdate.getNationality() != null) {
            this.nationality = userUpdate.getNationality();
        }
        if (userUpdate.getIntroduction() != null) {
            this.introduction = userUpdate.getIntroduction();
        }
        if (profileImageAddress != null) {
            this.picture = profileImageAddress;
        }
        return this;
    }

    public Long getIdByProviderId(String providerId) {
        return this.id;
    }
}
