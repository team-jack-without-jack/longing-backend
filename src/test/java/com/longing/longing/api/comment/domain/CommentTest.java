package com.longing.longing.api.comment.domain;

import com.longing.longing.api.comment.domain.Comment;
import com.longing.longing.api.comment.domain.CommentCreate;
import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.user.domain.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class CommentTest {

    @Test
    public void CommentCreate으로_댓글을_만들_수_있다() {
        // given
        CommentCreate commentCreate = CommentCreate.builder()
                .postId(1L)
                .content("test content")
                .build();

        User user = User.builder()
                .build();

        Post post = Post.builder()
                .id(1L)
                .title("test title")
                .content("test content")
                .build();

        // when
        Comment comment = Comment.from(user, post, commentCreate);

        // then
        assertThat(comment.getContent()).isEqualTo("test content");
        assertThat(comment.getPost().getTitle()).isEqualTo("test title");
        assertThat(comment.getPost().getContent()).isEqualTo("test content");
    }

    @Test
    public void CommentUpdate로_댓글을_수정할_수_있다() {

    }
}
