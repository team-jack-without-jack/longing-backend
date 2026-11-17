package com.longing.longing.config;

import com.longing.longing.api.user.service.port.UserRepository;
import com.longing.longing.config.auth.CustomOAuth2UserService;
import com.longing.longing.config.auth.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityConfigCorsTest {

    @Test
    void exposesReplayAndRetryHeadersToBrowsers() {
        SecurityConfig config = new SecurityConfig(
                mock(CustomOAuth2UserService.class), mock(JwtTokenProvider.class), mock(UserRepository.class));
        ReflectionTestUtils.setField(config, "allowedOrigins", "https://example.com");

        CorsConfigurationSource source = config.corsConfigurationSource();
        CorsConfiguration cors = ((UrlBasedCorsConfigurationSource) source).getCorsConfigurations().get("/**");

        assertThat(cors.getExposedHeaders()).contains("Idempotent-Replayed", "Retry-After");
    }
}
