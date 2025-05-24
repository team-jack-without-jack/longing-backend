package com.longing.longing.config.auth.dto;

import com.longing.longing.user.Provider;
import com.longing.longing.user.Role;
import com.longing.longing.user.infrastructure.UserEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Random;

@Slf4j
@Getter
public class OAuthAttributes {

    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String name;
    private String email;
    private String picture;
    private Provider provider;

    private String providerId;

    @Builder
    public OAuthAttributes (Map<String, Object> attributes,
                            String nameAttributeKey,
                            String name,
                            String email,
                            String picture,
                            Provider provider,
                            String providerId) {
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.name = name;
        this.email = email;
        this.picture = picture;
        this.provider = provider;
        this.providerId = providerId;
    }

    public static OAuthAttributes of (String registrationId,
                                      String userNameAttributeName,
                                      Map<String, Object> attributes) {

        switch (registrationId) {
            case "google":
                return ofGoogle(userNameAttributeName, attributes);
            case "kakao":
                return ofKakao(userNameAttributeName, attributes);
            case "facebook":
                return ofFacebook(userNameAttributeName, attributes);
            case "apple":
                return ofApple(userNameAttributeName, attributes);
            default:
                throw new IllegalArgumentException("Unsupported registration ID: " + registrationId);
        }
    }



    private static OAuthAttributes ofApple(String userNameAttributeName,
                                           Map<String, Object> attributes) {
        log.info("<<apple>>");
        // Apple OAuth의 경우, 기본 UserInfo 엔드포인트가 없어
        // id_token의 클레임을 attributes로 받을 때, sub, email 등을 꺼냅니다.
        return OAuthAttributes.builder()
                .name(attributes.get("name") != null
                        ? String.valueOf(attributes.get("name"))
                        : String.valueOf(attributes.get("temp_name")))  // Apple은 이름을 처음 로그인 시에만 제공
                .email(String.valueOf(attributes.get("email")))
                .picture("")  // Apple은 프로필 이미지를 제공하지 않음
                .provider(Provider.APPLE)
                .providerId(String.valueOf(attributes.get("sub")))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    private static OAuthAttributes ofFacebook(String userNameAttributeName, Map<String, Object> attributes) {
        log.info("<<faceBook>>");
        return OAuthAttributes.builder()
//                .name((String) attributes.get("name"))
//                .email((String) attributes.get("email"))
//                .picture((String) attributes.get("picture"))
                .name(String.valueOf(attributes.get("name")))
                .email(String.valueOf(attributes.get("email")))
                .picture(String.valueOf(attributes.get("picture")))
                .provider(Provider.FACEBOOK)
                .providerId(String.valueOf(attributes.get("id")))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    private static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
        log.info("<<kakao>>");
        return OAuthAttributes.builder()
//                .name((String) properties.get("nickname"))
//                .email((String) kakaoAccount.get("email"))
//                .picture((String) properties.get("profile_image"))
                .name(String.valueOf(properties.get("nickname")))
                .email(String.valueOf(kakaoAccount.get("email")))
                .picture(String.valueOf(properties.get("profile_image")))
                .provider(Provider.KAKAO)
                .providerId(String.valueOf(attributes.get("id")))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    private static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        log.info("<<google>>");
        return OAuthAttributes.builder()
//                .name((String) attributes.get("name"))
//                .email((String) attributes.get("email"))
//                .picture((String) attributes.get("picture"))
                .name(String.valueOf(attributes.get("name")))
                .email(String.valueOf(attributes.get("email")))
                .picture(String.valueOf(attributes.get("picture")))
                .provider(Provider.GOOGLE)
                .providerId(String.valueOf(attributes.get("sub")))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    public UserEntity toEntity() {
        return UserEntity.builder()
                .name(name)
                .email(email)
                .picture(picture)
                .provider(provider)
                .providerId(providerId)
                .role(Role.GUEST)
                .build();
    }
}
