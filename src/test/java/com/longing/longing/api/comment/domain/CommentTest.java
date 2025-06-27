package com.longing.longing.api.comment.domain;

import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.Role;
import com.longing.longing.api.user.domain.User;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

public class CommentTest {

    @Test
    public void CommentCreate으로_게시글을_만들_수_있다() {
        // given
        User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .name("testUser")
                .build();
        Post post = Post.builder()
                .id(1L)
                .title("test title")
                .content("test post content")
                .likeCount(0)
                .commentList(new ArrayList<>())
                .likeCount(0)
                .commentCount(0)
                .user(user)
                .build();

        CommentCreate commentCreate = CommentCreate.builder()
                .postId(1L)
                .content("test comment")
                .build();

        // when
        Comment comment = Comment.from(user, post, commentCreate);

        // then
        assertThat(comment.getContent()).isEqualTo("test comment");
        assertThat(comment.getUser().getName()).isEqualTo("testUser");
    }

    public void CommentUpdate로_comment를_수정할_수_있다() {
        // given
        CommentUpdate commentUpdate = CommentUpdate.builder()
                .content("update comment")
                .build();
        User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .name("testUser")
                .build();
        Post post = Post.builder()
                .id(1L)
                .title("test title")
                .content("test post content")
                .likeCount(0)
                .commentList(new ArrayList<>())
                .likeCount(0)
                .commentCount(0)
                .user(user)
                .build();
        Comment comment = Comment.builder()
                .id(1L)
                .content("test comment")
                .post(post)
                .user(user)
                .build();

        // when
        comment.update(commentUpdate);

        // then
        assertThat(comment.getContent()).isEqualTo("update comment");
    }
}
