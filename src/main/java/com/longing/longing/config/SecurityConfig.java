package com.longing.longing.config;

import com.longing.longing.common.filter.JwtAuthenticationFilter;
import com.longing.longing.config.auth.CustomOAuth2UserService;
import com.longing.longing.config.auth.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

//    private final CustomOAuth2UserService customOAuth2UserService;
    private final JwtTokenProvider jwtTokenProvider;


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        http;
//                .oauth2Login((oauth2) -> oauth2
////                        .loginPage("/oauth-login")
//                        .loginPage("/oauth-login")
//                        .userInfoEndpoint(userInfoEndpointConfig ->
//                                userInfoEndpointConfig.userService(customOAuth2UserService)));
//
//        http.logout().logoutSuccessUrl("/");
//
//        http
//                .authorizeHttpRequests((auth) -> auth
//                        .antMatchers("/", "/ping", "oauth2/**","/login/**", "/oauth-login/**").permitAll()
//                        .anyRequest().authenticated());
//
//        // 개발시에만 disabled
////        http.csrf((auth) -> auth.disable());
//        http.csrf().disable();
//
//
//        return http.build()

        // JWT 인증 필터 추가
        http.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        // 로그아웃 설정
        http.logout()
                .logoutSuccessUrl("/")
                .permitAll();

        // 권한 설정
        http.authorizeHttpRequests((auth) -> auth
                .antMatchers("/", "/ping", "oauth2/**", "/login/**", "/oauth-login/**").permitAll()
                .anyRequest().authenticated());

        // 구글 로그인 설정 (oauth2Login 추가)
        http.oauth2Login()
                .loginPage("/oauth-login") // 구글 로그인 후 리다이렉트할 페이지 설정
                .permitAll();

        // CSRF 비활성화 (개발 환경에서만 사용)
        http.csrf().disable();

        return http.build();


//        http
//                .csrf(csrf -> csrf.disable())  // JWT는 CSRF 필요 없음
//                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 사용 X
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/api/public/**").permitAll()
//                        .anyRequest().authenticated()
//                )
//                .oauth2Login()
//                .and()
//                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class); // JWT 필터 추가
//
//        return http.build();
    }
}
