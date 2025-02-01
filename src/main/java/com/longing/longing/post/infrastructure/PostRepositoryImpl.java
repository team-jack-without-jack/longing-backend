package com.longing.longing.post.infrastructure;

import com.longing.longing.post.domain.Post;
import com.longing.longing.post.service.port.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Slf4j
@Repository
@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepository {

    private final PostJpaRepository postJpaRepository;

    public Optional<Post> findById(Long id) {
        return postJpaRepository.findById(id).map(PostEntity::toModel);
    }


    @Override
    public Post save(Post post) {
        return postJpaRepository.save(PostEntity.fromModel(post)).toModel();
    }

    @Override
    public Page<Post> findAll(Pageable pageable) {
        List<Post> posts = postJpaRepository.findAll(pageable).stream()
                .map(PostEntity::toModel)
                .collect(Collectors.toList());

        return new PageImpl<>(posts, pageable, posts.size());
    }



    public void deleteById(Long postId) {
        postJpaRepository.deleteById(postId);
    }

    @Override
    public Page<Post> findAllwithLikeCountAndSearch(String keyword, Pageable pageable) {
        List<Post> postEntities = postJpaRepository.findAllWithLikeCountAndSearch(keyword, pageable)
                .stream()
                .map(PostEntity::toModel)
                .collect(Collectors.toList());

        return new PageImpl<>(postEntities, pageable, postEntities.size());
    }

    @Override
    public void flush() {
        postJpaRepository.flush();
    }


}
