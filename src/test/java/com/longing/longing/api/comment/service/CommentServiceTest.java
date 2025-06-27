package com.longing.longing.api.comment.service;

import com.longing.longing.api.comment.domain.Comment;
import com.longing.longing.api.comment.domain.CommentCreate;
import com.longing.longing.api.comment.domain.CommentUpdate;
import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.Role;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.mock.FakeCommentRepository;
import com.longing.longing.mock.FakePostImageRepository;
import com.longing.longing.mock.FakePostRepository;
import com.longing.longing.mock.FakeUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class CommentServiceTest {

    private CommentServiceImpl commentService;
    private User user;

    @BeforeEach
    void init() {
        FakeUserRepository fakeUserRepository = new FakeUserRepository();
        FakeCommentRepository fakeCommentRepository = new FakeCommentRepository();
        FakePostRepository fakePostRepository = new FakePostRepository();

        this.commentService = CommentServiceImpl.builder()
                .commentRepository(fakeCommentRepository)
                .postRepository(fakePostRepository)
                .userRepository(fakeUserRepository)
                .build();

        user = User.builder()
                .id(1L)
                .email("test@test.com")
                .name("test_name")
                .nationality("KOREA")
                .introduction("hello world")
                .role(Role.GUEST)
                .provider(Provider.GOOGLE)
                .providerId("1")
                .picture("test_picture")
                .build();

        Post post = Post.builder()
                .id(1L)
                .title("test_title")
                .content("test_content")
                .user(user)
                .build();

        Comment comment = Comment.builder()
                .id(1L)
                .user(user)
                .post(post)
                .content("post comment")
                .build();

        fakeUserRepository.save(user);
        fakePostRepository.save(post);
        fakeCommentRepository.save(comment);
    }

    @Test
    void createComment() {
        // given
        CommentCreate commentCreate = CommentCreate.builder()
                .postId(1L)
                .content("create comment")
                .build();

        // when
        Comment createdComment = commentService.createComment(user, commentCreate);
        List<Comment> commentList = commentService.getCommentList(1L, 1L, 10);

        // then
        assertThat(createdComment.getContent()).isEqualTo("create comment");
        assertThat(commentList.size()).isEqualTo(2);
    }

    @Test
    void getCommentList() {
        // given

        // when
        List<Comment> commentList = commentService.getCommentList(1L, 1L, 10);

        // then
        assertThat(commentList.size()).isEqualTo(1);
    }

    @Test
    void deleteComment() {
        // given
        // when
        commentService.deleteComment(user, 1L);
        List<Comment> commentList = commentService.getCommentList(1L, 1L, 10);

        // then
        assertThat(commentList.size()).isEqualTo(0);
    }

    @Test
    void updateComment() {
        // given
        CommentUpdate commentUpdate = CommentUpdate.builder()
                .content("update comment")
                .build();

        // when
        Comment result = commentService.updateComment(user, 1L, commentUpdate);

        // then
        assertThat(result.getContent()).isEqualTo("update comment");
    }
}
