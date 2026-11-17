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
import java.util.List;
import java.util.Optional;

public interface PostJpaRepository extends JpaRepository<PostEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PostEntity p WHERE p.id = :id")
    Optional<PostEntity> findByIdForUpdate(@Param("id") Long id);

    /** 사용자의 살아 있는 게시글 id를 오름차순으로 읽는다. 잠그지 않는다. 회원 탈퇴가 이 순서로 한 건씩 모두 잠근 뒤 지운다 (#128). */
    @Query("SELECT p.id FROM PostEntity p WHERE p.user.id = :userId ORDER BY p.id")
    List<Long> findIdsByUserId(@Param("userId") Long userId);

    // ---- 게시글 삭제 (#128). PostRepositoryImpl.softDelete가 게시글 행을 잠근 뒤 이 순서로 부른다. ----
    // 벌크 문장이라 @Where와 감사 기능을 거치지 않으므로 deleted 조건과 시각을 직접 넣는다.

    @Modifying
    @Query("DELETE FROM PostLikeEntity l WHERE l.post.id = :postId")
    int deleteLikesOf(@Param("postId") Long postId);

    @Modifying
    @Query("DELETE FROM PostBookmarkEntity b WHERE b.post.id = :postId")
    int deleteBookmarksOf(@Param("postId") Long postId);

    @Modifying
    @Query("UPDATE CommentEntity c SET c.deleted = true, c.deletedDate = :now "
            + "WHERE c.post.id = :postId AND c.deleted = false")
    int softDeleteCommentsOf(@Param("postId") Long postId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE PostImageEntity i SET i.deleted = true, i.deletedDate = :now "
            + "WHERE i.post.id = :postId AND i.deleted = false")
    int softDeleteImagesOf(@Param("postId") Long postId, @Param("now") LocalDateTime now);

    /**
     * 마지막 문장이다.
     * flushAutomatically: 아래 clear가 아직 DB에 보내지 않은 변경을 버리지 않도록 먼저 보낸다.
     * clearAutomatically: 남은 게시글·자식 엔티티를 비워서, 같은 요청에서 다시 읽으면 지워진 상태가 보이게 한다.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE PostEntity p SET p.deleted = true, p.deletedDate = :now WHERE p.id = :postId AND p.deleted = false")
    int softDeletePost(@Param("postId") Long postId, @Param("now") LocalDateTime now);


    @EntityGraph(attributePaths = {"postImageEntities"})
    Optional<PostEntity> findById(long id);

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
