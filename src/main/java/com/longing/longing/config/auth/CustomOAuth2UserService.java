package com.longing.longing.config.auth;

import com.longing.longing.config.auth.dto.OAuthAttributes;
import com.longing.longing.config.auth.dto.SessionUser;
import com.longing.longing.user.domain.User;
import com.longing.longing.user.infrastructure.UserEntity;
import com.longing.longing.user.infrastructure.UserJpaRepository;
import com.longing.longing.user.service.port.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Collections;

@Slf4j
@RequiredArgsConstructor
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final UserRepository userRepository;
    private final UserJpaRepository userJpaRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final HttpSession httpSession;
    private final HttpServletResponse response; // JWT를 쿠키로 설정하기 위해 추가


    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();

        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

        UserEntity userEntity = UserEntity.fromModel(saveOrUpdate(attributes));

//        SessionUser ses = new SessionUser(userEntity);
//        httpSession.setAttribute("user", ses);

        // ✅ JWT 발급
        String token = jwtTokenProvider.generateToken(userEntity.getEmail());

        // ✅ JWT를 쿠키에 저장
        response.addHeader("Set-Cookie", "longing-token=" + token + "; Path=/; HttpOnly; Secure; SameSite=Strict");


        return new DefaultOAuth2User(
                Collections.singleton(
                        new SimpleGrantedAuthority(
                                userEntity.getRoleKey()
                        )
                ),
                attributes.getAttributes(),
                attributes.getNameAttributeKey()
        );
    }

//    private User saveOrUpdate(OAuthAttributes attributes) {
//        UserEntity userEntity = userRepository.findByEmailAndProvider(attributes.getEmail(), attributes.getProvider())
//                .map(entity -> entity.update(attributes.getName(), attributes.getPicture()))
//                .orElse(attributes.toEntity());
//
//        return userRepository.save(userEntity.toModel());
//    }

    private User saveOrUpdate(OAuthAttributes attributes) {
        UserEntity userEntity = userRepository.findByEmailAndProvider(attributes.getEmail(), attributes.getProvider())
                .orElse(attributes.toEntity()); // 기존 유저가 없을 때만 새 엔티티 생성

        // 기존 유저가 있으면 save() 하지 않고 그대로 반환
        if (userEntity.getId() != null) {
            return userEntity.toModel();
        }

        return userJpaRepository.save(userEntity).toModel(); // 새로운 유저만 저장 후 반환
    }

}
