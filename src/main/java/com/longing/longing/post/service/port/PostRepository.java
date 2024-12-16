package com.longing.longing.post.service.port;

import com.longing.longing.post.domain.Post;
import com.longing.longing.post.infrastructure.PostEntity;

import java.util.List;
import java.util.Optional;

public interface PostRepository {
//    Optional<PostEntity> findById(long id);
    Optional<Post> findById(long id);

    Post save(Post post);

    List<Post> findAll();

}
