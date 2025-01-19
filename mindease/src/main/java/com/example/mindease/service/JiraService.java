package com.example.mindease.service;

import com.example.mindease.model.User;
import com.example.mindease.repository.UserRepository;
import com.example.mindease.util.JwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

@Service
public class JiraService {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final WebClient webClient;

    public JiraService(JwtUtil jwtUtil, UserRepository userRepository, WebClient.Builder webClientBuilder) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.webClient = webClientBuilder.baseUrl("https://your-jira-instance.atlassian.net").build();
    }

    public String fetchJiraIssues(String token) {
         token = token.replace("Bearer ", "");

        // Validate JWT Token
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("Invalid or expired JWT token.");
        }

        // Extract username from JWT Token
        String username = jwtUtil.extractUsername(token);

        // Fetch the user and their Jira token from the database
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            throw new RuntimeException("User not found.");
        }

        String jiraToken = userOptional.get().getJiraToken();

        // Make an API call to Jira
        return webClient.get()
                .uri("/rest/api/2/issue") // Adjust the URI to your Jira endpoint
                .header("Authorization", "Bearer " + jiraToken)
                .retrieve()
                .bodyToMono(String.class)
                .block(); // Blocking call for simplicity; switch to reactive if needed
    }
}
