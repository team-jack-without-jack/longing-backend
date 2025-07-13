package com.longing.longing.api.user.service;

import com.longing.longing.api.user.domain.UserUpdate;
import com.longing.longing.api.user.infrastructure.UserEntity;
import com.longing.longing.common.exceptions.ResourceNotFoundException;
import com.longing.longing.common.service.S3ImageServiceImpl;
import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.controller.port.UserService;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.api.user.service.port.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
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

    @Override
    public void blockUser(User user, long blockUserId) {
        User blockedUser = userRepository.findById(blockUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Users Id", blockUserId));
        userRepository.blockUser(user, blockedUser);
    }
}
