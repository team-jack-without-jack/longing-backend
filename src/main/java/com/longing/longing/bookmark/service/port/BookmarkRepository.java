package com.longing.longing.bookmark.service.port;

import com.longing.longing.api.post.domain.Post;
import com.longing.longing.bookmark.domain.PostBookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookmarkRepository {

    boolean exists(long userId, long postId);

    /**
     * 저장했으면 true, 같은 (사용자, 게시글) 북마크가 이미 있으면 false (#124).
     * 동시 요청은 unique 제약이 한 행만 남긴다. 트랜잭션 밖에서 호출해야 한다.
     * 위반으로 롤백되면 요청의 EntityManager(open-in-view)가 clear되어, 호출 전에 읽은 엔티티는 준영속이 된다.
     * 게시글 행을 먼저 잠근 뒤 저장한다. 게시글이 없거나 그사이 지워졌으면 저장하지 않고
     * ResourceNotFoundException을 던진다 (#133).
     */
    boolean saveIfAbsent(PostBookmark postBookmark);

    /** 해당 (사용자, 게시글)의 북마크를 모두 지우고 지운 행 수를 반환한다. */
    int delete(long userId, long postId);

    Page<Post> getBookmarkPost(Long userId, Pageable pageable);
}
