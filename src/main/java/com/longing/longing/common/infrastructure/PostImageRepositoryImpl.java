package com.longing.longing.common.infrastructure;

import com.longing.longing.common.domain.PostImage;
import com.longing.longing.common.service.port.PostImageRepository;
import com.longing.longing.api.post.infrastructure.PostEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@RequiredArgsConstructor
@Repository
public class PostImageRepositoryImpl implements PostImageRepository {

    private final PostImageJpaRepository postImageJpaRepository;

    @Override
    public PostImage save(PostImage postImage) {
        return postImageJpaRepository.save(PostImageEntity.fromModel(postImage)).toModel();
    }

    @Override
    public void deleteAllByPost(PostEntity postEntity) {
        postImageJpaRepository.deleteAllByPost(postEntity);
    }
}
