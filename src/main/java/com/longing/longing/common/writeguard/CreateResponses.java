package com.longing.longing.common.writeguard;

import com.longing.longing.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;

public final class CreateResponses {

    public static final String REPLAYED_HEADER = "Idempotent-Replayed";

    private CreateResponses() {
    }

    /** 기존 성공 규칙(ApiResponse.created, HTTP 200)을 유지하고 재요청이면 헤더를 붙인다. */
    public static <T> ResponseEntity<ApiResponse<T>> created(CreateOutcome<T> outcome) {
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (outcome.isReplayed()) {
            response.header(REPLAYED_HEADER, "true");
        }
        return response.body(ApiResponse.created(outcome.getValue()));
    }
}
