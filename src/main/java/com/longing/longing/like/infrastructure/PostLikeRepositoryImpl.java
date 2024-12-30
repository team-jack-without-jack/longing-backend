package com.longing.longing.like.infrastructure;

import com.longing.longing.like.domain.LikePost;
import com.longing.longing.like.service.port.PostLikeRepository;
import com.longing.longing.post.domain.Post;
import com.longing.longing.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PostLikeRepositoryImpl implements PostLikeRepository {

    private final PostLikeJpaRepository postLikeJpaRepository;

    @Override
    public void likePost(LikePost likePost) {

    }

    @Override
    public Optional<LikePost> findByPostAndUser(Post post, User user) {
        return postLikeJpaRepository.findByPostAndUser(post, user).map(PostLikeEntity::toModel);
    }

    @Override
    public LikePost save(LikePost likePost) {
        return postLikeJpaRepository.save(PostLikeEntity.fromModel(likePost)).toModel();
    }

    @Override
    public Optional<LikePost> findById(long likeId) {
        return Optional.empty();
    }

    @Override
    public void deleteById(long likeId) {

    }
}
