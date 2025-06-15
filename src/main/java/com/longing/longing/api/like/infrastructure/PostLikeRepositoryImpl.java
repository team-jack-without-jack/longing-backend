package com.longing.longing.api.like.infrastructure;

import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.post.infrastructure.PostEntity;
import com.longing.longing.api.like.domain.PostLike;
import com.longing.longing.api.like.service.port.PostLikeRepository;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.api.user.infrastructure.UserEntity;
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

//    @Override
//    public PostLike save(LikePostCreate likePost, Post post, User user) {
//        return postLikeJpaRepository.save(
//                PostLikeEntity.fromModel(
//                        likePost, PostEntity.fromModel(post), UserEntity.fromModel(user))
//        ).toModel();
//    }

    @Override
    public void save(PostLike postLike) {
        PostEntity postEntity   = PostEntity.fromModel(postLike.getPost());
        UserEntity userEntity   = UserEntity.fromModel(postLike.getUser());
        postLikeJpaRepository.save(PostLikeEntity.fromModel(postEntity, userEntity)).toModel();
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
