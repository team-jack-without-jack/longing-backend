package com.longing.longing.api.report.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.longing.longing.api.report.ReportReason;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ReportCreate {
    private ReportReason reportReason;

    public ReportCreate(@JsonProperty("reportReason") ReportReason reportReason) {
        this.reportReason = reportReason;
    }
}
