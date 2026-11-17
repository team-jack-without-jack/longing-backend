package com.longing.longing.mock;

import com.longing.longing.api.post.domain.Post;
import com.longing.longing.bookmark.domain.PostBookmark;
import com.longing.longing.bookmark.service.port.BookmarkRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

public class FakeBookmarkRepository implements BookmarkRepository {

    /** {userId, postId} */
    private final List<long[]> rows = new ArrayList<>();
    private int saveIfAbsentCalls = 0;

    @Override
    public boolean exists(long userId, long postId) {
        return count(userId, postId) > 0;
    }

    @Override
    public boolean saveIfAbsent(PostBookmark postBookmark) {
        saveIfAbsentCalls++;
        long userId = postBookmark.getUser().getId();
        long postId = postBookmark.getPost().getId();
        if (exists(userId, postId)) {
            return false;
        }
        rows.add(new long[]{userId, postId});
        return true;
    }

    @Override
    public int delete(long userId, long postId) {
        int before = rows.size();
        rows.removeIf(row -> row[0] == userId && row[1] == postId);
        return before - rows.size();
    }

    @Override
    public Page<Post> getBookmarkPost(Long userId, Pageable pageable) {
        return Page.empty(pageable);
    }

    public long count(long userId, long postId) {
        return rows.stream().filter(row -> row[0] == userId && row[1] == postId).count();
    }

    public int getSaveIfAbsentCalls() {
        return saveIfAbsentCalls;
    }
}
