package com.example.mindease.controller;

import com.example.mindease.service.JiraService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JiraController {

    private final JiraService jiraService;

    public JiraController(JiraService jiraService) {
        this.jiraService = jiraService;
    }

    @GetMapping("/jira/issues")
    public ResponseEntity<?> getJiraIssues(@RequestHeader("Authorization") String token) {
        try {
            // Call JiraService to handle the logic
            String jiraResponse = jiraService.fetchJiraIssues(token);
            return ResponseEntity.ok(jiraResponse);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }
}
