package com.longing.longing.comment.infrastructure;

import com.longing.longing.comment.domain.Comment;
import com.longing.longing.comment.service.port.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
        return Optional.empty();
    }

    @Override
    public Page<Comment> findByPostId(long postId, Pageable pageable) {
        return commentJpaRepository.findByPostId(postId, pageable).map(CommentEntity::toModel);
    }
}
