package com.longing.longing.auth.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.longing.longing.auth.service.OAuth2Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/oauth")
public class OAuthController {
    private final OAuth2Service oAuth2Service;

    @PostMapping("/authenticate")
    public ResponseEntity<Map<String, String>> authenticate(@RequestBody Map<String, String> requestBody) {
        String provider = requestBody.get("provider"); // "google", "kakao" 등
        String code = requestBody.get("code");

        String token = oAuth2Service.authenticate(provider, code);

        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        return ResponseEntity.ok(response);
    }

//    @PostMapping(
//            value = "/authenticate",
//            consumes = {
//                    MediaType.APPLICATION_JSON_VALUE,
//                    MediaType.APPLICATION_FORM_URLENCODED_VALUE
//            }
//    )
//    public ResponseEntity<Map<String, String>> authenticate(
//            @RequestBody(required = false) Map<String, String> jsonBody,
//            @RequestParam(value = "provider", required = false) String providerParam,
//            @RequestParam(value = "code",     required = false) String codeParam,
//            @RequestParam(value = "user",     required = false) String userParam
//    ) throws JsonProcessingException {
//        // JSON 바디 우선, 없으면 form-param 사용
//        String provider = (jsonBody != null && jsonBody.get("provider") != null)
//                ? jsonBody.get("provider")
//                : providerParam;
//        String code = (jsonBody != null && jsonBody.get("code") != null)
//                ? jsonBody.get("code")
//                : codeParam;
//        String userJson = (jsonBody != null && jsonBody.get("user") != null)
//                ? jsonBody.get("user")
//                : userParam;
//
//        log.info("provider>> {}", provider);
//        log.info("code    >> {}", code);
//        log.info("userJson>> {}", userJson);
//
//        String token = oAuth2Service.authenticate(provider, code, userJson);
//        log.info("token   >> {}", token);
//
//        return ResponseEntity.ok(Collections.singletonMap("token", token));
//    }
}
