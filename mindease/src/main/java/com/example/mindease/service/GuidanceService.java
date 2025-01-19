package com.example.mindease.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Service
public class GuidanceService {
    @Value("${gemini.api-key}")
    private String geminiApiKey;
    private final WebClient webClient;
    private ObjectMapper objectMapper;

    public GuidanceService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://generativelanguage.googleapis.com/v1beta/").build();
        this.objectMapper = new ObjectMapper();
    }

    public Map<String, Object> getGuidance(String summary, String description) {
        // Build the prompt for the generative AI
        String prompt = String.format(
                "Task Summary: %s\nDescription: %s\n\nProvide guidance, steps, and suggestions to complete this task in short.",
                summary, description
        );

        try {
            // Call the generative AI API (e.g., Gemini)
            String response = webClient.post()
                    .uri(uriBuilder -> uriBuilder.path("models/gemini-1.5-flash:generateContent")
                            .queryParam("key", geminiApiKey)
                            .build())
                    .header("Content-Type", "application/json")
                    .bodyValue("{\"contents\": [{\"parts\": [{\"text\": \"" + prompt + "\"}]}]}")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // Parse and return the guidance
            Map<String, Object> result = new HashMap<>();
            result.put("summary", summary);
            result.put("description", description);
            result.put("guidance", extractGuidanceText(response));
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "Failed to retrieve guidance: " + e.getMessage());
        }
    }
    private String extractGuidanceText(String jsonResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode contentsNode = rootNode.at("/candidates/0/content/parts/0/text");
            return contentsNode.asText("No guidance available");
        } catch (Exception e) {
            e.printStackTrace();
            return "Error parsing guidance response";
        }
    }
}
