package com.longing.longing.auth.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Getter
@Component
@ConfigurationProperties(prefix = "oauth")
public class OAuthProperties {
    private Map<String, OAuthProviderInfo> google;
    private Map<String, OAuthProviderInfo> facebook;
    private Map<String, OAuthProviderInfo> kakao;
}
