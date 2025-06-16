package com.longing.longing.api.report.domain;

import com.longing.longing.api.report.ReportReason;
import lombok.Getter;

@Getter
public class PostReportResponse {
    private final Long reportId;
    private final Long postId;
    private final Long reporterId;
    private final ReportReason reportReason;

    public PostReportResponse(Long reportId, Long postId, Long reporterId, ReportReason reportReason) {
        this.reportId = reportId;
        this.postId = postId;
        this.reporterId = reporterId;
        this.reportReason = reportReason;
    }

    public static PostReportResponse from(PostReport report) {
        return new PostReportResponse(
                report.getId(),
                report.getPost().getId(),
                report.getReporter().getId(),
                report.getReportReason()
        );
    }
}
