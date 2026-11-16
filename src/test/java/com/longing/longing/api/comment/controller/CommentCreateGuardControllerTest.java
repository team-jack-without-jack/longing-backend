package com.longing.longing.api.comment.controller;

import com.longing.longing.api.comment.controller.port.CommentService;
import com.longing.longing.api.comment.domain.Comment;
import com.longing.longing.api.comment.service.GuardedCommentCreator;
import com.longing.longing.api.post.domain.Post;
import com.longing.longing.common.writeguard.CreateGuardExecutor;
import com.longing.longing.common.writeguard.RedisWriteGuard;
import com.longing.longing.common.writeguard.WriteGuardProperties;
import com.longing.longing.support.AuthenticatedMvc;
import com.longing.longing.support.RedisTestSupport;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.GenericContainer;

import java.util.Optional;

import static com.longing.longing.support.AuthenticatedMvc.authenticated;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommentCreateGuardControllerTest {

    static GenericContainer<?> redis;
    RedisWriteGuard guard;
    CommentService commentService;
    MockMvc mvc;

    @BeforeAll
    static void startRedis() {
        redis = RedisTestSupport.startRedis();
    }

    @AfterAll
    static void stopRedis() {
        if (redis != null) {
            redis.stop();
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        RedisTestSupport.flush(redis);
        guard = RedisTestSupport.patientGuard(redis);
        commentService = mock(CommentService.class);
        CreateGuardExecutor executor = new CreateGuardExecutor(guard, new WriteGuardProperties(), new SimpleMeterRegistry());
        mvc = AuthenticatedMvc.build(new CommentController(commentService, new GuardedCommentCreator(commentService, executor)));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        guard.close();
    }

    @Test
    void keylessDoubleSubmitCreatesOnceAndRejectsTheSecondWith409() throws Exception {
        when(commentService.createComment(any(), any(), isNull())).thenReturn(comment(1L, 10L, "hello"));

        mvc.perform(createComment(10L, "hello")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
        mvc.perform(createComment(10L, "hello")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("DUPLICATE_SUBMISSION"));

        verify(commentService, times(1)).createComment(any(), any(), isNull());
    }

    @Test
    void sameContentOnDifferentPostsIsNotBlocked() throws Exception {
        when(commentService.createComment(any(), any(), isNull())).thenReturn(comment(1L, 10L, "hello"));

        mvc.perform(createComment(10L, "hello")).andExpect(status().isOk());
        mvc.perform(createComment(11L, "hello")).andExpect(status().isOk());

        verify(commentService, times(2)).createComment(any(), any(), isNull());
    }

    @Test
    void sameKeyReplaysTheOriginalComment() throws Exception {
        Comment created = Comment.builder().id(3L).content("hello").clientRequestId("key-1")
                .post(Post.builder().id(10L).clientRequestId("author-key").build()).build();
        when(commentService.findByClientRequestId(42L, "key-1")).thenReturn(Optional.empty(), Optional.of(created));
        when(commentService.createComment(any(), any(), eq("key-1"))).thenReturn(created);

        mvc.perform(createComment(10L, "hello").header("Idempotency-Key", "key-1"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Idempotent-Replayed"));
        mvc.perform(createComment(10L, "hello").header("Idempotency-Key", "key-1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andExpect(jsonPath("$.data.id").value(3))
                .andExpect(jsonPath("$.data.clientRequestId").doesNotExist())
                .andExpect(jsonPath("$.data.post.clientRequestId").doesNotExist());

        verify(commentService, times(1)).createComment(any(), any(), eq("key-1"));
    }

    @Test
    void sameKeyOnAnotherPostIs422() throws Exception {
        when(commentService.findByClientRequestId(42L, "key-1")).thenReturn(Optional.of(comment(3L, 10L, "hello")));

        mvc.perform(createComment(11L, "hello").header("Idempotency-Key", "key-1"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.message").value("IDEMPOTENCY_KEY_REUSED"));

        verify(commentService, never()).createComment(any(), any(), any());
    }

    @Test
    void malformedKeyIs400BeforeTouchingTheService() throws Exception {
        mvc.perform(createComment(10L, "hello").header("Idempotency-Key", "x".repeat(65)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commentService);
    }

    private static MockHttpServletRequestBuilder createComment(long postId, String content) {
        return authenticated(post("/api/comment")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"postId\":" + postId + ",\"content\":\"" + content + "\"}"));
    }

    private static Comment comment(long id, long postId, String content) {
        return Comment.builder().id(id).content(content).post(Post.builder().id(postId).build()).build();
    }
}
