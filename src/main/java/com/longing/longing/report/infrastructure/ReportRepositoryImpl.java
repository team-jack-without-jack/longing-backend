package com.longing.longing.report.infrastructure;

import com.longing.longing.post.infrastructure.PostEntity;
import com.longing.longing.report.ReportReason;
import com.longing.longing.report.domain.PostReport;
import com.longing.longing.report.service.port.ReportRepository;
import com.longing.longing.user.infrastructure.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReportRepositoryImpl implements ReportRepository {

    private final ReportJpaRepository reportJpaRepository;

    @Override
    public PostReport create(PostReportEntity postReportEntity) {
        PostReportEntity postReport = reportJpaRepository.save(postReportEntity);
        return postReport.toModel();
    }


    @Override
    public Optional<PostReportEntity> findOne(UserEntity reporter, PostEntity post, ReportReason reportReason) {
        return reportJpaRepository.findByReporterAndPostAndReportReason(reporter, post, reportReason);
    }
}
