package com.longing.longing.api.report.service;

import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.report.ReportReason;
import com.longing.longing.api.report.domain.PostReport;
import com.longing.longing.api.report.domain.ReportCreate;
import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.Role;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.common.exceptions.AlreadyReportedPostException;
import com.longing.longing.mock.FakePostRepository;
import com.longing.longing.mock.FakeReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ReportServiceTest {

    private ReportServiceImpl reportService;
    private User user;
    private Post post;
    private FakePostRepository fakePostRepository;
    private FakeReportRepository fakeReportRepository;

    @BeforeEach
    void init() {
        fakeReportRepository = new FakeReportRepository();
        fakePostRepository = new FakePostRepository();

        this.reportService = ReportServiceImpl.builder()
                .reportRepository(fakeReportRepository)
                .postRepository(fakePostRepository)
                .build();

        user = User.builder()
                .id(1L)
                .email("test@test.com")
                .name("test_name")
                .nationality("KOREA")
                .introduction("hello world")
                .role(Role.GUEST)
                .provider(Provider.GOOGLE)
                .providerId("1")
                .picture("test_picture")
                .build();

        post = Post.builder()
                .id(1L)
                .title("test_title")
                .content("test_content")
                .user(user)
                .build();

        PostReport postReport = PostReport.builder()
                .id(1L)
                .reportReason(ReportReason.HARASSMENT)
                .reporter(user)
                .post(post)
                .build();

        fakePostRepository.save(post);
        fakeReportRepository.create(post, user, postReport);
    }

    @Test
    public void createPostReport() {
        // given
        ReportCreate reportCreate = ReportCreate.builder()
                .reportReason(ReportReason.ADVERTISEMENT)
                .build();

        // when
        PostReport postReport = reportService.createPostReport(post.getId(), reportCreate, user);

        // then
        assertThat(postReport.getId()).isEqualTo(2L);
        assertThat(postReport.getReportReason()).isEqualTo(ReportReason.ADVERTISEMENT);
    }

    @Test
    public void 이미_신고한_게시글을_신고하면_에러를_뱉는다() {
        // given
        ReportCreate reportCreate = ReportCreate.builder()
                .reportReason(ReportReason.HARASSMENT)
                .build();
        // when

        // then
        assertThatThrownBy(() -> {
            reportService.createPostReport(post.getId(), reportCreate, user);
        }).isInstanceOf(AlreadyReportedPostException.class);
    }

    @Test
    public void 같은_사유_신고가_3건이_되면_게시글을_삭제한다() {
        ReportCreate harassment = ReportCreate.builder().reportReason(ReportReason.HARASSMENT).build();

        reportService.createPostReport(post.getId(), harassment, reporter(2L));
        assertThat(fakePostRepository.findById(post.getId(), user.getId()).isPresent()).isTrue();

        reportService.createPostReport(post.getId(), harassment, reporter(3L));

        assertThat(fakePostRepository.findById(post.getId(), user.getId()).isPresent()).isFalse();
    }

    @Test
    public void 다른_사유의_신고는_따로_센다() {
        reportService.createPostReport(post.getId(),
                ReportCreate.builder().reportReason(ReportReason.ADVERTISEMENT).build(), reporter(2L));
        reportService.createPostReport(post.getId(),
                ReportCreate.builder().reportReason(ReportReason.OTHERS).build(), reporter(3L));

        assertThat(fakePostRepository.findById(post.getId(), user.getId()).isPresent()).isTrue();
    }

    @Test
    public void 중복_신고_행은_자동_삭제_기준에_한_번만_센다() {
        // 운영 DB 정리 전, 사용자 1의 같은 사유 신고가 중복으로 한 행 더 남아 있는 상황
        fakeReportRepository.create(post, user, PostReport.builder()
                .reportReason(ReportReason.HARASSMENT).reporter(user).post(post).build());

        reportService.createPostReport(post.getId(),
                ReportCreate.builder().reportReason(ReportReason.HARASSMENT).build(), reporter(2L));

        // 행은 3개지만 신고자는 2명이라 삭제되지 않는다.
        assertThat(fakePostRepository.findById(post.getId(), user.getId()).isPresent()).isTrue();
    }

    private static User reporter(long id) {
        return User.builder()
                .id(id)
                .email("reporter" + id + "@test.com")
                .name("reporter" + id)
                .role(Role.GUEST)
                .provider(Provider.GOOGLE)
                .providerId(String.valueOf(id))
                .build();
    }
}
