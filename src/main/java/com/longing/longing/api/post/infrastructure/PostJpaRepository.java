package com.longing.longing.api.post.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import javax.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PostJpaRepository extends JpaRepository<PostEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PostEntity p WHERE p.id = :id")
    Optional<PostEntity> findByIdForUpdate(@Param("id") Long id);


    @EntityGraph(attributePaths = {"postImageEntities"})
    Optional<PostEntity> findById(long id);
    void deleteById(long id);

    Optional<PostEntity> findByIdAndUserId(Long postId, Long userId);

    /**
     * 제목·본문·수정 시각만 바꾼다. 좋아요·댓글 수와 이미지 목록은 건드리지 않는다 (#126).
     * 벌크 UPDATE라 감사(@LastModifiedDate)와 @Where를 거치지 않으므로 수정 시각과 deleted 조건을 직접 넣는다.
     * flushAutomatically·clearAutomatically: 요청 EntityManager(open-in-view)의 변경을 먼저 반영한 뒤 남은 엔티티
     * (옛 게시글 포함)를 모두 비워서, 이어서 다시 조회할 때 바뀐 값을 읽게 한다.
     */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE PostEntity p SET p.title = :title, p.content = :content, p.modifiedDate = :now WHERE p.id = :id AND p.deleted = false")
    int updateContent(@Param("id") Long id, @Param("title") String title, @Param("content") String content,
                      @Param("now") LocalDateTime now);

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

    Page<PostEntity> findByUserId(long userId, Pageable pageable);
}
