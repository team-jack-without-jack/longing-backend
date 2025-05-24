package com.longing.longing.auth.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.longing.longing.auth.service.OAuth2Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/oauth")
public class OAuthController {
    private final OAuth2Service oAuth2Service;

    @PostMapping("/authenticate")
    public ResponseEntity<Map<String, String>> authenticate(@RequestBody Map<String, String> requestBody) throws JsonProcessingException {
        String provider = requestBody.get("provider"); // "google", "kakao" 등
        String code = requestBody.get("code");
        String userJson = requestBody.get("user");     // Apple 최초 동의 시 전달되는 이름 정보(JSON)
        log.info("provider>> " + provider);
        log.info("code>> " + code);
        log.info("userJson>> " + userJson);

        String token = oAuth2Service.authenticate(provider, code, userJson);
        log.info("token>> " + token);

        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        return ResponseEntity.ok(response);
    }
}
