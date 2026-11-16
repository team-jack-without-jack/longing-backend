package com.longing.longing.api.user.service;

import com.longing.longing.api.post.service.port.PostRepository;
import com.longing.longing.api.user.domain.UserBlock;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityNotFoundException;

@Slf4j
@Builder
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final S3ImageServiceImpl s3ImageService;
    private final PostRepository postRepository;

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

    /**
     * 회원 탈퇴 (#128). 사용자의 게시글을 id 순으로 한 건씩 잠가 soft delete한 뒤 사용자를 soft delete한다.
     * 게시글의 신고는 남고 좋아요·북마크는 지워진다. 이 사용자가 다른 게시글에 남긴 데이터는 그대로 둔다.
     * 게시글 삭제와 사용자 삭제를 한 트랜잭션으로 묶는다. @Transactional을 빼면 게시글만 지워지고 사용자는 남을 수 있다.
     */
    @Override
    @Transactional
    public void deavtivateUser(String email, Provider provider) {
        User user = userRepository.findByEmailAndProvider(email, provider)
                .orElseThrow(() -> new ResourceNotFoundException("Users", email));

        postRepository.softDeleteAllByUserId(user.getId());
        userRepository.deleteById(user.getId());
    }

    @Override
    public void blockUser(User user, long blockUserId) {
        User blockedUser = userRepository.findById(blockUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Users Id", blockUserId));

        /**
         * TODO
         */
//        checkIsAlreadyBlockedUser(user, blockUserId);

        userRepository.blockUser(user, blockedUser);
    }

    @Override
    public void cancelBlockUser(User user, long blockUserId) {
        User blockedUser = userRepository.findById(blockUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Users Id", blockUserId));

        log.info("blockUser.getId() " + blockedUser.getId());

        UserBlock userBlock = userRepository.findBlockedData(user, blockedUser)
                .orElseThrow(() -> new EntityNotFoundException("this user is not blocked by" + blockedUser.getEmail()));

        log.info("userBlock.getId() " + userBlock.getId());

        userRepository.deleteByUserBlockedId(userBlock.getId());
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
