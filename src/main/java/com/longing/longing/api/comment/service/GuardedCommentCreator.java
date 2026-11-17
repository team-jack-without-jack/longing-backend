package com.longing.longing.api.comment.service;

import com.longing.longing.api.comment.controller.port.CommentService;
import com.longing.longing.api.comment.domain.Comment;
import com.longing.longing.api.comment.domain.CommentCreate;
import com.longing.longing.api.comment.infrastructure.CommentEntity;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.common.writeguard.CreateGuardExecutor;
import com.longing.longing.common.writeguard.CreateOutcome;
import com.longing.longing.common.writeguard.CreateRequest;
import com.longing.longing.common.writeguard.Fingerprints;
import com.longing.longing.common.writeguard.IdempotencyKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/** 트랜잭션 없음: 서비스 빈(트랜잭션 프록시)을 호출한다. */
@Component
@RequiredArgsConstructor
public class GuardedCommentCreator {

    private final CommentService commentService;
    private final CreateGuardExecutor executor;

    public CreateOutcome<Comment> create(User user, CommentCreate commentCreate, Optional<IdempotencyKey> key) {
        long userId = user.getId();
        return executor.execute(CreateRequest.<Comment>builder()
                .scope("comment")
                .userId(userId)
                .key(key)
                .fingerprint(commentCreate.getPostId() + ":" + Fingerprints.sha256Hex(commentCreate.getContent()))
                .uniqueConstraint(CommentEntity.CLIENT_REQUEST_UNIQUE_CONSTRAINT)
                .findByKey(clientRequestId -> commentService.findByClientRequestId(userId, clientRequestId))
                .sameContent(existing -> existing.getPost() != null
                        && Objects.equals(existing.getPost().getId(), commentCreate.getPostId())
                        && Objects.equals(existing.getContent(), commentCreate.getContent()))
                .create(clientRequestId -> commentService.createComment(user, commentCreate, clientRequestId))
                .build());
    }
}
