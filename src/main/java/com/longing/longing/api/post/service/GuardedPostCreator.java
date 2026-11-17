package com.longing.longing.api.post.service;

import com.longing.longing.api.post.controller.port.PostService;
import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.post.domain.PostCreate;
import com.longing.longing.api.post.infrastructure.PostEntity;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.common.writeguard.CreateGuardExecutor;
import com.longing.longing.common.writeguard.CreateOutcome;
import com.longing.longing.common.writeguard.CreateRequest;
import com.longing.longing.common.writeguard.Fingerprints;
import com.longing.longing.common.writeguard.IdempotencyKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** 트랜잭션 없음: 서비스 빈(트랜잭션 프록시)을 호출한다. */
@Component
@RequiredArgsConstructor
public class GuardedPostCreator {

    private final PostService postService;
    private final CreateGuardExecutor executor;

    public CreateOutcome<Post> create(User user, PostCreate postCreate, List<MultipartFile> images,
                                      Optional<IdempotencyKey> key) {
        long userId = user.getId();
        return executor.execute(CreateRequest.<Post>builder()
                .scope("post")
                .userId(userId)
                .key(key)
                .fingerprint(Fingerprints.sha256Hex(postCreate.getTitle() + "\u0000" + postCreate.getContent()))
                .uniqueConstraint(PostEntity.CLIENT_REQUEST_UNIQUE_CONSTRAINT)
                .findByKey(clientRequestId -> postService.findByClientRequestId(userId, clientRequestId))
                .sameContent(existing -> Objects.equals(existing.getTitle(), postCreate.getTitle())
                        && Objects.equals(existing.getContent(), postCreate.getContent()))
                .create(clientRequestId -> postService.createPost(user, postCreate, images, clientRequestId))
                .build());
    }
}
