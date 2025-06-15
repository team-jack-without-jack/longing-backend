package com.longing.longing.api.user.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
public class UserUpdate {
    private String name;

    private String nationality;
    private String introduction;

    @Builder
    public UserUpdate(
            @JsonProperty("name") String name,
            @JsonProperty("nationality") String nationality,
            @JsonProperty("introduction") String introduction) {
        this.name = name;
        this.nationality = nationality;
        this.introduction = introduction;
    }
}
