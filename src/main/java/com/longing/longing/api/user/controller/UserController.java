package com.longing.longing.api.user.controller;

import com.longing.longing.api.user.domain.UserUpdate;
import com.longing.longing.common.response.ApiResponse;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.controller.port.UserService;
import com.longing.longing.api.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PatchMapping()
    public ApiResponse<User> updateUser(
        @AuthenticationPrincipal User user,
        UserUpdate userUpdate,
        @RequestParam(value = "profileImage", required = false) MultipartFile profileImage
    ) {
        User updateUser = userService.updateUser(user, userUpdate, profileImage);
        return ApiResponse.ok(updateUser);
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


    @DeleteMapping("/deactivate")
    public ApiResponse<Boolean> deavtivateMe(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String email = userDetails.getEmail();
        Provider provider = userDetails.getProvider();
        userService.deavtivateUser(email, provider);
        return ApiResponse.ok(true);
    }

    @PostMapping("/block")
    public ApiResponse<Boolean> blockUser(
            @NotNull @RequestParam() long blockUserId,
            @AuthenticationPrincipal User user
    ) {
        userService.blockUser(user, blockUserId);
        return ApiResponse.ok(true);
    }
}
