package com.longing.longing.api.comment.service;

import com.longing.longing.api.comment.domain.Comment;
import com.longing.longing.api.comment.domain.CommentCreate;
import com.longing.longing.api.comment.domain.CommentUpdate;
import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.Role;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.common.exceptions.ResourceNotFoundException;
import com.longing.longing.mock.FakeCommentRepository;
import com.longing.longing.mock.FakePostImageRepository;
import com.longing.longing.mock.FakePostRepository;
import com.longing.longing.mock.FakeUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void 댓글을_삭제하면_댓글_수를_한_번_줄인다() {
        FakePostRepository posts = new FakePostRepository();
        FakeCommentRepository comments = new FakeCommentRepository();
        CommentServiceImpl service = serviceWith(comments, posts);
        seed(posts, comments);

        service.deleteComment(user, 1L);

        assertThat(posts.getDecrementCommentCountCalls()).isEqualTo(1);
        assertThat(comments.findById(1L).isPresent()).isFalse();
    }

    @Test
    void 다른_요청이_먼저_지웠으면_RESOURCE_NOT_FOUND다() {
        FakePostRepository posts = new FakePostRepository();
        // 조회는 됐지만 지우는 순간에는 다른 요청이 이미 지운 상황
        FakeCommentRepository comments = new FakeCommentRepository() {
            @Override
            public int softDelete(long commentId) {
                // 잠금 순서(게시글 행 먼저)를 지키려면 댓글 수를 먼저 줄인 뒤 지워야 한다.
                assertThat(posts.getDecrementCommentCountCalls()).isEqualTo(1);
                return 0;
            }
        };
        CommentServiceImpl service = serviceWith(comments, posts);
        seed(posts, comments);

        // 먼저 줄인 댓글 수는 예외로 트랜잭션이 롤백되며 되돌아간다(Fake에는 트랜잭션이 없어 통합 테스트에서 확인한다).
        assertThatThrownBy(() -> service.deleteComment(user, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void 삭제된_댓글을_수정하면_RESOURCE_NOT_FOUND다() {
        FakePostRepository posts = new FakePostRepository();
        FakeCommentRepository comments = new FakeCommentRepository();
        CommentServiceImpl service = serviceWith(comments, posts);
        seed(posts, comments);
        comments.softDelete(1L);

        assertThatThrownBy(() -> service.updateComment(user, 1L, CommentUpdate.builder().content("edited").build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void 내용_없이_수정하면_내용은_그대로_두고_수정_시각만_바꾼다() {
        FakePostRepository posts = new FakePostRepository();
        FakeCommentRepository comments = new FakeCommentRepository();
        CommentServiceImpl service = serviceWith(comments, posts);
        seed(posts, comments);

        Comment result = service.updateComment(user, 1L, CommentUpdate.builder().build());

        assertThat(result.getContent()).isEqualTo("post comment");
        assertThat(result.getModifiedDate()).isNotNull();
    }

    private CommentServiceImpl serviceWith(FakeCommentRepository comments, FakePostRepository posts) {
        return CommentServiceImpl.builder()
                .commentRepository(comments)
                .postRepository(posts)
                .userRepository(new FakeUserRepository())
                .build();
    }

    private void seed(FakePostRepository posts, FakeCommentRepository comments) {
        Post post = Post.builder().id(1L).title("test_title").content("test_content").user(user).build();
        posts.save(post);
        comments.save(Comment.builder().id(1L).user(user).post(post).content("post comment").build());
    }
}
