package com.longing.longing.report.domain;

import com.longing.longing.post.domain.Post;
import com.longing.longing.post.domain.PostCreate;
import com.longing.longing.report.ReportReason;
import com.longing.longing.user.domain.User;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PostReport {
    private final Long id;
    private final Post post;
    private final User reporter;
    private final ReportReason reportReason;

    @Builder
    public PostReport(Long id, Post post, User reporter, ReportReason reportReason) {
        this.id = id ;
        this.post = post;
        this.reporter = reporter;
        this.reportReason = reportReason;
    }
}
