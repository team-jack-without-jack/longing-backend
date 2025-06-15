package com.longing.longing.api.report.infrastructure;

import com.longing.longing.api.post.infrastructure.PostEntity;
import com.longing.longing.api.report.ReportReason;
import com.longing.longing.api.report.domain.PostReport;
import com.longing.longing.api.user.infrastructure.UserEntity;
import lombok.*;

import javax.persistence.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "post_reports")
public class PostReportEntity {
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
    @Column(nullable = false)
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

    public PostReport toModel() {
        return PostReport.builder()
                .id(id)
                .post(post.toModel())
                .reporter(reporter.toModel())
                .reportReason(reportReason)
                .build();
    }
}
