package com.example.mindease.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class MoodService {

    @Value("${gemini.api-key}")
    private String geminiApiKey;

    private final WebClient webClient;

    public MoodService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/")
                .build();
    }

    public String getMoodMessage(String mood) {
        String prompt = String.format(
                "Provide a upto 3 sentence uplifting suggestion for someone feeling %s with some emoji motivating person reducing stress.",
                mood
        );

        try {
            // Make a POST request to the Gemini API
            String response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("models/gemini-1.5-flash:generateContent")
                            .queryParam("key", geminiApiKey)
                            .build())
                    .header("Content-Type", "application/json")
                    .bodyValue("""
                    {
                      "contents": [{
                        "parts": [{"text": "%s"}]
                      }]
                    }
                    """.formatted(prompt))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return extractSuggestion(response);
        } catch (Exception e) {
            e.printStackTrace();
            return "Could not fetch a suggestion. Please try again later.";
        }
    }

    private String extractSuggestion(String response) {
        try {
            // Extract the suggestion text from the Gemini response
            JsonNode root = new ObjectMapper().readTree(response);
            return root.at("/candidates/0/content/parts/0/text").asText("No suggestion available");
        } catch (Exception e) {
            e.printStackTrace();
            return "Error parsing the suggestion.";
        }
    }
}
