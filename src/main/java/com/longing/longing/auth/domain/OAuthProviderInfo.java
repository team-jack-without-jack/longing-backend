package com.longing.longing.auth.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class OAuthProviderInfo {
//    private final String providerName;
//    private final String clientId;
//    private final String clientSecret;
//    private final String redirectUri;
//    private final String tokenUri;
//    private final String userInfoUri;
//    private final String userNameAttribute;

    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String tokenUri;
    private String userInfoUri;
    private String userNameAttribute;
    private String providerName;  // 이 추가 필드가 필요할 수 있음
    public boolean isIosClient() {
        return this.redirectUri.startsWith("com."); // iOS 클라이언트 확인
    }

//    @Value("${oauth.google.client-id}")
//    private String googleClientId;
//
//    @Value("${oauth.google.client-secret}")
//    private String googleClientSecret;
//
//    @Value("${oauth.google.redirect-uri}")
//    private String googleRedirectUri;
//
//    @Value("${oauth.google.token-uri}")
//    private String googleTokenUri;
//
//    @Value("${oauth.google.user-info-uri}")
//    private String googleUserInfoUri;
//
//    @Value("${oauth.google.user-name-attribute}")
//    private String googleUserNameAttribute;

    // Kakao, GitHub도 동일한 방식으로 추가 가능

    // ✅ 각 공급자별 정보 설정
//    public static OAuthProviderInfo of(String provider) {
//        switch (provider.toLowerCase()) {
//            case "google":
//                return new OAuthProviderInfo(
//                        "google",
//                        "구글_CLIENT_ID",
//                        "구글_CLIENT_SECRET",
//                        "http://localhost:3000/oauth/callback/google", // 프론트엔드 Redirect URI
//                        "https://oauth2.googleapis.com/token",
//                        "https://www.googleapis.com/oauth2/v3/userinfo",
//                        "sub" // Google의 user ID 속성명
//                );
//            case "kakao":
//                return new OAuthProviderInfo(
//                        "kakao",
//                        "카카오_CLIENT_ID",
//                        "카카오_CLIENT_SECRET",
//                        "http://localhost:3000/oauth/callback/kakao",
//                        "https://kauth.kakao.com/oauth/token",
//                        "https://kapi.kakao.com/v2/user/me",
//                        "id"
//                );
//            case "github":
//                return new OAuthProviderInfo(
//                        "github",
//                        "깃허브_CLIENT_ID",
//                        "깃허브_CLIENT_SECRET",
//                        "http://localhost:3000/oauth/callback/github",
//                        "https://github.com/login/oauth/access_token",
//                        "https://api.github.com/user",
//                        "id"
//                );
//            default:
//                throw new IllegalArgumentException("지원하지 않는 OAuth 공급자: " + provider);
//        }
//    }
}
