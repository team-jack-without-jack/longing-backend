package com.longing.longing.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Provider {

    FACEBOOK("facebook", "페이스북"),
    KAKAO("kakao", "카카오"),
    GOOGLE("google", "구글"),
    APPLE("apple", "애플");

    private final String key;
    private final String title;
}
