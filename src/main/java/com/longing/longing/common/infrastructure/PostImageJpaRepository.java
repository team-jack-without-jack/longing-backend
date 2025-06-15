package com.longing.longing.common.infrastructure;

import com.longing.longing.api.post.infrastructure.PostEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostImageJpaRepository extends JpaRepository<PostImageEntity, Long> {
    void deleteAllByPost(PostEntity postEntity);
}
