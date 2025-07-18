package com.longing.longing.api.user.controller.port;

import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.domain.UserUpdate;
import com.longing.longing.api.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
//    User getUser(String oauthId);
    User getUser(String email, Provider provider);
    User updateUser(User user, UserUpdate userUpdate, MultipartFile profileImage);

    void deavtivateUser(String email, Provider provider);

    void blockUser(User user, long blockUserId);

    void cancelBlockUser(User user, long blockUserId);

    Page<User> getBlockedUserList(User user, String keyword, int page, int size, String sortBy, String sortDirection);

    User getUserProfile(long userId);
}
