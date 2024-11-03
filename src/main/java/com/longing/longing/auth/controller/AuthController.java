package com.longing.longing.auth.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    @GetMapping("/google-oauth-login")
    public String oauthLogin() {
//        return "/oauthLoginPage";
        return "/oauth2/authorization/google";
    }
}
