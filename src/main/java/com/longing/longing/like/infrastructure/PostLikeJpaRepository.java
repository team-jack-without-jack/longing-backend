package com.longing.longing.like.infrastructure;

import com.longing.longing.like.domain.LikePost;
import com.longing.longing.post.domain.Post;
import com.longing.longing.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeJpaRepository extends JpaRepository<PostLikeEntity, Long> {

//    void likePost(LikePost likePost);
    Optional<PostLikeEntity> findByPostAndUser(Post post, User user);

    Optional<PostLikeEntity> findById(long id);

    void deleteById(long id);

}
