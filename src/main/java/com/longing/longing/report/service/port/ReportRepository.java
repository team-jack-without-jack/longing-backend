package com.longing.longing.report.service.port;

import com.longing.longing.post.infrastructure.PostEntity;
import com.longing.longing.report.ReportReason;
import com.longing.longing.report.domain.PostReport;
import com.longing.longing.report.infrastructure.PostReportEntity;
import com.longing.longing.user.infrastructure.UserEntity;

import java.util.Optional;

public interface ReportRepository {
    PostReport create(PostReportEntity postReportEntity);
    Optional<PostReportEntity> findOne(UserEntity reporter, PostEntity post, ReportReason reportReason);
}
