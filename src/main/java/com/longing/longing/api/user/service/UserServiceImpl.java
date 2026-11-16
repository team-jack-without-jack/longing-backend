package com.longing.longing.api.user.service;

import com.longing.longing.api.user.domain.UserUpdate;
import com.longing.longing.api.user.infrastructure.UserEntity;
import com.longing.longing.common.exceptions.ResourceNotFoundException;
import com.longing.longing.common.service.S3ImageServiceImpl;
import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.controller.port.UserService;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.api.user.service.port.UserRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Builder
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final S3ImageServiceImpl s3ImageService;

    private String uploadProfileImage(MultipartFile image, User user) {
        // S3 업로드 경로 설정: profile_images/user_{userId}/{originalFilename}
        String directoryPath = "profile_images/user_" + user.getId() + "/";
        return s3ImageService.uploadProfileImage(image, directoryPath);
    }

    @Override
    public User getUser(String email, Provider provider) {
        return userRepository.findByEmailAndProvider(email, provider)
                .orElseThrow(() -> new ResourceNotFoundException("Users", email));
    }

    @Override
    @Transactional
    public User updateUser(User user, UserUpdate userUpdate, MultipartFile profileImage) {
        String imageUrl = null;
        if (profileImage != null && !profileImage.isEmpty()) {
            imageUrl = uploadProfileImage(profileImage, user);
        }
        user.update(userUpdate, imageUrl);
        userRepository.save(user);
        return user;
    }

    @Override
    public void deavtivateUser(String email, Provider provider) {
        User user = userRepository.findByEmailAndProvider(email, provider)
                .orElseThrow(() -> new ResourceNotFoundException("Users", email));

        userRepository.deleteById(user.getId());
    }

    /**
     * 멱등: 이미 차단했으면 그대로 성공한다 (#124).
     * 동시 요청이 모두 "없음"을 봐도 blockIfAbsent가 unique 제약으로 한 행만 남긴다.
     * blockIfAbsent는 트랜잭션 밖에서 호출해야 하므로 @Transactional을 붙이지 않는다.
     */
    @Override
    public void blockUser(User user, long blockUserId) {
        User blockedUser = userRepository.findById(blockUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Users Id", blockUserId));

        if (userRepository.isBlocked(user.getId(), blockedUser.getId())) {
            return;
        }
        userRepository.blockIfAbsent(user, blockedUser);
    }

    /** 멱등: 차단 관계가 없어도 성공하고, 중복 행이 남아 있으면 함께 지운다 (#124). */
    @Override
    public void cancelBlockUser(User user, long blockUserId) {
        User blockedUser = userRepository.findById(blockUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Users Id", blockUserId));

        userRepository.unblock(user.getId(), blockedUser.getId());
    }

    @Override
    public Page<User> getBlockedUserList(User user, String keyword, int page, int size, String sortBy, String sortDirection) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
//        if (keyword == null || keyword.trim().isEmpty()) {
//            return postRepository.findAll(user.getId(), pageable);
//        }
        return userRepository.findBlockedUserList(user.getId(), keyword, pageable);

    }

    @Override
    public User getUserProfile(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("user not found, id = ", userId));
    }

}
