package com.longing.longing.config;

import com.longing.longing.config.auth.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .oauth2Login((oauth2) -> oauth2
//                        .loginPage("/oauth-login")
                        .loginPage("/google-oauth-login")
                        .userInfoEndpoint(userInfoEndpointConfig ->
                                userInfoEndpointConfig.userService(customOAuth2UserService)));

        http.logout().logoutSuccessUrl("/");

        http
                .authorizeHttpRequests((auth) -> auth
                        .antMatchers("/", "oauth2/**","/login/**", "/oauth-login/**").permitAll()
                        .anyRequest().authenticated());

        // 개발시에만 disabled
//        http.csrf((auth) -> auth.disable());
        http.csrf().disable();

//        http
//                .sessionManagement((auth) -> auth
//                        .maximumSessions(1)
//                        .maxSessionsPreventsLogin(true)); // 이 값을 초과했을 경우 기존 로그인을 로그아웃 시킬지 결정
        // true: 초과시 새로운 로그인 차단
        // false: 초과시 기존 세션 하나 삭제 후 새로들어온 로그인 처리

        // 참고: https://substantial-park-a17.notion.site/10-36136f5a91f647b499dbcb5a884aff72
//        http
//                .sessionManagement((auth) -> auth
//                        .sessionFixation().changeSessionId());


        return http.build();
    }
}
