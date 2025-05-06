package com.longing.longing.utils.aws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

@Component
@Slf4j
public class AwsSecretService {

    private final SecretsManagerClient smClient;
    private final String secretName;

    public AwsSecretService(
            @Value("${aws.region}") String region,
            @Value("${aws.secret-name}") String secretName
    ) {
        this.smClient = SecretsManagerClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        this.secretName = secretName;
    }

    /**
     * ARN 또는 이름으로 저장된 시크릿을 가져옵니다.
     */
    public String getSecret() {
        GetSecretValueResponse resp = smClient.getSecretValue(
                GetSecretValueRequest.builder()
                        .secretId(secretName)
                        .build()
        );
        if (resp.secretString() != null) {
            return resp.secretString();
        }
        // Binary 형식일 경우, Base64 디코딩 로직 추가
        throw new IllegalStateException("Secret is not a string");
    }
}
