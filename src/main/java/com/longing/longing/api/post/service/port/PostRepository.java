package com.longing.longing.api.post.service.port;

import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.post.infrastructure.PostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface PostRepository {
    Optional<Post> findById(Long id, Long userId);
    Optional<Post> findByIdForUpdate(Long id);

    Post save(Post post);

    /**
     * 제목·본문·수정 시각만 바꾸고 바뀐 행 수를 반환한다. 없거나 삭제된 게시글이면 0이다.
     * 좋아요·댓글 수와 이미지는 그대로 둔다. 요청 EntityManager를 반영 후 비운다 (#126).
     */
    int updateContent(long postId, String title, String content);

    Page<Post> findAll(Long userId, Pageable pageable);

    /**
     * 게시글을 soft delete한다 (#128). 게시글 행을 먼저 잠근 뒤, 좋아요·북마크는 지우고 댓글·이미지·게시글은 soft delete한다.
     * 신고는 근거로 남긴다. 호출한 쪽의 트랜잭션이 있으면 합류한다.
     * 끝에 영속성 컨텍스트를 flush한 뒤 비운다. 호출한 쪽이 관리하던 엔티티는 준영속이 된다.
     * @return 지웠으면 1, 없거나 이미 삭제됐으면 0
     */
    int softDelete(long postId);

    /**
     * 회원 탈퇴 (#128). 사용자의 살아 있는 게시글을 id 오름차순으로 기본 키로 한 건씩 모두 잠근 뒤, 게시글마다 softDelete와 같은 문장을 실행한다.
     * 지운 게시글 수를 반환한다.
     * 한 문장으로 여러 게시글을 잠그지 않는다(실행 계획에 따라 다른 사용자의 게시글 행까지 잠글 수 있다).
     * 잠금을 자식 문장보다 먼저 끝낸다(자식 문장의 간격 잠금을 쥔 채 다음 게시글을 기다리면 교착 상태가 난다).
     */
    int softDeleteAllByUserId(long userId);

    Page<Post> findAllwithLikeCountAndSearch(Long userId, String keyword, Pageable pageable);
    Page<Post> findMyPostsWithLikeCountAndSearch(Long userId, String keyword, Pageable pageable);

    Optional<Post> findByIdAndUserId(Long id, Long userId);

    void incrementLikeCount(Long postId);
    void decrementLikeCount(Long postId);
  
    void incrementCommentCount(Long postId);
    void decrementCommentCount(Long postId);

    void flush();

    Page<Post> findByUserId(long userId, Pageable pageable);

    Optional<Post> findByUserIdAndClientRequestId(long userId, String clientRequestId);
}
