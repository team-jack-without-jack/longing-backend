package com.longing.longing.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.longing.longing.api.comment.domain.Comment;
import com.longing.longing.api.post.domain.Post;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClientRequestIdJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void postJsonDoesNotExposeClientRequestId() throws Exception {
        Post post = Post.builder().id(1L).title("t").content("c").clientRequestId("key-1").build();

        String json = objectMapper.writeValueAsString(post);

        assertThat(json).doesNotContain("clientRequestId").doesNotContain("key-1");
    }

    @Test
    void commentJsonDoesNotExposeClientRequestId() throws Exception {
        Post post = Post.builder().id(1L).title("t").content("c").clientRequestId("key-1").build();
        Comment comment = Comment.builder().id(2L).content("c").post(post).clientRequestId("key-2").build();

        String json = objectMapper.writeValueAsString(comment);

        assertThat(json).doesNotContain("clientRequestId").doesNotContain("key-1").doesNotContain("key-2");
    }
}
