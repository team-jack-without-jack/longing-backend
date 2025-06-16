package com.longing.longing.mock;

import com.longing.longing.api.post.infrastructure.PostEntity;
import com.longing.longing.common.domain.PostImage;
import com.longing.longing.common.service.port.PostImageRepository;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FakePostImageRepository implements PostImageRepository {

    private final Map<Long, PostImage> postImageMap = new ConcurrentHashMap<>();
    private Long sequence = 1L;

    @Override
    public PostImage save(PostImage postImage) {
        if (postImage.getId() == null) {
            postImage = PostImage.builder()
                    .id(sequence++)
                    .address(postImage.getAddress())
                    .post(postImage.getPost())
                    .user(postImage.getUser())
                    .build();
        }
        postImageMap.put(postImage.getId(), postImage);
        return postImage;
    }

    @Override
    public void deleteAllByPost(PostEntity postEntity) {
        postImageMap.values().removeIf(postImage -> 
            postImage.getPost().getId().equals(postEntity.getId()));
    }

    public List<PostImage> findAll() {
        return new ArrayList<>(postImageMap.values());
    }

    public void clear() {
        postImageMap.clear();
        sequence = 1L;
    }
}
