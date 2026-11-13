package com.knowhub.controller;

import com.knowhub.common.ApiResponse;
import com.knowhub.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "application", "knowhub-ai");
    }

    @GetMapping("/ok")
    public ApiResponse<String> ok(){
        return ResultUtils.success("success");
    }
}
