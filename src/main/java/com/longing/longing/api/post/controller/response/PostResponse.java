package com.longing.longing.api.post.controller.response;

import com.longing.longing.api.comment.controller.response.CommentResponse;
import com.longing.longing.api.comment.domain.Comment;
import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.common.domain.PostImage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class PostResponse {

    private final Long id;
    private final String title;
    private final String content;
    private final User user;
    private int likeCount = 0;
    private int commentCount = 0;
    private boolean liked;
    private boolean bookmarked;

    List<PostImage> postImageList;
    private final LocalDateTime createdDate;
    private final LocalDateTime modifiedDate;

    @Builder
    public PostResponse(
            Long id,
            String title,
            String content,
            User user,
            int likeCount,
            int commentCount,
            boolean liked,
            boolean bookmarked,
            List<PostImage> postImageList,
            LocalDateTime createdDate,
            LocalDateTime modifiedDate) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.user = user;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.liked = liked;
        this.bookmarked = bookmarked;
        this.postImageList = postImageList;
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
    }


    public static PostResponse fromDomain(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .user(post.getUser())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .liked(post.isLiked())
                .bookmarked(post.isBookmarked())
                .postImageList(post.getPostImageList())
                .createdDate(post.getCreatedDate())
                .modifiedDate(post.getModifiedDate())
                .build();
    }
}
