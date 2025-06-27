package com.longing.longing.api.report.service;

import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.report.ReportReason;
import com.longing.longing.api.report.domain.PostReport;
import com.longing.longing.api.report.domain.ReportCreate;
import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.Role;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.common.domain.ResourceNotFoundException;
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

    @BeforeEach
    void init() {
        FakeReportRepository fakeReportRepository = new FakeReportRepository();
        FakePostRepository fakePostRepository = new FakePostRepository();

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
}
