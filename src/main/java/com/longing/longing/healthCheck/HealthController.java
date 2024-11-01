package com.longing.longing.healthCheck;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HealthController {

    @GetMapping("/ping") public Boolean ping() {
        return true;
    }
}
