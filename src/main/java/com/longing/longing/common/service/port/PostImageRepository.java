package com.longing.longing.common.service.port;

import com.longing.longing.common.domain.PostImage;
import com.longing.longing.api.post.infrastructure.PostEntity;

public interface PostImageRepository {
    PostImage save(PostImage postImage);
    void deleteAllByPost(PostEntity postEntity);
}
