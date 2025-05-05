package com.longing.longing.utils.slack;

import com.slack.api.Slack;
import com.slack.api.model.Attachment;
import com.slack.api.model.Field;
import com.slack.api.webhook.Payload;
import com.slack.api.webhook.WebhookResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@Profile("prod")  // prod 환경에서만 이 빈이 등록된다
public class SlackUtils {

    private final Slack slackClient = Slack.getInstance();
    @Value("${slack.webhook.url}")
    private String webhookUrl;

    // attachment 생성 메서드
    private Attachment generateSlackAttachment(Exception e, HttpServletRequest request) {
        String requestTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").format(LocalDateTime.now());
        String xffHeader = request.getHeader("X-FORWARDED-FOR");  // 프록시 서버일 경우 client IP는 여기에 담길 수 있습니다.
        return Attachment.builder()
                .color("ff0000")  // 붉은 색으로 보이도록
                .title(requestTime + " 발생 에러 로그")
// Field도 List 형태로 담아주어야 합니다.
                .fields(List.of(
                                generateSlackField("Request IP", xffHeader == null ? request.getRemoteAddr() : xffHeader),
                                generateSlackField("Request URL", request.getRequestURL() + " " + request.getMethod()),
                                generateSlackField("Error Message", e.getMessage())
                        )
                )
                .build();
    }

    // Field 생성 메서드
    private Field generateSlackField(String title, String value) {
        return Field.builder()
                .title(title)
                .value(value)
                .valueShortEnough(false)
                .build();
    }


    public void sendSlackAlertErrorLog(Exception e, HttpServletRequest request) {
        // 에러 코드 조회 (Dispatcher가 설정해 주는 속성)
        Object statusObj = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int statusCode = (statusObj != null)
                ? Integer.parseInt(statusObj.toString())
                : 0;

        // 500번대 에러일 때만 알람 전송
        if (statusCode >= HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                && statusCode < 600) {
            try {
                Payload payload = Payload.builder()
                        .text("서버 에러 발생! (HTTP " + statusCode + ")")
                        .attachments(List.of(generateSlackAttachment(e, request)))
                        .build();

                slackClient.send(webhookUrl, payload);
            } catch (IOException slackError) {
                log.debug("Slack 통신 중 예외 발생", slackError);
            }
        } else {
            // 400번대 등은 무시
            log.debug("HTTP {} 에러는 슬랙 알람 대상 아님", statusCode);
        }
    }
}
