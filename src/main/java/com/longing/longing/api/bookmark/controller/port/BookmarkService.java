package com.longing.longing.api.bookmark.controller.port;

import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.longing.longing.api.post.domain.Post;
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
