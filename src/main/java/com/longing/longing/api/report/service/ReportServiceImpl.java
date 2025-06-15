package com.longing.longing.api.report.service;

import com.longing.longing.api.report.controller.port.ReportService;
import com.longing.longing.api.report.domain.ReportCreate;
import com.longing.longing.api.report.service.port.ReportRepository;
import com.longing.longing.common.domain.ResourceNotFoundException;
import com.longing.longing.common.exceptions.AlreadyReportedPostException;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.post.service.port.PostRepository;
import com.longing.longing.api.report.ReportReason;
import com.longing.longing.api.report.domain.PostReport;
import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.api.user.service.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final PostRepository postRepository;

    private void checkAlreadyReportedPost(User reporter, Post post, ReportReason reportReason) {
        reportRepository.findOne(reporter, post, reportReason)
                .ifPresent(r -> {
                    throw new AlreadyReportedPostException("you already reported this post!");
                });
    }

    @Override
    public PostReport createPostReport(Long postId, ReportCreate reportCreate, User reporter) {
        ReportReason reportReason = reportCreate.getReportReason();

        Post post = postRepository.findById(postId, reporter.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Posts", postId));

        /**
         * 유저가 이미 신고한 게시글인지 확인
         */
        checkAlreadyReportedPost(reporter, post, reportReason);

        PostReport postReport = PostReport.builder()
                .post(post)
                .reporter(reporter)
                .reportReason(reportReason)
                .build();

        return reportRepository.create(post, reporter, postReport);
    }
}
