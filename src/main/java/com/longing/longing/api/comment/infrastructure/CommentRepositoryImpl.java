package com.longing.longing.api.comment.infrastructure;

import com.longing.longing.api.comment.service.port.CommentRepository;
import com.longing.longing.api.comment.domain.Comment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CommentRepositoryImpl implements CommentRepository {

    private final CommentJpaRepository commentJpaRepository;

    @Override
    public Comment save(Comment comment) {
        return commentJpaRepository.save(CommentEntity.fromModel(comment)).toModel();
    }

    @Override
    public Optional<Comment> findById(Long id) {
        return commentJpaRepository.findById(id).map(CommentEntity::toModel);
    }

    @Override
    public Page<Comment> findByPostId(long postId, Pageable pageable) {
        return commentJpaRepository.findByPostId(postId, pageable).map(CommentEntity::toModel);
    }

    @Override
    public List<Comment> getCommentList(long postId, long lastCommentId, Pageable pageable) {
         return commentJpaRepository.findByPostIdAndIdGreaterThanOrderByIdAsc(postId, lastCommentId, pageable)
                .stream()
                .map(CommentEntity::toModel)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(long commentId) {
        commentJpaRepository.deleteById(commentId);
    }

    @Override
    public Optional<Comment> findByIdAndUserId(long commentId, long id) {
        return commentJpaRepository.findByIdAndUserId(commentId, id).map(CommentEntity::toModel);
    }


}
