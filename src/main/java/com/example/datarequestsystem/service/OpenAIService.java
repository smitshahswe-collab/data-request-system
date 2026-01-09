package com.example.datarequestsystem.service;

import com.example.datarequestsystem.model.DataRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenAIService implements AIService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAIService.class);

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    private final RestTemplate restTemplate;

    public OpenAIService() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String generateRequestSummary(DataRequest request) {
        // If API key is not configured, return a default summary
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("your-api-key-here")) {
            return generateDefaultSummary(request);
        }

        try {
            String prompt = String.format(
                    "Generate a brief, professional summary (max 100 words) for this data request:\n" +
                            "Type: %s\n" +
                            "Requester ID: %s\n" +
                            "Notes: %s\n" +
                            "Summary should be human-readable and concise.",
                    request.getRequestType(),
                    request.getRequesterId(),
                    request.getNotes() != null ? request.getNotes() : "None provided"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-3.5-turbo");
            requestBody.put("messages", List.of(
                    Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("max_tokens", 150);
            requestBody.put("temperature", 0.7);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    if (message != null) {
                        return (String) message.get("content");
                    }
                }
            }

            return generateDefaultSummary(request);

        } catch (Exception e) {
            logger.error("Failed to generate AI summary: {}", e.getMessage());
            return generateDefaultSummary(request);
        }
    }

    private String generateDefaultSummary(DataRequest request) {
        String action = switch (request.getRequestType()) {
            case ACCESS -> "requesting access to their personal data";
            case DELETE -> "requesting deletion of their personal data";
            case CORRECT -> "requesting correction of their personal data";
        };

        String notesPart = (request.getNotes() != null && !request.getNotes().isEmpty())
                ? " Additional notes: " + request.getNotes()
                : "";

        return String.format("User %s is %s.%s",
                request.getRequesterId(),
                action,
                notesPart
        );
    }
}