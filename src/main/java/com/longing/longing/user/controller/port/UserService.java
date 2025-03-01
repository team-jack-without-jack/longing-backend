package com.longing.longing.user.controller.port;

import com.longing.longing.user.domain.User;
import com.longing.longing.user.domain.UserUpdate;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    User getUser(String oauthId);
    User updateUser(String oauthId, UserUpdate userUpdate, MultipartFile profileImage);
}
