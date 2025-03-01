package com.longing.longing.user.service;

import com.longing.longing.common.domain.ResourceNotFoundException;
import com.longing.longing.common.service.S3ImageService;
import com.longing.longing.user.controller.port.UserService;
import com.longing.longing.user.domain.User;
import com.longing.longing.user.domain.UserUpdate;
import com.longing.longing.user.infrastructure.UserEntity;
import com.longing.longing.user.infrastructure.UserJpaRepository;
import com.longing.longing.user.service.port.UserRepository;
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
    private final UserJpaRepository userJpaRepository;
    private final S3ImageService s3ImageService;

    private String uploadImage(MultipartFile image, UserEntity userEntity) {
        // S3 업로드 경로 설정: profileImages/{userId}/{originalFilename}
        String directoryPath = "profileImages/" + userEntity.getId() + "/";
        return s3ImageService.upload(image, directoryPath);
    }

    @Override
    public User getUser(String oauthId) {
        return userRepository.findByProviderId(oauthId)
                .orElseThrow(() -> new ResourceNotFoundException("Users", oauthId));

    }

    @Override
    @Transactional
    public User updateUser(String oauthId, UserUpdate userUpdate, MultipartFile profileImage) {
        UserEntity userEntity = userJpaRepository.findByProviderId(oauthId)
                .orElseThrow(() -> new ResourceNotFoundException("Users", oauthId));

        log.info(userUpdate.getName());
        log.info(userUpdate.getNationality());
        log.info(userUpdate.getIntroduction());
        String imageUrl = uploadImage(profileImage, userEntity);
        log.info(imageUrl);
        return userEntity.update(
                userUpdate.getName(),
                userUpdate.getNationality(),
                userUpdate.getIntroduction(),
                imageUrl
        ).toModel();
    }
}
