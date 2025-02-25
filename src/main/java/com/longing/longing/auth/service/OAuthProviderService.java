package com.longing.longing.auth.service;

import com.longing.longing.auth.domain.OAuthProperties;
import com.longing.longing.auth.domain.OAuthProviderInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class OAuthProviderService {
//    private final OAuthProperties oAuthProperties;

//    public Map<String, OAuthProviderInfo> getProviderInfo(String provider) {
//        switch (provider.toLowerCase()) {
//            case "google":
//                return oAuthProperties.getGoogle();
//            case "kakao":
//                return oAuthProperties.getKakao();
//            case "facebook":
//                return oAuthProperties.getFacebook();
//            default:
//                throw new IllegalArgumentException("지원하지 않는 OAuth 공급자: " + provider);
//        }
//    }
//    private OAuthProviderInfo getProviderInfo(String provider) {
//        switch (provider.toLowerCase()) {
//            case "google":
//                return oAuthProperties.getGoogle().get("google");  // "google" key로 직접 값 접근
//            case "kakao":
//                return oAuthProperties.getKakao().get("kakao");  // "kakao" key로 직접 값 접근
//            case "facebook":
//                return oAuthProperties.getFacebook().get("facebook");  // "facebook" key로 직접 값 접근
//            default:
//                throw new IllegalArgumentException("지원하지 않는 OAuth 공급자: " + provider);
//        }
//    }
}
