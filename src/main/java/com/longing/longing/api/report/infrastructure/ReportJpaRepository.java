package com.longing.longing.api.report.infrastructure;

import com.longing.longing.api.post.infrastructure.PostEntity;
import com.longing.longing.api.report.ReportReason;
import com.longing.longing.api.user.infrastructure.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportJpaRepository extends JpaRepository<PostReportEntity, Long> {

    Optional<PostReportEntity> findByReporterAndPostAndReportReason(UserEntity reporter, PostEntity post, ReportReason reportReason);
}
