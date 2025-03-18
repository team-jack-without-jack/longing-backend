package com.longing.longing.bookmark.controller.port;

import com.longing.longing.config.auth.dto.CustomUserDetails;

public interface BookmarkService {
    void addBookmark(CustomUserDetails userDetails, Long postId);
    void removeBookmark(CustomUserDetails userDetails, Long postId);
}
