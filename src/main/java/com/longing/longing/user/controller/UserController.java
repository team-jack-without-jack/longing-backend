package com.longing.longing.user.controller;

import com.longing.longing.common.response.ApiResponse;
import com.longing.longing.post.domain.Post;
import com.longing.longing.user.controller.port.UserService;
import com.longing.longing.user.domain.User;
import com.longing.longing.user.domain.UserUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PatchMapping()
    public ApiResponse<User> updateUser(
        Authentication authentication,
        UserUpdate userUpdate,
        MultipartFile profileImage
    ) {
        String oauthId = authentication.getName();
        log.info("oauthId>> " + oauthId);
        User user = userService.updateUser(oauthId, userUpdate, profileImage);
        return ApiResponse.ok(user);
    }

    @GetMapping("/myProfile")
    public ApiResponse<User> getMyProfile(
            Authentication authentication
    ) {
        String oauthId = authentication.getName();
        User user = userService.getUser(oauthId);
        return ApiResponse.ok(user);
    }
}
