package com.example.mindease.controller;


import com.example.mindease.service.MoodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mood")
public class MoodController {

    @Autowired
    private MoodService moodService;

    @PostMapping
    public Map<String, String> getMoodMessage(@RequestBody Map<String, String> request) {
        String mood = request.get("mood");
        String message = moodService.getMoodMessage(mood);
        return Map.of("message", message);
    }
}
