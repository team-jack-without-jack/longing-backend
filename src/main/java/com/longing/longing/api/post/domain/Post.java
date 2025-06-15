package com.longing.longing.api.post.domain;

import com.longing.longing.api.comment.domain.Comment;
import com.longing.longing.common.domain.PostImage;
import com.longing.longing.api.user.domain.User;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
public class Post {

    private Long id;

    private String title;

    private String content;

    private User user;

    private List<Comment> commentList;
    private List<PostImage> postImageList;

    private int likeCount = 0;
    private int commentCount = 0;

    private boolean bookmarked;
    private boolean liked;

    @Builder
    public Post(
            Long id,
            String title,
            String content,
            User user,
            List<Comment> commentList,
            List<PostImage> postImageList,
            int likeCount,
            int commentCount,
            boolean bookmarked,
            boolean liked
    ) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.user = user;
        this.commentList = commentList;
        this.postImageList = postImageList;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.bookmarked = bookmarked;
        this.liked = liked;
    }

    public static Post from(User user, PostCreate postCreate) {
        return Post.builder()
                .title(postCreate.getTitle())
                .content(postCreate.getContent())
                .user(user)
                .build();
    }

    public Post update(PostUpdate postUpdate) {
        if (postUpdate.getTitle() != null) {
            this.title = postUpdate.getTitle();
        }
        if (postUpdate.getContent() != null) {
            this.content = postUpdate.getContent();
        }
        return this;
    }

    // 좋아요 기능 추가
    public void like() {
        this.likeCount++;
    }

    // 좋아요 취소
    public void unlike() {
        this.likeCount = Math.max(0, this.likeCount - 1);
    }

    public void addCommentCount() {
        this.commentCount++;
    }

    public void removeCommentCount() {
        this.commentCount = Math.max(0, this.commentCount - 1);
    }
}
