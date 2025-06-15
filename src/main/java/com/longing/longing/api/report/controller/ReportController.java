package com.longing.longing.api.report.controller;

import com.longing.longing.common.response.ApiResponse;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.longing.longing.api.report.controller.port.ReportService;
import com.longing.longing.api.report.domain.PostReport;
import com.longing.longing.api.report.domain.PostReportResponse;
import com.longing.longing.api.report.domain.ReportCreate;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * Post 신고 기능
     * @param postId
     * @param customUserDetails
     * @return
     */
    @PostMapping("/posts/{id}")
    public ApiResponse<PostReportResponse> postReport(
            @PathVariable("id") long postId,
            @RequestBody ReportCreate reportCreate,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        PostReport postReportCreate = reportService.createPostReport(postId, reportCreate, customUserDetails);
        return ApiResponse.created(PostReportResponse.from(postReportCreate));
    }
}
