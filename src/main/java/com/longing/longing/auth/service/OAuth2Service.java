package com.longing.longing.auth.service;

import com.longing.longing.auth.domain.OAuthProperties;
import com.longing.longing.auth.domain.OAuthProviderInfo;
import com.longing.longing.config.auth.JwtTokenProvider;
import com.longing.longing.config.auth.dto.OAuthAttributes;
import com.longing.longing.user.domain.User;
import com.longing.longing.user.infrastructure.UserEntity;
import com.longing.longing.user.infrastructure.UserJpaRepository;
import com.longing.longing.user.service.port.UserRepository;
import com.nimbusds.jose.JOSEException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;


import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2Service {
    private final UserRepository userRepository;
    private final UserJpaRepository userJpaRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate;
    private final OAuthProperties oAuthProperties;;
    private final AppleClientSecretService appleClientSecretService;

    public String authenticate(String provider, String code) throws JOSEException {
        OAuthProviderInfo providerInfo = getProviderInfo(provider);
        log.info("providerInfo>> " + providerInfo);

        // ✅ Authorization Code를 이용해 Access Token 요청
        String accessToken = requestAccessToken(providerInfo, code);

        // ✅ Access Token을 이용해 유저 정보 요청
        OAuthAttributes attributes = fetchUserInfo(providerInfo, accessToken);

        // ✅ 유저 정보 저장 또는 업데이트
        UserEntity userEntity = UserEntity.fromModel(saveOrUpdate(attributes));

        // ✅ JWT 발급
        return jwtTokenProvider.generateToken(userEntity.getEmail(), provider);
    }

    private OAuthProviderInfo getProviderInfo(String provider) throws JOSEException {
        switch (provider.toLowerCase()) {
            case "google":
                return new OAuthProviderInfo(oAuthProperties.getGoogleClientId(),
                        oAuthProperties.getGoogleClientSecret(),
                        oAuthProperties.getGoogleRedirectUri(),
                        "https://oauth2.googleapis.com/token",  // token uri
                        "https://www.googleapis.com/oauth2/v3/userinfo", // user info uri
                        "sub",
                        "google"); // user name attribute
            case "kakao":
                return new OAuthProviderInfo(oAuthProperties.getKakaoClientId(),
                        oAuthProperties.getKakaoClientSecret(),
                        oAuthProperties.getKakaoRedirectUri(),
                        "https://kauth.kakao.com/oauth/token",
                        "https://kapi.kakao.com/v2/user/me",
                        "id",
                        "kakao");
            case "facebook":
                return new OAuthProviderInfo(oAuthProperties.getFacebookClientId(),
                        oAuthProperties.getFacebookClientSecret(),
                        oAuthProperties.getFacebookRedirectUri(),
                        "https://graph.facebook.com/v12.0/oauth/access_token",
                        "https://graph.facebook.com/v12.0/me",
                        "id",
                        "kakao");
            case "apple":
                String appleSecret = appleClientSecretService.generate();
                log.info("appleSecret>> " + appleSecret);
                return new OAuthProviderInfo(
                        oAuthProperties.getAppleClientId(),       // Services ID
                        oAuthProperties.getAppleClientSecret(),   // JWT로 생성한 client_secret
                        oAuthProperties.getAppleRedirectUri(),
                        "https://appleid.apple.com/auth/token",    // token endpoint
                        "https://appleid.apple.com/auth/userinfo", // userinfo endpoint
                        "sub",                                     // Apple user identifier
                        "apple");
            default:
                throw new IllegalArgumentException("지원하지 않는 OAuth 공급자: " + provider);
        }
    }

    private String requestAccessToken(OAuthProviderInfo provider, String code) {
        HttpHeaders headers = new HttpHeaders();
        // headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Content-Type", "application/x-www-form-urlencoded");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

        log.info("client_id>> " +provider.getClientId());
        log.info("client_secret>> " +provider.getClientSecret());
        log.info("code>> " + code);
        log.info("redirect_uri>> " + provider.getRedirectUri());

        params.add("client_id", provider.getClientId());
        params.add("client_secret", provider.getClientSecret());
        params.add("code", code);
        params.add("redirect_uri", provider.getRedirectUri());
        params.add("grant_type", "authorization_code");

        if (!"google".equalsIgnoreCase(provider.getProviderName()) || !provider.isIosClient()) {
            params.add("client_secret", provider.getClientSecret());
        }

        // ✅ Google OAuth 2.0 Scope 추가
        if ("google".equalsIgnoreCase(provider.getProviderName())) {
            params.add("scope", "https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile");
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(provider.getTokenUri(), request, Map.class);
        log.info("response>> " + response);

        return (String) response.getBody().get("access_token");
    }

    private OAuthAttributes fetchUserInfo(OAuthProviderInfo provider, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(provider.getUserInfoUri(), HttpMethod.GET, entity, Map.class);
        return OAuthAttributes.of(provider.getProviderName(), provider.getUserNameAttribute(), response.getBody());
    }

    private User saveOrUpdate(OAuthAttributes attributes) {
        UserEntity userEntity = userJpaRepository.findByEmailAndProvider(attributes.getEmail(), attributes.getProvider())
                .orElse(attributes.toEntity()); // 기존 유저가 없을 때만 새 엔티티 생성

        // 기존 유저가 있으면 save() 하지 않고 그대로 반환
        if (userEntity.getId() != null) {
            return userEntity.toModel();
        }

        return userJpaRepository.save(userEntity).toModel(); // 새로운 유저만 저장 후 반환
    }
}
