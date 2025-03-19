package com.longing.longing.bookmark.controller.port;

import com.longing.longing.bookmark.domain.PostBookmark;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.longing.longing.post.domain.Post;
import org.springframework.data.domain.Page;

public interface BookmarkService {
    void addBookmark(CustomUserDetails userDetails, Long postId);
    void removeBookmark(CustomUserDetails userDetails, Long postId);
    Page<Post> getBookmarkPost(
            CustomUserDetails userDetails,
            Integer page,
            Integer size,
            String sortBy,
            String sortDirection);
}
