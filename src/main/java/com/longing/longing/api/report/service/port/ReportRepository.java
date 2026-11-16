package com.longing.longing.api.report.service.port;

import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.report.ReportReason;
import com.longing.longing.api.report.domain.PostReport;
import com.longing.longing.api.user.domain.User;

public interface ReportRepository {
    PostReport create(Post post, User user, PostReport postReport);

    /** 같은 신고자·게시글·사유의 신고가 있는지. 존재 여부 쿼리라 중복 행이 남아 있어도 깨지지 않는다 (#126). */
    boolean exists(long reporterId, long postId, ReportReason reportReason);

    /**
     * 게시글을 해당 사유로 신고한 서로 다른 사용자 수. 게시글 행을 잠근 트랜잭션 안에서 부른다.
     * 운영 DB 정리 SQL을 실행하기 전 남아 있는 중복 신고 행도 한 사람으로 센다 (#126).
     */
    long countReporters(long postId, ReportReason reportReason);
}
