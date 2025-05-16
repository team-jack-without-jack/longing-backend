package com.longing.longing.auth.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import javax.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.KeyFactory;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Component
public class AppleClientSecretGenerator {
    @Value("${spring.security.oauth2.client.registration.apple.team-id}")
    private String teamId;
    @Value("${spring.security.oauth2.client.registration.apple.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.registration.apple.key-id}")
    private String keyId;
    @Value("${spring.security.oauth2.client.registration.apple.private-key-secret-id}")
    private String privateKeySecretId;

    private final SecretsManagerClient secretsClient;
    private ECPrivateKey privateKey;

    public AppleClientSecretGenerator(SecretsManagerClient secretsClient) {
        this.secretsClient = secretsClient;
    }

    @PostConstruct
    public void init() throws Exception {
        // AWS Secrets Manager에서 .p8 내용 가져오기
        GetSecretValueRequest req = GetSecretValueRequest.builder()
                .secretId(privateKeySecretId)
                .build();
        GetSecretValueResponse res = secretsClient.getSecretValue(req);
        String pem = res.secretString();

        // PEM 파싱
        String content = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(content);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("EC");
        privateKey = (ECPrivateKey) kf.generatePrivate(spec);
    }

    public String generate() {
        Instant now = Instant.now();
        Algorithm alg = Algorithm.ECDSA256((ECPublicKey) null, privateKey);
        return JWT.create()
                .withKeyId(keyId)
                .withIssuer(teamId)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusSeconds(300)))
                .withAudience("https://appleid.apple.com")
                .withSubject(clientId)
                .sign(alg);
    }

//    @PostConstruct
//    public void init() throws Exception {
//        String content = Files.readString(
//                new ClassPathResource(privateKeyPath).getFile().toPath(), StandardCharsets.UTF_8
//        );
//        content = content
//                .replace("-----BEGIN PRIVATE KEY-----", "")
//                .replace("-----END PRIVATE KEY-----", "")
//                .replaceAll("\\s+", "");
//        byte[] decoded = Base64.getDecoder().decode(content);
//        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
//        KeyFactory kf = KeyFactory.getInstance("EC");
//        privateKey = (ECPrivateKey) kf.generatePrivate(spec);
//    }
//
//    public String generate() {
//        Instant now = Instant.now();
//        Algorithm alg = Algorithm.ECDSA256((ECPublicKey) null, privateKey);
//        return JWT.create()
//                .withKeyId(keyId)
//                .withIssuer(teamId)                          //iss
//                .withIssuedAt(Date.from(now))                //iat
//                .withExpiresAt(Date.from(now.plusSeconds(300))) //exp (5분 이내 추천)
//                .withAudience("https://appleid.apple.com")   //aud
//                .withSubject(clientId)                       //sub
//                .sign(alg);
//    }
}
