package com.longing.longing.post.infrastructure;

import com.longing.longing.post.domain.Post;
import com.longing.longing.post.service.port.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepository {

    private final PostJpaRepository postJpaRepository;

    public Optional<Post> findById(long id) {
        return postJpaRepository.findById(id).map(PostEntity::toModel);
    }

    @Override
    public Post save(Post post) {
        return postJpaRepository.save(PostEntity.fromModel(post)).toModel();
    }

    @Override
    public List<Post> findAll() {
        return postJpaRepository.findAll().stream()
                .map(PostEntity::toModel)
                .collect(Collectors.toList());
    }
}
