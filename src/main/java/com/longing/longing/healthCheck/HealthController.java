package com.longing.longing.healthCheck;

import com.longing.longing.common.response.ApiResponse;
import com.longing.longing.post.domain.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HealthController {

    @GetMapping("/ping")
    public ApiResponse<Boolean> ping() {
        return ApiResponse.ok(true);
    }
}
