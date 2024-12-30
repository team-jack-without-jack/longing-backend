package com.longing.longing.post.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostJpaRepository extends JpaRepository<PostEntity, Long> {
    Optional<PostEntity> findById(long id);
    void deleteById(long id);

    @EntityGraph(attributePaths = {"postLikeEntities"})
    @Query("SELECT p FROM PostEntity p " +
            "WHERE p.title LIKE %:keyword% OR p.content LIKE %:keyword%")
//    @Query("SELECT p FROM PostEntity p " +
//            "LEFT JOIN FETCH p.postLikeEntities " +
//            "WHERE p.title LIKE %:keyword% OR p.content LIKE %:keyword%")
    Page<PostEntity> findAllWithLikeCountAndSearch(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM PostEntity p LEFT JOIN FETCH p.postLikeEntities WHERE p.id = :id")
    Optional<PostEntity> findByIdWithLikeCount(@Param("id") Long id);

//    @Query("SELECT p FROM PostEntity p LEFT JOIN FETCH p.postLikeEntities")
//    List<PostEntity> findAllWithLikeCount(Pageable pageable);
}
