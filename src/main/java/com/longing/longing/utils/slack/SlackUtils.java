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

import javax.servlet.http.HttpServletRequest;
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
        try {
            Payload payload = Payload.builder()
                    .text("서버 에러 발생!")
                    .attachments(List.of(generateSlackAttachment(e, request)))
                    .build();

            slackClient.send(webhookUrl, payload);
        } catch (IOException slackError) {
            // slack 통신 시 발생한 예외에서 Exception을 던져준다면 재귀적인 예외가 발생합니다.
            // 따라서 로깅으로 처리하였고, 모카콩 서버 에러는 아니므로 `error` 레벨보다 낮은 레벨로 설정했습니다.
            log.debug("Slack 통신과의 예외 발생");
        }
    }
}
