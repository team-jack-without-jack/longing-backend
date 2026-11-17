package com.longing.longing.api.comment.service;

import com.longing.longing.api.comment.controller.port.CommentService;
import com.longing.longing.api.comment.domain.Comment;
import com.longing.longing.api.comment.domain.CommentCreate;
import com.longing.longing.api.comment.domain.CommentUpdate;
import com.longing.longing.api.comment.infrastructure.CommentEntity;
import com.longing.longing.api.comment.service.port.CommentRepository;
import com.longing.longing.common.exceptions.ResourceNotFoundException;
import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.post.infrastructure.PostEntity;
import com.longing.longing.api.post.infrastructure.PostJpaRepository;
import com.longing.longing.api.post.service.port.PostRepository;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.api.user.service.port.UserRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Builder
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostJpaRepository postJpaRepository;
    private final PostRepository postRepository;

    @Transactional
    public Comment _createComment(User user, CommentCreate commentCreate) {
        PostEntity postEntity = postJpaRepository.findById(commentCreate.getPostId())
                .orElseThrow(() -> new ResourceNotFoundException("Posts", commentCreate.getPostId()));

        Comment comment = Comment.from(user, postEntity.toModel(), commentCreate);

        CommentEntity commentEntity = CommentEntity.fromModel(comment);
        postEntity.addComment(commentEntity);
        return comment;
    }

    @Override
    @Transactional
    public Comment createComment(User user, CommentCreate commentCreate, String clientRequestId) {
        Post post = postRepository.findById(commentCreate.getPostId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Posts", commentCreate.getPostId()));

        Comment comment = Comment.from(user, post, commentCreate, clientRequestId);

        postRepository.incrementCommentCount(post.getId());
        return commentRepository.save(comment);
    }

    /**
     * 댓글 삭제 (#126).
     * 댓글 수를 먼저 줄이고(게시글 행 잠금) 조건부 soft delete를 한다. 다른 요청이 먼저 지워 0행이면
     * RESOURCE_NOT_FOUND(기존과 같음)를 던지고, 트랜잭션이 롤백되며 줄인 댓글 수도 되돌아간다.
     * 그래서 동시에 지워도 댓글 수는 한 번만 준다.
     * 게시글 행을 먼저 잠그는 순서라 신고 자동 삭제(게시글 잠금 → 댓글 일괄 soft delete)와는 교착 상태가 없다.
     * 게시글 삭제·회원 탈퇴도 게시글 행을 먼저 잠그므로(#128) 교착 상태가 없다.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Comment> findByClientRequestId(long userId, String clientRequestId) {
        if (clientRequestId == null) {
            return Optional.empty();
        }
        return commentRepository.findByUserIdAndClientRequestId(userId, clientRequestId);
    }

    @Override
    @Transactional
    public void deleteComment(User user, long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comments", commentId));

        if (!comment.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("not a user who created");
        }

        Post post = postRepository.findById(comment.getPost().getId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Posts", comment.getPost().getId()));

        postRepository.decrementCommentCount(post.getId());
        if (commentRepository.softDelete(commentId) == 0) {
            throw new ResourceNotFoundException("Comments", commentId);
        }
    }

    /**
     * 댓글 수정 (#129).
     * 먼저 읽지 않고 조건부 UPDATE 한 문장으로 쓴다(작성자의 삭제되지 않은 댓글일 때만, 내용이 null이면 내용은 그대로).
     * 전에는 읽어 둔 댓글 전체를 다시 저장(merge)해서, 그사이 커밋된 삭제를 되돌려 댓글이 살아났다.
     * 0행(없음·남의 댓글·삭제됨)이면 기존처럼 RESOURCE_NOT_FOUND다.
     * 응답은 UPDATE 뒤 다시 조회한 댓글이라 내용과 수정 시각이 최신이다.
     * 댓글 행 하나만 잠그므로, 게시글 행 → 댓글 행 순서로 잠그는 댓글 삭제와 교착 상태가 없다.
     */
    @Override
    @Transactional
    public Comment updateComment(User writer, long commentId, CommentUpdate commentUpdate) {
        if (commentRepository.updateContent(commentId, writer.getId(), commentUpdate.getContent()) == 0) {
            throw new ResourceNotFoundException("Comment", commentId);
        }
        return commentRepository.findByIdAndUserId(commentId, writer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));
    }

    @Override
    public List<Comment> getCommentList(long postId, long lastCommentId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return commentRepository.getCommentList(postId, lastCommentId, pageable);
    }
}
