package com.longing.longing.healthCheck;

import com.longing.longing.common.response.ApiResponse;
import com.longing.longing.post.domain.Post;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HealthController {

    @GetMapping("/ping")
    public ApiResponse<Boolean> ping() {
        return ApiResponse.created(false);
    }


    @GetMapping("/ping2")
    public ApiResponse<Boolean> ping2() {
        return ApiResponse.ok(true);
    }

}
