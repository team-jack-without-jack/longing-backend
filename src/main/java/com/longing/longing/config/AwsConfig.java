package com.longing.longing.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

@Configuration
public class AwsConfig {
    @Bean
    public SecretsManagerClient secretsManagerClient(
            @Value("${aws.region}") String region
    ) {
        return SecretsManagerClient.builder()
                .region(Region.of(region))
                .build();
    }
}
