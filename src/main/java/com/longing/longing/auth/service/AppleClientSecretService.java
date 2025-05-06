package com.longing.longing.auth.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.longing.longing.auth.domain.OAuthProperties;
import com.longing.longing.utils.aws.AwsSecretService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

/**
 * Apple OAuth2 client-secret(JWT) 생성 서비스
 */
@Component
@Slf4j
public class AppleClientSecretService {

    private final AwsSecretService awsSecretService;
    private final OAuthProperties oAuthProperties;
    private ECPrivateKey privateKey;

    public AppleClientSecretService(AwsSecretService awsSecretService,
                                    OAuthProperties oAuthProperties) {
        this.awsSecretService = awsSecretService;
        this.oAuthProperties = oAuthProperties;
        loadPrivateKey();
    }

    /**
     * Secrets Manager에서 .p8 키 파일을 읽어 ECPrivateKey로 파싱
     */
    private void loadPrivateKey() {
        try {
            String secretPem = awsSecretService.getSecret();
            String pem = secretPem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] der = Base64.getDecoder().decode(pem);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
            KeyFactory kf = KeyFactory.getInstance("EC");
            this.privateKey = (ECPrivateKey) kf.generatePrivate(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load Apple private key from Secrets Manager", e);
        }
    }

    /**
     * 유효기간 5분의 JWT 형태 client-secret 생성
     * @return signed JWT string
     */
    public String generate() throws JOSEException {
        Instant now = Instant.now();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(oAuthProperties.getAppleTeamId())
                .subject(oAuthProperties.getAppleClientId())
                .audience("https://appleid.apple.com")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(5, ChronoUnit.MINUTES)))
                .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                .keyID(oAuthProperties.getAppleKeyId())
                .build();

        SignedJWT jwt = new SignedJWT(header, claims);
        jwt.sign(new ECDSASigner(privateKey));
        return jwt.serialize();
    }
}
