package com.longing.longing.report.service;

import com.longing.longing.common.domain.ResourceNotFoundException;
import com.longing.longing.common.exceptions.AlreadyReportedPostException;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.longing.longing.post.infrastructure.PostEntity;
import com.longing.longing.post.infrastructure.PostJpaRepository;
import com.longing.longing.report.ReportReason;
import com.longing.longing.report.controller.port.ReportService;
import com.longing.longing.report.domain.PostReport;
import com.longing.longing.report.domain.ReportCreate;
import com.longing.longing.report.infrastructure.PostReportEntity;
import com.longing.longing.report.service.port.ReportRepository;
import com.longing.longing.user.Provider;
import com.longing.longing.user.infrastructure.UserEntity;
import com.longing.longing.user.infrastructure.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final UserJpaRepository userJpaRepository;
    private final ReportRepository reportRepository;
    private final PostJpaRepository postJpaRepository;

    private void checkAlreadyReportedPost(UserEntity reporter, PostEntity post, ReportReason reportReason) {
        reportRepository.findOne(reporter, post, reportReason)
                .ifPresent(r -> {
                    throw new AlreadyReportedPostException("you already reported this post!");
                });
    }

    @Override
    public PostReport createPostReport(Long postId, ReportCreate reportCreate, CustomUserDetails userDetails) {
        String email = userDetails.getEmail();
        Provider provider = userDetails.getProvider();
        ReportReason reportReason = reportCreate.getReportReason();

        UserEntity reporter = userJpaRepository.findByEmailAndProvider(email, provider)
                .orElseThrow(() -> new ResourceNotFoundException("Users", email));

        PostEntity postEntity = postJpaRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Posts", postId));

        /**
         * 유저가 이미 신고한 게시글인지 확인
         */
        checkAlreadyReportedPost(reporter, postEntity, reportReason);

        PostReportEntity postReportEntity = PostReportEntity.builder()
                .reporter(reporter)
                .post(postEntity)
                .reportReason(reportReason)
                .build();

        return reportRepository.create(postReportEntity);
    }
}
