package com.espacogeek.geek.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/actuator/diagnostic")
public class DiagnosticController {

    @Value("${spring.mvc.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @GetMapping("/cors")
    public ResponseEntity<Map<String, Object>> getCorsConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("allowedOrigins", allowedOrigins);
        config.put("message", "CORS configuration loaded from environment");
        return ResponseEntity.ok(config);
    }
}
