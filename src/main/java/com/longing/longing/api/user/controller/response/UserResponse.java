package com.longing.longing.api.user.controller.response;

import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.Role;
import com.longing.longing.api.user.domain.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.function.Function;

@Getter
public class UserResponse {
    private final Long id;
    private final String name;
    private final String email;
    private final String picture;
    private final Provider provider;
    private final String providerId;
    private final Role role;
    private final String nationality;
    private final String introduction;
    private final LocalDateTime createdDate;
    private final LocalDateTime modifiedDate;

    @Builder
    public UserResponse(
            Long id,
            String name,
            String email,
            String picture,
            Provider provider,
            String providerId,
            Role role,
            String nationality,
            String introduction,
            LocalDateTime createdDate,
            LocalDateTime modifiedDate) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.picture = picture;
        this.provider = provider;
        this.providerId = providerId;
        this.role = role;
        this.nationality = nationality;
        this.introduction = introduction;
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
    }

    public static UserResponse fromDomain(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .picture(user.getPicture())
                .provider(user.getProvider())
                .providerId(user.getProviderId())
                .role(user.getRole())
                .nationality(user.getNationality())
                .introduction(user.getIntroduction())
                .createdDate(user.getCreatedDate())
                .modifiedDate(user.getModifiedDate())
                .build();
    }
}
