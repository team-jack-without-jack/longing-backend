package com.longing.longing.api.post.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostJpaRepository extends JpaRepository<PostEntity, Long> {


    @EntityGraph(attributePaths = {"postImageEntities"})
    Optional<PostEntity> findById(long id);
    void deleteById(long id);

    @EntityGraph(attributePaths = {"postLikeEntities"})
    @Query("SELECT p, " +
            "CASE WHEN pb.user.id = :userId THEN TRUE ELSE FALSE END AS bookmarked, " +
            "CASE WHEN pl.user.id = :userId THEN TRUE ELSE FALSE END AS liked " +
            "FROM PostEntity p " +
            "LEFT JOIN PostBookmarkEntity pb ON p.id = pb.post.id AND pb.user.id = :userId " +
            "LEFT JOIN PostLikeEntity pl ON p.id = pl.post.id AND pl.user.id = :userId")
    Page<Object[]> findAll(
        @Param("userId") Long userId,
        Pageable pageable);


    @EntityGraph(attributePaths = {"postLikeEntities"})
//    @Query("SELECT p FROM PostEntity p " +
//            "WHERE p.title LIKE %:keyword% OR p.content LIKE %:keyword%")
    @Query("SELECT p, " +
            "CASE WHEN pb.user.id = :userId THEN TRUE ELSE FALSE END AS bookmarked, " +
            "CASE WHEN pl.user.id = :userId THEN TRUE ELSE FALSE END AS liked " +
            "FROM PostEntity p " +
            "LEFT JOIN PostBookmarkEntity pb ON p.id = pb.post.id AND pb.user.id = :userId " +
            "LEFT JOIN PostLikeEntity pl ON p.id = pl.post.id AND pl.user.id = :userId " +
            "WHERE p.title LIKE %:keyword% OR p.content LIKE %:keyword%")
    Page<Object[]> findAllWithLikeCountAndSearch(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            Pageable pageable);

//    @EntityGraph(attributePaths = {"postLikeEntities"})
//    @Query("SELECT p FROM PostEntity p " +
//            "WHERE (p.title LIKE %:keyword% OR p.content LIKE %:keyword%) " +
//            "AND p.user.id = :userId")
//    @EntityGraph(attributePaths = {"postLikeEntities"})
//    @Query("SELECT p, " +
//            "EXISTS (SELECT 1 FROM PostBookmarkEntity pb WHERE pb.post.id = p.id AND pb.user.id = :userId), " +
//            "EXISTS (SELECT 1 FROM PostLikeEntity pl WHERE pl.post.id = p.id AND pl.user.id = :userId) " +
//            "FROM PostEntity p " +
//            "WHERE (p.title LIKE %:keyword% OR p.content LIKE %:keyword%) " +
//            "AND p.user.id = :userId")
//    Page<Object[]> findMyPostsWithLikeCountAndSearch(
//            @Param("userId") Long userId,
//            @Param("keyword") String keyword,
//            Pageable pageable);

    @Query("SELECT p FROM PostEntity p LEFT JOIN FETCH p.postLikeEntities WHERE p.id = :id")
    Optional<PostEntity> findByIdWithLikeCount(@Param("id") Long id);

//    @Query("SELECT p FROM PostEntity p LEFT JOIN FETCH p.postLikeEntities")
//    List<PostEntity> findAllWithLikeCount(Pageable pageable);

    @Modifying
    @Query("UPDATE PostEntity p SET p.likeCount = p.likeCount + 1 WHERE p.id = :postId")
    void incrementLikeCount(@Param("postId") Long postId);

    @Modifying
    @Query("UPDATE PostEntity p SET p.likeCount = p.likeCount - 1 WHERE p.id = :postId")
    void decrementLikeCount(@Param("postId") Long postId);

    @Modifying
    @Query("UPDATE PostEntity p SET p.commentCount = p.commentCount + 1 WHERE p.id = :postId")
    void incrementCommentCount(Long postId);

    @Modifying
    @Query("UPDATE PostEntity p SET p.commentCount = p.commentCount - 1 WHERE p.id = :postId")
    void decrementCommentCount(Long postId);
}
