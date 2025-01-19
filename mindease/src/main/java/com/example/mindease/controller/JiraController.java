package com.example.mindease.controller;

import com.example.mindease.service.AuthService;
import com.example.mindease.service.GuidanceService;
import com.example.mindease.service.JiraService;
import com.example.mindease.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class JiraController {

    private final JiraService jiraService;
    private final JwtUtil jwtUtil;

    public JiraController(JiraService jiraService, JwtUtil jwtUtil) {
        this.jiraService = jiraService;
        this.jwtUtil = jwtUtil;

    }

    @GetMapping("/jira/tasks")
    public ResponseEntity<?> getCategorizedTasks(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            if(!jwtUtil.validateToken(token))
                throw new RuntimeException("Invalid API token");

            Map<String, List<Map<String, Object>>> tasks = jiraService.fetchAndCategorizeTasksWithEstimates(token);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @Autowired
    private GuidanceService guidanceService;

    @PostMapping("/guidance")
    public ResponseEntity<Map<String, Object>> getTaskGuidance(@RequestBody Map<String, String> taskDetails) {
        String summary = taskDetails.get("summary");
        String description = taskDetails.get("description");

        if (summary == null || description == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Summary and description are required."));
        }

        Map<String, Object> guidance = guidanceService.getGuidance(summary, description);
        return ResponseEntity.ok(guidance);
    }

}
