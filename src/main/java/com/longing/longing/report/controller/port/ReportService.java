package com.longing.longing.report.controller.port;

import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.longing.longing.report.ReportReason;
import com.longing.longing.report.domain.PostReport;
import com.longing.longing.report.domain.ReportCreate;

public interface ReportService {

    PostReport createPostReport(Long postId, ReportCreate reportCreate, CustomUserDetails userDetails);
}
