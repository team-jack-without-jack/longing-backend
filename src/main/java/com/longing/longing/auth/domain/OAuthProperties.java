package com.longing.longing.auth.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

//@Getter
//@Setter
//@Component
//@ConfigurationProperties(prefix = "oauth")
@Getter
@Component
public class OAuthProperties {
//    private Map<String, OAuthProviderInfo> google;
//    private Map<String, OAuthProviderInfo> facebook;
//    private Map<String, OAuthProviderInfo> kakao;
//    private OAuthProviderInfo google;
//    private OAuthProviderInfo facebook;
//    private OAuthProviderInfo kakao;
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String googleRedirectUri;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String kakaoClientSecret;

    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    @Value("${spring.security.oauth2.client.registration.facebook.client-id}")
    private String facebookClientId;

    @Value("${spring.security.oauth2.client.registration.facebook.client-secret}")
    private String facebookClientSecret;

    @Value("${spring.security.oauth2.client.registration.facebook.redirect-uri}")
    private String facebookRedirectUri;


    @Value("${spring.security.oauth2.client.registration.apple.client-id}")
    private String appleClientId;

    @Value("${spring.security.oauth2.client.registration.apple.redirect-uri}")
    private String appleRedirectUri;

//    @Value("${spring.security.oauth2.client.registration.apple.client-id}")
//    private String appleClientId;

}
