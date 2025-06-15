package com.longing.longing.healthCheck;

import com.longing.longing.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HealthController {

    @GetMapping("/ping")
    public ApiResponse<Boolean> ping() {
        return ApiResponse.ok(false);
    }


    @GetMapping("/ping2")
    public ApiResponse<Boolean> ping2() {
        return ApiResponse.ok(true);
    }

}
