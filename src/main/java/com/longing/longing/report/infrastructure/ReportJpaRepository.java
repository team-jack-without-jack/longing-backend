package com.longing.longing.report.infrastructure;

import com.longing.longing.post.infrastructure.PostEntity;
import com.longing.longing.report.ReportReason;
import com.longing.longing.user.infrastructure.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportJpaRepository extends JpaRepository<PostReportEntity, Long> {

    Optional<PostReportEntity> findByReporterAndPostAndReportReason(UserEntity reporter, PostEntity post, ReportReason reportReason);
}
