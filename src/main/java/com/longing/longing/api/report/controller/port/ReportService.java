package com.longing.longing.api.report.controller.port;

import com.longing.longing.api.user.domain.User;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.longing.longing.api.report.domain.PostReport;
import com.longing.longing.api.report.domain.ReportCreate;

public interface ReportService {

    PostReport createPostReport(Long postId, ReportCreate reportCreate, User user);
}
