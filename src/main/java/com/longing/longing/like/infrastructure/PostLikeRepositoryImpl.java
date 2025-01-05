package com.longing.longing.like.infrastructure;

import com.longing.longing.like.domain.LikePostCreate;
import com.longing.longing.like.domain.PostLike;
import com.longing.longing.like.service.port.PostLikeRepository;
import com.longing.longing.post.domain.Post;
import com.longing.longing.post.infrastructure.PostEntity;
import com.longing.longing.user.domain.User;
import com.longing.longing.user.infrastructure.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PostLikeRepositoryImpl implements PostLikeRepository {

    private final PostLikeJpaRepository postLikeJpaRepository;

    @Override
    public Optional<PostLike> findByPostAndUser(Post post, User user) {
        return postLikeJpaRepository.findByPostIdAndUserId(post.getId(), user.getId()).map(PostLikeEntity::toModel);
    }

    @Override
    public PostLike save(LikePostCreate likePost, Post post, User user) {
        return postLikeJpaRepository.save(
                PostLikeEntity.fromModel(
                        likePost, PostEntity.fromModel(post), UserEntity.fromModel(user))
        ).toModel();
    }

    @Override
    public Optional<PostLike> findById(long likeId) {
        return Optional.empty();
    }

    @Override
    public void deleteById(long likeId) {
        postLikeJpaRepository.deleteById(likeId);
    }
}
