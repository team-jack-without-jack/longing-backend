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
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Builder
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    private final ReportRepository reportRepository;
    private final PostRepository postRepository;

    private void checkAlreadyReportedPost(User reporter, Post post, ReportReason reportReason) {
        reportRepository.findOne(reporter, post, reportReason)
                .ifPresent(r -> {
                    throw new AlreadyReportedPostException("you already reported this post!");
                });
    }
    @Override
    @Transactional
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

        PostReport createdReport = reportRepository.create(post, reporter, postReport);

        /**
         * 해당 게시글의 신고가 동일 사유로 3번인 경우 postDelete
         */
        long totalByReason = reportRepository.countByPostAndReportReasonForUpdate(post, reportReason);
        if (totalByReason >= 3) {
            postRepository.deleteById(post.getId());
        }

        return createdReport;
    }
}
