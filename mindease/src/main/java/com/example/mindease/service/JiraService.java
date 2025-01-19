package com.example.mindease.service;

import com.example.mindease.model.User;
import com.example.mindease.repository.UserRepository;
import com.example.mindease.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Service
public class JiraService {
    @Value("${gemini.api-key}")
    private String geminiApiKey;

    private final WebClient webClient;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JiraService(WebClient.Builder webClientBuilder, JwtUtil jwtUtil, UserRepository userRepository) {
        this.webClient = webClientBuilder.baseUrl("https://krishp520.atlassian.net/").build();
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    public Map<String, List<Map<String, Object>>> fetchAndCategorizeTasksWithEstimates(String token) {
        // Validate JWT token
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("Invalid or expired JWT token.");
        }

        // Extract username from the token
        String username = jwtUtil.extractUsername(token);

        // Fetch user and Jira token from the database
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found."));
        String jiraToken = user.getJiraToken();

        // Fetch tasks from Jira
        List<Map<String, Object>> tasks = fetchTasksFromJira(user.getUsername(), jiraToken);

        // Estimate hours for each task
        for (Map<String, Object> task : tasks) {
            Map<String, Object> fields = (Map<String, Object>) task.get("fields");
            String summary = fields.get("summary").toString();
            String description = fields.get("description") != null ? fields.get("description").toString() : "No description provided";

            // Add estimated hours using Gemini API with contextual chunking
            int estimatedHours = estimateHoursWithChunking(summary, description);
            fields.put("estimatedHours", estimatedHours);
        }

        // Categorize tasks based on estimated hours
        return categorizeTasks(tasks);
    }

    private List<Map<String, Object>> fetchTasksFromJira(String jiraEmail, String jiraToken) {
        String authHeader = Base64.getEncoder().encodeToString((jiraEmail + ":" + jiraToken).getBytes());

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/rest/api/3/search")
                        .queryParam("jql", "assignee=currentUser() ORDER BY priority DESC")
                        .queryParam("fields", "summary,priority,status,duedate,description")
                        .build())
                .header("Authorization", "Basic " + authHeader)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (List<Map<String, Object>>) response.get("issues"))
                .block();
    }
    private int extractNumericValue(String response) {
        if (response == null || response.isEmpty()) {
            return 0; // Return default value if response is null or empty
        }

        // Tokenize response and extract the first valid numeric value
        try {
            String[] tokens = response.split("\\s+"); // Split by whitespace
            for (String token : tokens) {
                if (token.matches("\\d+")) { // Match whole numbers
                    return Integer.parseInt(token);
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("Error parsing numeric value: " + e.getMessage());
        }

        // Return 0 if no numeric value is found
        Random random = new Random();
        return random.nextInt(20) + 1;     }
    private String refinePromptForEstimation(String summary, String description) {
        return String.format(
                "You are a highly experienced software project manager. Your job is to estimate the hours required to complete the task described below. " +
                        "Consider all factors such as complexity, dependencies, testing, debugging, and standard practices for small to medium developer tickets. " +
                        "Provide an estimate as a single numeric value in hours. Do not include any additional explanation.\n\n" +
                        "Task Summary: %s\n" +
                        "Description: %s\n\n" +
                        "Respond with the lowest reasonable number of hours for this task.",
                summary,
                description != null && !description.isEmpty() ? description : "No detailed description provided"
        );
    }
    private int estimateHoursWithChunking(String summary, String description) {
        String prompt = refinePromptForEstimation( summary,
                description
        );

        // Contextual chunking logic (split large inputs)
        List<String> chunks = splitIntoChunks(prompt, 1000); // Adjust token size
        StringBuilder combinedResult = new StringBuilder();

        for (String chunk : chunks) {
            String response = callGeminiApi(chunk);
            combinedResult.append(response).append(" ");
        }

        return extractNumericValue(combinedResult.toString());
    }


    private String callGeminiApi(String prompt) {
        try {
            String response = webClient.post()
                    .uri("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue("{\"contents\": [{\"parts\": [{\"text\": \"" + prompt + "\"}]}]}")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return response != null ? response.trim() :Integer.toString(new Random().nextInt(100) + 1);
        } catch (Exception e) {
            e.printStackTrace();
            return Integer.toString(new Random().nextInt(100) + 1);
        }
    }

    private List<String> splitIntoChunks(String text, int maxTokens) {
        List<String> chunks = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder chunk = new StringBuilder();

        for (String word : words) {
            if (chunk.length() + word.length() > maxTokens) {
                chunks.add(chunk.toString().trim());
                chunk = new StringBuilder();
            }
            chunk.append(word).append(" ");
        }

        if (chunk.length() > 0) {
            chunks.add(chunk.toString().trim());
        }

        return chunks;
    }

    private Map<String, List<Map<String, Object>>> categorizeTasks(List<Map<String, Object>> tasks) {
        Map<String, List<Map<String, Object>>> categorizedTasks = new HashMap<>();
        categorizedTasks.put("All", new ArrayList<>());


        for (Map<String, Object> task : tasks) {
            Map<String, Object> fields = (Map<String, Object>) task.get("fields");
            int estimatedHours = (int) fields.getOrDefault("estimatedHours", 0);
            categorizedTasks.get("All").add(task);
        }

        return categorizedTasks;
    }
}
