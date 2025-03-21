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
    public Page<Post> findAll(Long userId, Pageable pageable) {
//        List<Post> posts = postJpaRepository.findAll(userId, pageable).stream()
//                .map(PostEntity::toModel)
//                .collect(Collectors.toList());
//
//        return new PageImpl<>(posts, pageable, posts.size());
        Page<Object[]> results = postJpaRepository.findAll(userId, pageable);

        List<Post> postEntities = results.getContent().stream().map(result -> {
            PostEntity postEntity = (PostEntity) result[0]; // 첫 번째 항목은 PostEntity
            Boolean bookmarked = (Boolean) result[1]; // 두 번째 항목은 bookmarked 값
            Boolean liked = (Boolean) result[2]; // 세 번째 항목은 liked 값

            return postEntity.toModel(bookmarked, liked); // PostEntity를 Post로 변환
        }).collect(Collectors.toList());

        return new PageImpl<>(postEntities, pageable, results.getTotalElements());
    }



    public void deleteById(Long postId) {
        postJpaRepository.deleteById(postId);
    }

    @Override
    public Page<Post> findAllwithLikeCountAndSearch(Long userId, String keyword, Pageable pageable) {
        Page<Object[]> results = postJpaRepository.findAllWithLikeCountAndSearch(userId, keyword, pageable);

        List<Post> postEntities = results.getContent().stream().map(result -> {
            PostEntity postEntity = (PostEntity) result[0]; // 첫 번째 항목은 PostEntity
            Boolean bookmarked = (Boolean) result[1]; // 두 번째 항목은 bookmarked 값
            Boolean liked = (Boolean) result[2]; // 세 번째 항목은 liked 값

            return postEntity.toModel(bookmarked, liked); // PostEntity를 Post로 변환
        }).collect(Collectors.toList());

        return new PageImpl<>(postEntities, pageable, results.getTotalElements());
    }

    @Override
    public Page<Post> findMyPostsWithLikeCountAndSearch(Long userId, String keyword, Pageable pageable) {
        List<Post> postEntities = postJpaRepository.findMyPostsWithLikeCountAndSearch(userId, keyword, pageable)
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
