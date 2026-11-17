package com.longing.longing.api.report.infrastructure;

import com.longing.longing.api.report.ReportReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportJpaRepository extends JpaRepository<PostReportEntity, Long> {

    boolean existsByReporterIdAndPostIdAndReportReason(Long reporterId, Long postId, ReportReason reportReason);

    /** 같은 게시글·사유를 신고한 서로 다른 사용자 수. 마이그레이션 전 중복 행이 남아 있어도 한 사람은 한 번만 센다 (#126). */
    @Query("SELECT COUNT(DISTINCT r.reporter.id) FROM PostReportEntity r WHERE r.post.id = :postId AND r.reportReason = :reason")
    long countReporters(@Param("postId") Long postId, @Param("reason") ReportReason reason);
}
