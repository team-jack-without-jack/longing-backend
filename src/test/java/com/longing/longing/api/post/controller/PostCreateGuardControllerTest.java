package com.longing.longing.api.post.controller;

import com.longing.longing.api.post.controller.port.PostService;
import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.post.service.GuardedPostCreator;
import com.longing.longing.common.writeguard.CreateGuardExecutor;
import com.longing.longing.common.writeguard.RedisWriteGuard;
import com.longing.longing.common.writeguard.WriteGuardProperties;
import com.longing.longing.support.AuthenticatedMvc;
import com.longing.longing.support.RedisTestSupport;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import com.longing.longing.api.post.domain.PostCreate;
import com.longing.longing.api.user.domain.User;
import org.junit.jupiter.api.AfterAll;
import org.mockito.ArgumentCaptor;
import org.springframework.web.multipart.MultipartFile;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.testcontainers.containers.GenericContainer;

import java.nio.charset.StandardCharsets;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PostCreateGuardControllerTest {

    static GenericContainer<?> redis;
    RedisWriteGuard guard;
    PostService postService;
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
        postService = mock(PostService.class);
        CreateGuardExecutor executor = new CreateGuardExecutor(guard, new WriteGuardProperties(), new SimpleMeterRegistry());
        mvc = AuthenticatedMvc.build(new PostController(postService, new GuardedPostCreator(postService, executor)));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        guard.close();
    }

    @Test
    void keylessDoubleSubmitCreatesOnceAndRejectsTheSecondWith409() throws Exception {
        when(postService.createPost(any(), any(), any(), isNull())).thenReturn(post(1L, "title", "content"));

        mvc.perform(createPost("title", "content")).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(header().doesNotExist("Idempotent-Replayed"));
        mvc.perform(createPost("title", "content")).andExpect(status().isConflict())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value(409))
                .andExpect(jsonPath("$.error.message").value("DUPLICATE_SUBMISSION"));

        verify(postService, times(1)).createPost(any(), any(), any(), isNull());
    }

    @Test
    void keylessDifferentContentIsNotBlocked() throws Exception {
        when(postService.createPost(any(), any(), any(), isNull())).thenReturn(post(1L, "title", "content"));

        mvc.perform(createPost("title", "content")).andExpect(status().isOk());
        mvc.perform(createPost("title", "other content")).andExpect(status().isOk());

        verify(postService, times(2)).createPost(any(), any(), any(), isNull());
    }

    @Test
    void keylessFailedCreateDoesNotBlockImmediateRetry() throws Exception {
        when(postService.createPost(any(), any(), any(), isNull()))
                .thenThrow(new IllegalStateException("s3 down"))
                .thenReturn(post(1L, "title", "content"));

        mvc.perform(createPost("title", "content")).andExpect(jsonPath("$.success").value(false));
        mvc.perform(createPost("title", "content")).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(postService, times(2)).createPost(any(), any(), any(), isNull());
    }

    @Test
    void sameKeyReplaysTheOriginalPost() throws Exception {
        Post created = Post.builder().id(5L).title("title").content("content").clientRequestId("key-1").build();
        when(postService.findByClientRequestId(42L, "key-1")).thenReturn(Optional.empty(), Optional.of(created));
        when(postService.createPost(any(), any(), any(), eq("key-1"))).thenReturn(created);

        mvc.perform(createPost("title", "content").header("Idempotency-Key", "key-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(header().doesNotExist("Idempotent-Replayed"));
        mvc.perform(createPost("title", "content").header("Idempotency-Key", "key-1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.clientRequestId").doesNotExist());

        verify(postService, times(1)).createPost(any(), any(), any(), eq("key-1"));
    }

    @Test
    void sameKeyWithDifferentContentIs422() throws Exception {
        when(postService.findByClientRequestId(42L, "key-1")).thenReturn(Optional.of(post(5L, "title", "content")));

        mvc.perform(createPost("title", "changed").header("Idempotency-Key", "key-1"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value(422))
                .andExpect(jsonPath("$.error.message").value("IDEMPOTENCY_KEY_REUSED"));

        verify(postService, never()).createPost(any(), any(), any(), any());
    }

    @Test
    void differentKeysWithSameContentAreBothCreated() throws Exception {
        when(postService.createPost(any(), any(), any(), any())).thenReturn(post(1L, "title", "content"));

        mvc.perform(createPost("title", "content").header("Idempotency-Key", "key-1")).andExpect(status().isOk());
        mvc.perform(createPost("title", "content").header("Idempotency-Key", "key-2")).andExpect(status().isOk());

        verify(postService).createPost(any(), any(), any(), eq("key-1"));
        verify(postService).createPost(any(), any(), any(), eq("key-2"));
    }

    @Test
    void malformedKeyIs400BeforeTouchingTheService() throws Exception {
        mvc.perform(createPost("title", "content").header("Idempotency-Key", "bad key!"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("INVALID_IDEMPOTENCY_KEY"));

        verifyNoInteractions(postService);
    }

    @Test
    void inProgressSameKeyIs409WithRetryAfterOverHttp() throws Exception {
        guard.tryAcquire("write-guard:idem:post:42:key-1", Duration.ofSeconds(30));

        mvc.perform(createPost("title", "content").header("Idempotency-Key", "key-1"))
                .andExpect(status().isConflict())
                .andExpect(header().string("Retry-After", "1"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.error.message").value("REQUEST_IN_PROGRESS"));

        verify(postService, never()).createPost(any(), any(), any(), any());
    }

    @Test
    void redisDownFailsOpenOverHttp() throws Exception {
        MockMvc downMvc = AuthenticatedMvc.build(new PostController(postService, new GuardedPostCreator(postService,
                new CreateGuardExecutor(RedisTestSupport.UNAVAILABLE, new WriteGuardProperties(), new SimpleMeterRegistry()))));
        when(postService.createPost(any(), any(), any(), isNull())).thenReturn(post(1L, "title", "content"));

        downMvc.perform(createPost("title", "content")).andExpect(status().isOk());
        downMvc.perform(createPost("title", "content")).andExpect(status().isOk());

        verify(postService, times(2)).createPost(any(), any(), any(), isNull());
    }

    @Test
    void passesTheAuthenticatedUserContentAndImagesToTheService() throws Exception {
        when(postService.createPost(any(), any(), any(), any())).thenReturn(post(1L, "title", "content"));

        mvc.perform(createPost("title", "content")
                        .file(new MockMultipartFile("images", "a.png", "image/png", new byte[]{1, 2, 3}))
                        .header("Idempotency-Key", "key-9"))
                .andExpect(status().isOk());

        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<PostCreate> create = ArgumentCaptor.forClass(PostCreate.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MultipartFile>> images = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(postService).createPost(user.capture(), create.capture(), images.capture(), key.capture());
        org.assertj.core.api.Assertions.assertThat(user.getValue().getId()).isEqualTo(AuthenticatedMvc.MEMBER.getId());
        org.assertj.core.api.Assertions.assertThat(create.getValue().getTitle()).isEqualTo("title");
        org.assertj.core.api.Assertions.assertThat(create.getValue().getContent()).isEqualTo("content");
        org.assertj.core.api.Assertions.assertThat(images.getValue()).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(images.getValue().get(0).getOriginalFilename()).isEqualTo("a.png");
        org.assertj.core.api.Assertions.assertThat(key.getValue()).isEqualTo("key-9");
    }

    private static MockMultipartHttpServletRequestBuilder createPost(String title, String content) {
        return authenticated(multipart("/api/post")
                .file(text("title", title))
                .file(text("content", content)));
    }

    private static MockMultipartFile text(String name, String value) {
        return new MockMultipartFile(name, null, "text/plain", value.getBytes(StandardCharsets.UTF_8));
    }

    private static Post post(long id, String title, String content) {
        return Post.builder().id(id).title(title).content(content).build();
    }
}
