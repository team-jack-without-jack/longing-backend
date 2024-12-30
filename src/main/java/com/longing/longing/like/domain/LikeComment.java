package com.longing.longing.like.domain;

import com.longing.longing.comment.domain.Comment;
import com.longing.longing.user.domain.User;
import lombok.Builder;
import lombok.Getter;

@Getter
public class LikeComment {

    private final Long id;

    private final User user;

    private final Comment comment;

    @Builder
    public LikeComment(Long id, User user, Comment comment) {
        this.id = id;
        this.user = user;
        this.comment = comment;
    }

}
