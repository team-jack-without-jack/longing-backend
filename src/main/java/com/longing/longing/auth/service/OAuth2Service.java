package com.longing.longing.auth.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.longing.longing.auth.domain.OAuthProperties;
import com.longing.longing.auth.domain.OAuthProviderInfo;
import com.longing.longing.config.auth.JwtTokenProvider;
import com.longing.longing.config.auth.dto.OAuthAttributes;
import com.longing.longing.user.domain.User;
import com.longing.longing.user.infrastructure.UserEntity;
import com.longing.longing.user.infrastructure.UserJpaRepository;
import com.longing.longing.user.service.port.UserRepository;
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


import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2Service {
    private final UserRepository userRepository;
    private final UserJpaRepository userJpaRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate;
    private final OAuthProperties oAuthProperties;
    private final AppleClientSecretGenerator appleSecretGen;

//    public String authenticate(String provider, String code) {
//        OAuthProviderInfo providerInfo = getProviderInfo(provider);
//        log.info("providerInfo>> " + providerInfo);
//
//        // ✅ Authorization Code를 이용해 Access Token 요청
//        String accessToken = requestAccessToken(providerInfo, code);
//
//        // ✅ Access Token을 이용해 유저 정보 요청
//        OAuthAttributes attributes = fetchUserInfo(providerInfo, accessToken);
//
//        // ✅ 유저 정보 저장 또는 업데이트
//        UserEntity userEntity = UserEntity.fromModel(saveOrUpdate(attributes));
//
//        // ✅ JWT 발급
//        return jwtTokenProvider.generateToken(userEntity.getEmail(), provider);
//    }


    public String authenticate(String provider, String code) {
        OAuthProviderInfo providerInfo = getProviderInfo(provider);

        OAuthAttributes attributes;
        if ("apple".equalsIgnoreCase(provider)) {
            // Apple: token response contains id_token
            String appleTempUserName = createAppleTempName(10);
            Map<String, Object> tokenResponse = requestTokenResponse(providerInfo, code);
            String idToken = (String) tokenResponse.get("id_token");
            attributes = fetchAppleUserInfo(providerInfo, idToken, appleTempUserName);
        } else {
            String accessToken = requestAccessToken(providerInfo, code);
            attributes = fetchUserInfo(providerInfo, accessToken);
        }

        UserEntity userEntity = UserEntity.fromModel(saveOrUpdate(attributes));
        return jwtTokenProvider.generateToken(userEntity.getEmail(), provider);
    }

    private Map<String, Object> requestTokenResponse(OAuthProviderInfo provider, String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/x-www-form-urlencoded");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", provider.getClientId());
        params.add("client_secret", appleSecretGen.generate());
        params.add("code", code);
        params.add("redirect_uri", provider.getRedirectUri());
        params.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(provider.getTokenUri(), request, Map.class);
        return response.getBody();
    }

    private OAuthProviderInfo getProviderInfo(String provider) {
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
                        "facebook");
            case "apple":
                return new OAuthProviderInfo(
                        oAuthProperties.getAppleClientId(),
                        null,  // clientSecret not used here
                        oAuthProperties.getAppleRedirectUri(),
                        "https://appleid.apple.com/auth/token",
                        null,  // no userinfo endpoint
                        "sub",
                        "apple"
                );
//                return new OAuthProviderInfo(oAuthProperties.getAppleClientId(),
//                        appleSecretGen.generate(),
//                        oAuthProperties.getAppleRedirectUri(),
//                        "https://appleid.apple.com/auth/token",
//                        "https://appleid.apple.com/auth/userinfo",
//                        "sub",
//                        "apple");
            default:
                throw new IllegalArgumentException("지원하지 않는 OAuth 공급자: " + provider);
        }
//        switch (provider.toLowerCase()) {
//            case "google":
//                return oAuthProperties.getGoogle();  // "google" key로 직접 값 접근
//            case "kakao":
//                return oAuthProperties.getKakao();  // "kakao" key로 직접 값 접근
//            case "facebook":
//                return oAuthProperties.getFacebook();  // "facebook" key로 직접 값 접근
//            default:
//                throw new IllegalArgumentException("지원하지 않는 OAuth 공급자: " + provider);
//        }
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

    private String createAppleTempName(int range) {
        StringBuilder sb = new StringBuilder();
        Random rd = new Random();
        for(int i=0;i<range;i++){
            if(rd.nextBoolean()){
                sb.append(rd.nextInt(10));
            }else {
                sb.append((char)(rd.nextInt(26)+65));
            }
        }

        return sb.toString();
    }

    private OAuthAttributes fetchAppleUserInfo(OAuthProviderInfo provider, String idToken, String appleTempUserName) {
        DecodedJWT jwt = JWT.decode(idToken);
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", jwt.getSubject());
        claims.put("email", jwt.getClaim("email").asString());
        claims.put("email_verified", jwt.getClaim("email_verified").asBoolean());
        claims.put("temp_name", createAppleTempName(10));

        // 최초 동의 시에만 전달되는 userJson에서 이름 정보 추출
//        if (userJson != null) {
//            ObjectMapper mapper = new ObjectMapper();
//            JsonNode userNode = mapper.readTree(userJson);
//            String firstName = userNode.path("name").path("firstName").asText(null);
//            String lastName  = userNode.path("name").path("lastName").asText(null);
//            // 여기에 로그 추가
//            log.info("🍎 Apple user parsed name — firstName: {}, lastName: {}", firstName, lastName);
//
//            claims.put("firstName", firstName);
//            claims.put("lastName", lastName);
//        }

        return OAuthAttributes.of(
                provider.getProviderName(),
                provider.getUserNameAttribute(),
                claims
        );
    }

    private OAuthAttributes fetchUserInfo(OAuthProviderInfo provider, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(provider.getUserInfoUri(), HttpMethod.GET, entity, Map.class);
        // apple로그인이 아닌 경우라 파라미터에 빈 스트링을 넣습니다.
        return OAuthAttributes.of(provider.getProviderName(), provider.getUserNameAttribute(), response.getBody());
    }

//    private User saveOrUpdate(OAuthAttributes attributes) {
//        UserEntity userEntity = userRepository.findByEmailAndProvider(attributes.getEmail(), attributes.getProvider())
//                .map(entity -> entity.update(attributes.getName(), attributes.getPicture())) // 기존 엔티티 수정
//                .orElse(attributes.toEntity()); // 새로운 엔티티 생성
//
//        return userJpaRepository.save(userEntity).toModel(); // 그대로 저장
//    }

    private User saveOrUpdate(OAuthAttributes attributes) {
        UserEntity userEntity = userJpaRepository.findByEmailAndProvider(attributes.getEmail(), attributes.getProvider())
                .orElse(attributes.toEntity()); // 기존 유저가 없을 때만 새 엔티티 생성

        // 기존 유저가 있으면 save() 하지 않고 그대로 반환
        if (userEntity.getId() != null) {
            return userEntity.toModel();
        }

        return userJpaRepository.save(userEntity).toModel(); // 새로운 유저만 저장 후 반환
    }

//    public String authenticate(String provider, String code) {
//        OAuthProviderInfo providerInfo = OAuthProviderInfo.of(provider);
//
//        // ✅ Authorization Code를 이용해 Access Token 요청
//        String accessToken = requestAccessToken(providerInfo, code);
//
//        // ✅ Access Token을 이용해 유저 정보 요청
//        OAuthAttributes attributes = fetchUserInfo(providerInfo, accessToken);
//
//        // ✅ 유저 정보 저장 또는 업데이트
//        UserEntity userEntity = UserEntity.fromModel(saveOrUpdate(attributes));
//
//        // ✅ JWT 발급
//        return jwtTokenProvider.generateToken(userEntity.getEmail());
//    }
//
//    private String requestAccessToken(OAuthProviderInfo provider, String code) {
//        HttpHeaders headers = new HttpHeaders();
//        headers.set("Content-Type", "application/x-www-form-urlencoded");
//
//        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
//        params.add("client_id", provider.getClientId());
//        params.add("client_secret", provider.getClientSecret());
//        params.add("code", code);
//        params.add("redirect_uri", provider.getRedirectUri());
//        params.add("grant_type", "authorization_code");
//
//
//        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
//        ResponseEntity<Map> response = restTemplate.postForEntity(provider.getTokenUri(), request, Map.class);
//
//        return (String) response.getBody().get("access_token");
//    }
//
//    private OAuthAttributes fetchUserInfo(OAuthProviderInfo provider, String accessToken) {
//        HttpHeaders headers = new HttpHeaders();
//        headers.setBearerAuth(accessToken);
//        HttpEntity<String> entity = new HttpEntity<>(headers);
//
//        ResponseEntity<Map> response = restTemplate.exchange(provider.getUserInfoUri(), HttpMethod.GET, entity, Map.class);
//        return OAuthAttributes.of(provider.getProviderName(), provider.getUserNameAttribute(), response.getBody());
//    }
//
//    private User saveOrUpdate(OAuthAttributes attributes) {
//        UserEntity userEntity = userRepository.findByEmailAndProvider(attributes.getEmail(), attributes.getProvider())
//                .map(entity -> entity.update(attributes.getName(), attributes.getPicture()))
//                .orElse(attributes.toEntity());
//
//        return userRepository.save(userEntity.toModel());
//    }
}
