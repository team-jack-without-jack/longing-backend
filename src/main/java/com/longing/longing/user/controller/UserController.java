package com.longing.longing.user.controller;

import com.longing.longing.common.response.ApiResponse;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.longing.longing.post.domain.Post;
import com.longing.longing.user.Provider;
import com.longing.longing.user.controller.port.UserService;
import com.longing.longing.user.domain.User;
import com.longing.longing.user.domain.UserUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PatchMapping()
    public ApiResponse<User> updateUser(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        UserUpdate userUpdate,
        @RequestParam(value = "profileImage", required = false) MultipartFile profileImage
    ) {
        User user = userService.updateUser(userDetails, userUpdate, profileImage);
        return ApiResponse.ok(user);
    }

    @GetMapping("/myProfile")
    public ApiResponse<User> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String email = userDetails.getEmail();
        Provider provider = userDetails.getProvider();
        User user = userService.getUser(email, provider);
        return ApiResponse.ok(user);
    }


    @PostMapping("/deactivate")
    public ApiResponse<Boolean> deavtivateMe(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String email = userDetails.getEmail();
        Provider provider = userDetails.getProvider();
        userService.deavtivateUser(email, provider);
        return ApiResponse.ok(true);
    }
}
