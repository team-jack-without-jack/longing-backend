package com.longing.longing.config;

import com.longing.longing.config.auth.CustomOAuth2UserService;
import com.longing.longing.config.auth.JwtAuthenticationFilter;
import com.longing.longing.config.auth.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

        private final CustomOAuth2UserService customOAuth2UserService;
    private final JwtTokenProvider jwtTokenProvider;


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // JWT 인증 필터 추가
        http.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);
//        http.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class)
//                .addFilterBefore(new OAuth2AuthorizationRequestRedirectFilter(), JwtAuthenticationFilter.class);


        // OAuth2 로그인 설정
        http.oauth2Login(oauth2 -> oauth2
                .loginPage("/oauth-login") // 로그인 페이지 경로 설정
                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService)) // ✅ CustomOAuth2UserService 적용
        );

        // 로그아웃 설정
        http.logout()
                .logoutSuccessUrl("/")
                .permitAll();

        // 권한 설정
        http.authorizeHttpRequests((auth) -> auth
                        .antMatchers("/", "/test", "/ping", "oauth2/**", "/login/**", "/oauth-login/**", "/oauth/authenticate").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);


        // CSRF 비활성화 (개발 환경에서만 사용)
        http.cors().configurationSource(corsConfigurationSource())
                .and().csrf().disable();

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("*"); // ✅ iOS에서 오는 요청 허용 (배포 시 변경 필요)
        configuration.addAllowedMethod("*"); // ✅ 모든 HTTP 메서드 허용
        configuration.addAllowedHeader("*"); // ✅ 모든 헤더 허용
        configuration.setAllowCredentials(true); // ✅ 인증 정보 포함 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
