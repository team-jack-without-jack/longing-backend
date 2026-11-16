package com.longing.longing.api.report.service;

import com.longing.longing.api.report.controller.port.ReportService;
import com.longing.longing.api.report.domain.ReportCreate;
import com.longing.longing.api.report.service.port.ReportRepository;
import com.longing.longing.common.exceptions.ResourceNotFoundException;
import com.longing.longing.common.exceptions.AlreadyReportedPostException;
import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.post.service.port.PostRepository;
import com.longing.longing.api.report.ReportReason;
import com.longing.longing.api.report.domain.PostReport;
import com.longing.longing.api.user.domain.User;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import javax.transaction.Transactional;

@Builder
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    private final ReportRepository reportRepository;
    private final PostRepository postRepository;

    /**
     * 같은 사유 신고가 이 수 이상이면 게시글을 자동 삭제한다(soft delete).
     * 삭제는 엔티티 삭제라 cascade로 그 게시글의 신고·좋아요·북마크 행이 하드 삭제된다(기존 동작, 후속 과제).
     */
    private static final int AUTO_DELETE_THRESHOLD = 3;

    /**
     * 게시글 신고 (#126).
     * 게시글 행을 트랜잭션의 첫 쿼리로 잠가 같은 게시글의 신고를 한 줄로 세운다. 그래서 중복 확인·저장·집계·자동 삭제가
     * 다른 신고와 섞이지 않고, 저장 뒤 서로 잠그며 집계하던 교착 상태도 생기지 않는다.
     * REPEATABLE READ의 스냅샷은 잠금 뒤 첫 일반 조회에서 만들어지므로 앞서 커밋된 신고를 모두 본다.
     * 잠금보다 앞에 다른 조회를 넣지 않는다.
     * 이미 열린 트랜잭션 안에서 호출하지 않는다. 그 트랜잭션이 앞서 조회했다면 스냅샷이 잠금보다 먼저 만들어진다.
     */
    @Override
    @Transactional
    public PostReport createPostReport(Long postId, ReportCreate reportCreate, User reporter) {
        ReportReason reportReason = reportCreate.getReportReason();

        Post post = postRepository.findByIdForUpdate(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Posts", postId));

        if (reportRepository.exists(reporter.getId(), post.getId(), reportReason)) {
            throw new AlreadyReportedPostException("you already reported this post!");
        }

        PostReport postReport = PostReport.builder()
                .post(post)
                .reporter(reporter)
                .reportReason(reportReason)
                .build();
        PostReport createdReport = reportRepository.create(post, reporter, postReport);

        if (reportRepository.countReporters(post.getId(), reportReason) >= AUTO_DELETE_THRESHOLD) {
            postRepository.deleteById(post.getId());
        }
        return createdReport;
    }
}
