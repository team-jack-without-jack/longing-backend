package com.longing.longing.api.user.controller;

import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.user.controller.response.UserResponse;
import com.longing.longing.api.user.domain.UserUpdate;
import com.longing.longing.common.response.ApiResponse;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.controller.port.UserService;
import com.longing.longing.api.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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

    /**
     * 유저를 차단합니다.
     * @param blockUserId
     * @param user
     * @return
     */
    @PostMapping("/block")
    public ApiResponse<Boolean> blockUser(
            @NotNull @RequestParam long blockUserId,
            @AuthenticationPrincipal User user
    ) {
        userService.blockUser(user, blockUserId);
        return ApiResponse.ok(true);
    }


    /**
     * 유저 차단을 해제합니다.
     * @param blockUserId
     * @param user
     * @return
     */
    @DeleteMapping("/block")
    public ApiResponse<Boolean> cancelBlockUser(
            @NotNull @RequestParam long blockUserId,
            @AuthenticationPrincipal User user
    ) {
        log.info("blockUserId>> " + blockUserId);
        userService.cancelBlockUser(user, blockUserId);
        return ApiResponse.ok(true);
    }

    /**
     * 자신이 차단한 유저목록을 조회합니다.
     * @param keyword
     * @param page
     * @param size
     * @param sortBy
     * @param sortDirection
     * @param user
     * @return
     */
    @GetMapping("/block")
    public ApiResponse<Page<UserResponse>> getBlockedUserList(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @AuthenticationPrincipal User user
    ) {
        Page<User> blockedUserList = userService.getBlockedUserList(user, keyword, page, size, sortBy, sortDirection);
        Page<UserResponse> responsePage = blockedUserList.map(UserResponse::fromDomain);
        return ApiResponse.ok(responsePage);
    }


    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getPost(
            @PathVariable("id") Long userId
    ) {
        User userProfile = userService.getUserProfile(userId);
        return ApiResponse.ok(UserResponse.fromDomain(userProfile));
    }
}
