package com.longing.longing.config;

import com.longing.longing.config.auth.AuthenticationPrincipalArgumentResolver;
import com.longing.longing.config.auth.CustomOAuth2UserService;
import com.longing.longing.config.auth.JwtAuthenticationFilter;
import com.longing.longing.config.auth.JwtTokenProvider;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.api.user.service.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig implements WebMvcConfigurer {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new AuthenticationPrincipalArgumentResolver(userRepository));
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // JWT 인증 필터 추가
        http.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        // OAuth2 로그인 설정
        http.oauth2Login(oauth2 -> oauth2
                .loginPage("/oauth-login")
                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
        );

        // 로그아웃 설정
        http.logout()
                .logoutSuccessUrl("/")
                .permitAll();

        // 권한 설정
        http.authorizeHttpRequests((auth) -> auth
                        .requestMatchers(EndpointRequest.to("health", "info")).permitAll()
                        .requestMatchers(EndpointRequest.toAnyEndpoint()).authenticated()
                        .antMatchers("/", "/test", "/ping", "/ping2", "oauth2/**", "/login/**", "/oauth-login/**", "/oauth/authenticate").permitAll()
                        .anyRequest().authenticated());

        // CSRF 비활성화 및 CORS 설정
        http.cors().configurationSource(corsConfigurationSource())
                .and().csrf().disable();

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        Arrays.stream(allowedOrigins.split(","))
                .forEach(configuration::addAllowedOrigin);
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
