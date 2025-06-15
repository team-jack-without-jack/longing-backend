package com.longing.longing.api.like.controller;

import com.longing.longing.common.domain.ResourceNotFoundException;
import com.longing.longing.common.response.ApiResponse;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.longing.longing.api.like.controller.port.LikeService;
import com.longing.longing.api.like.domain.LikePostCreate;
import com.longing.longing.api.like.domain.LikePostDelete;
import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.api.user.service.port.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;
    private final UserRepository userRepository;

    @PostMapping("/post/{id}/like")
    public ApiResponse<Boolean> likePost(
            @PathVariable("id") long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String email = userDetails.getEmail();
        Provider provider = userDetails.getProvider();
        User user = userRepository.findByEmailAndProvider(email, provider)
                .orElseThrow(() -> new ResourceNotFoundException("Users", email));
        Long userId = user.getId();
        LikePostCreate likePostCreate = new LikePostCreate(postId, userId);
        likeService.likePost(likePostCreate);
        return ApiResponse.ok(true);
    }

    @DeleteMapping("/post/{id}/unlike")
    public ApiResponse<?> unlikePost(
            @PathVariable("id") Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String email = userDetails.getEmail();
        Provider provider = userDetails.getProvider();
        User user = userRepository.findByEmailAndProvider(email, provider)
                .orElseThrow(() -> new ResourceNotFoundException("Users", email));
        Long userId = user.getId();
        LikePostDelete likePostDelete = new LikePostDelete(postId, userId);
        likeService.unlikePost(likePostDelete);
        return ApiResponse.ok(null);
    }


    // OAuth2AuthenticationToken에서 사용자 ID 추출
//    public Long getUserIdFromAuthentication(Authentication authentication) {
//        if (authentication.getPrincipal() instanceof OAuth2User) {
//            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
//            // 여기서 "sub" 또는 사용자 고유 ID가 담긴 속성을 추출해야 합니다.
//            // "sub"가 일반적인 고유 사용자 ID로 사용되지만, 사용하는 OAuth 제공자에 따라 다를 수 있습니다.
//            String userId = oauth2User.getAttribute("sub"); // 예시: "sub" 필드 사용
//            return Long.valueOf(userId);
//        } else {
//            throw new RuntimeException("Unable to get user ID from authentication.");
//        }
//    }

}
