package com.longing.longing.report.service.port;

import com.longing.longing.post.domain.Post;
import com.longing.longing.post.infrastructure.PostEntity;
import com.longing.longing.report.ReportReason;
import com.longing.longing.report.domain.PostReport;
import com.longing.longing.report.infrastructure.PostReportEntity;
import com.longing.longing.user.domain.User;
import com.longing.longing.user.infrastructure.UserEntity;

import java.util.Optional;

public interface ReportRepository {
    PostReport create(Post post, User user, PostReport postReport);
    Optional<PostReport> findOne(User reporter, Post post, ReportReason reportReason);
}
