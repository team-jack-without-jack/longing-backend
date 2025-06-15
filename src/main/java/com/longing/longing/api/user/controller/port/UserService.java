package com.longing.longing.api.user.controller.port;

import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.api.user.domain.UserUpdate;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
//    User getUser(String oauthId);
    User getUser(String email, Provider provider);
    User updateUser(CustomUserDetails userDetails, UserUpdate userUpdate, MultipartFile profileImage);

    void deavtivateUser(String email, Provider provider);
}
