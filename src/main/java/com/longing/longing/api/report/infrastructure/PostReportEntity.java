package com.longing.longing.api.report.infrastructure;

import com.longing.longing.api.post.infrastructure.PostEntity;
import com.longing.longing.api.report.ReportReason;
import com.longing.longing.api.user.infrastructure.UserEntity;
import lombok.*;

import javax.persistence.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "post_reports", uniqueConstraints = @UniqueConstraint(
        name = PostReportEntity.REPORTER_POST_REASON_UNIQUE_CONSTRAINT,
        columnNames = {"reporter_id", "post_id", "report_reason"}))
public class PostReportEntity {
    /** 운영 SQL(scripts/sql/126-post-report-unique.sql)과 같은 이름이어야 한다 (#126). */
    public static final String REPORTER_POST_REASON_UNIQUE_CONSTRAINT = "uk_post_reports_reporter_post_reason";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private PostEntity post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private UserEntity reporter;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_reason", nullable = false)
    private ReportReason reportReason;

//    protected PostReportEntity() {
//        // 기본 생성자는 Hibernate가 내부적으로 사용
//    }

    public static PostReportEntity fromModel(
            PostEntity postEntity,
            UserEntity userEntity,
            ReportReason reportReason
    ) {
        return PostReportEntity.builder()
                .post(postEntity)
                .reporter(userEntity)
                .reportReason(reportReason)
                .build();
    }
}
