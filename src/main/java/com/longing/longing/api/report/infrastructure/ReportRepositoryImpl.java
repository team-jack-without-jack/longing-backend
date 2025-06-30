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

import java.util.Optional;

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
    public Optional<PostReport> findOne(User reporter, Post post, ReportReason reportReason) {
        UserEntity userEntity = UserEntity.fromModel(reporter);
        PostEntity postEntity = PostEntity.fromModel(post);
        return reportJpaRepository.findByReporterAndPostAndReportReason(userEntity, postEntity, reportReason)
                .map(PostReportEntity::toModel);
    }

    @Override
    public long countByPostAndReportReasonForUpdate(Post post, ReportReason reportReason) {
        // 도메인 Post를 엔티티로 변환
        PostEntity postEntity = PostEntity.fromModel(post);
        log.info("====================================================");
        log.info(postEntity.getId().toString());
        log.info(postEntity.getTitle());
        log.info(reportReason.toString());
        // PESSIMISTIC_WRITE 잠금으로 count 쿼리 실행
        long count = reportJpaRepository.countReportsByPostAndReason(postEntity, reportReason);
        log.info("count>> " + count);
        return count;
    }
}
