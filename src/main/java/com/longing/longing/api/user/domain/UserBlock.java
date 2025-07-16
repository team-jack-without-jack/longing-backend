package com.longing.longing.api.user.domain;

import com.longing.longing.api.post.domain.Post;
import lombok.Builder;
import lombok.Getter;

@Getter
public class UserBlock {
    private final Long id;
    private final User user;
    private final User blockedUser;

    @Builder
    public UserBlock(Long id, User user, User blockedUser) {
        this.id = id ;
        this.user = user;
        this.blockedUser = blockedUser;
    }
}
