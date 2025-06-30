package com.longing.longing.api.report.service.port;

import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.report.ReportReason;
import com.longing.longing.api.report.domain.PostReport;
import com.longing.longing.api.user.domain.User;

import java.util.Optional;

public interface ReportRepository {
    PostReport create(Post post, User user, PostReport postReport);
    Optional<PostReport> findOne(User reporter, Post post, ReportReason reportReason);
    long countByPostAndReportReasonForUpdate(Post post, ReportReason reportReason);
}
