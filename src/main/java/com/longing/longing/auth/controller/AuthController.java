package com.longing.longing.auth.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    @GetMapping("/oauth-login")
    public String oauthLogin() {
        return "login";
    }

    @GetMapping("/test")
    public String test(Authentication authentication) {
        log.info("session>> " + authentication.getName());
        return "test";
    }

//    @GetMapping("/api/google/oauth/redirect")
//    public String oauthRedirect(HttpServletRequest request,
//                             HttpServletResponse response,
//                             @PathVariable(value = "type") String type,
//                             @RequestParam(value = "code") String code) {
//        System.out.println("authCode:: " + code);
//        System.out.println("type:: " + type);
//
////        String access_Token = kakaoService.getAccessToken(code);
////        System.out.println("###access_token#### : " + access_Token);
////        UserInfoDto userInfo = kakaoService.getUserinfo(request, response, access_Token, type);
//
//        return "redirect:/";
//    }

//    @GetMapping("/login/oauth2/code/google")
//    public String oauthRedirect2(HttpServletRequest request,
//                                HttpServletResponse response,
//                                @PathVariable(value = "type") String type,
//                                @RequestParam(value = "code") String code) {
//
////        String access_Token = kakaoService.getAccessToken(code);
////        System.out.println("###access_token#### : " + access_Token);
////        UserInfoDto userInfo = kakaoService.getUserinfo(request, response, access_Token, type);
//
//        return "redirect:/";
//    }
}
