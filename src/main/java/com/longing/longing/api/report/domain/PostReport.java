package com.longing.longing.api.report.domain;

import com.longing.longing.api.user.domain.User;
import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.report.ReportReason;
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
