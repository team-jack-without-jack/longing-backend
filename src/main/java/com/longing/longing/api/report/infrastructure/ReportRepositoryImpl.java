package com.longing.longing.api.report.infrastructure;

import com.longing.longing.api.report.service.port.ReportRepository;
import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.post.infrastructure.PostEntity;
import com.longing.longing.api.report.ReportReason;
import com.longing.longing.api.report.domain.PostReport;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.api.user.infrastructure.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ReportRepositoryImpl implements ReportRepository {

    private final ReportJpaRepository reportJpaRepository;

    @Override
    public PostReport create(Post post, User user, PostReport postReport) {
        PostEntity postEntity = PostEntity.fromModel(post);
        UserEntity userEntity = UserEntity.fromModel(user);
        reportJpaRepository.save(PostReportEntity.fromModel(postEntity, userEntity, postReport.getReportReason()));
        return postReport;
    }


    @Override
    public boolean exists(long reporterId, long postId, ReportReason reportReason) {
        return reportJpaRepository.existsByReporterIdAndPostIdAndReportReason(reporterId, postId, reportReason);
    }

    @Override
    public long countReporters(long postId, ReportReason reportReason) {
        return reportJpaRepository.countReporters(postId, reportReason);
    }
}
