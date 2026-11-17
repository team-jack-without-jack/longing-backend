package com.longing.longing.support;

import com.longing.longing.api.user.domain.User;
import com.longing.longing.api.user.service.port.UserRepository;
import com.longing.longing.common.GlobalExceptionHandler;
import com.longing.longing.common.writeguard.WriteGuardExceptionHandler;
import com.longing.longing.config.auth.AuthenticationPrincipalArgumentResolver;
import com.longing.longing.config.auth.JwtAuthenticationFilter;
import com.longing.longing.config.auth.JwtTokenProvider;
import com.longing.longing.utils.slack.SlackUtils;
import io.jsonwebtoken.Claims;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class AuthenticatedMvc {

    public static final String EMAIL = "member@example.com";
    public static final User MEMBER = User.builder().id(42L).email(EMAIL).build();
    private static final String TOKEN = "valid-token";

    private AuthenticatedMvc() {
    }

    public static MockMvc build(Object controller) {
        UserRepository users = mock(UserRepository.class);
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(MEMBER));
        JwtTokenProvider tokens = mock(JwtTokenProvider.class);
        Claims claims = mock(Claims.class);
        when(tokens.validateToken(TOKEN)).thenReturn(true);
        when(tokens.getClaims(TOKEN)).thenReturn(claims);
        when(claims.getSubject()).thenReturn(EMAIL);
        when(claims.get("provider", String.class)).thenReturn("GOOGLE");
        return MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver(users))
                .setControllerAdvice(new GlobalExceptionHandler(mock(SlackUtils.class)), new WriteGuardExceptionHandler())
                .addFilters(new JwtAuthenticationFilter(tokens))
                .build();
    }

    public static <B extends MockHttpServletRequestBuilder> B authenticated(B request) {
        SecurityContextHolder.clearContext();
        request.header("Authorization", "Bearer " + TOKEN);
        return request;
    }
}
