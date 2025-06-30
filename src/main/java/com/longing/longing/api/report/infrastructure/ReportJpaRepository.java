package com.longing.longing.api.report.infrastructure;

import com.longing.longing.api.post.infrastructure.PostEntity;
import com.longing.longing.api.report.ReportReason;
import com.longing.longing.api.user.infrastructure.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.Optional;

public interface ReportJpaRepository extends JpaRepository<PostReportEntity, Long> {

    Optional<PostReportEntity> findByReporterAndPostAndReportReason(UserEntity reporter, PostEntity post, ReportReason reportReason);

//    long countByPostAndReportReasonForUpdate(PostEntity postEntity, ReportReason reportReason);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select count(r) "
            + "from PostReportEntity r "
            + "where r.post = :post "
            + "  and r.reportReason = :reason")
    long countReportsByPostAndReason(
            @Param("post")   PostEntity post,
            @Param("reason") ReportReason reason
    );

}

