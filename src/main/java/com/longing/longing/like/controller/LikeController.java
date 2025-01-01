package com.longing.longing.like.controller;

import com.longing.longing.common.domain.ResourceNotFoundException;
import com.longing.longing.like.controller.port.LikeService;
import com.longing.longing.like.domain.LikeCommentCreate;
import com.longing.longing.like.domain.LikePostCreate;
import com.longing.longing.like.domain.LikePostDelete;
import com.longing.longing.user.domain.User;
import com.longing.longing.user.service.port.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;
    private final UserRepository userRepository;

    @PostMapping("/post/{id}/like")
    public void likePost(
            @PathVariable("id") long postId,
            Authentication authentication
    ) {
        String providerId = authentication.getName(); // 현재 인증된 사용자의 ID를 가져옴
        User user = userRepository.findByProviderId(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Users", providerId));
        Long userId = user.getId();
        LikePostCreate likePostCreate = new LikePostCreate(postId, userId);
        likeService.likePost(likePostCreate);
    }

    @DeleteMapping("/post/{id}/unlike")
    public void unlikePost(
            @PathVariable("id") long postId,
            Authentication authentication
    ) {
        String providerId = authentication.getName(); // 현재 인증된 사용자의 ID를 가져옴
        User user = userRepository.findByProviderId(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Users", providerId));
        Long userId = user.getId();
        LikePostDelete likePostDelete = new LikePostDelete(postId, userId);
        likeService.unlikePost(likePostDelete);
    }

    @PostMapping("/comment/{id}/like")
    public void likeComment(
            @RequestBody LikeCommentCreate likeCommentCreate
    ) {
//        likeService.likeComment(likeComment);
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
