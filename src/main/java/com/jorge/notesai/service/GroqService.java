package com.jorge.notesai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class GroqService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${groq.api-key}")
    private String apiKey;

    @Value("${groq.model}")
    private String model;

    public GroqService() {
        this.restClient = RestClient.create("https://api.groq.com");
    }

    private boolean isMockMode() {
        return apiKey == null || apiKey.isBlank();
    }

    /**
     * Resume el contenido de una nota en 1-2 frases.
     * En modo mock (sin API key, típico en CI o desarrollo sin configurar) devuelve
     * un resumen simulado en vez de llamar a la IA real.
     */
    public String summarize(String content) {
        if (isMockMode()) {
            return mockSummary(content);
        }

        String prompt = "Resume el siguiente texto en una o dos frases, en español, "
                + "sin añadir comentarios ni introducciones, solo el resumen directo:\n\n" + content;

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.2,
                "max_tokens", 200
        );

        String responseJson = restClient.post()
                .uri("/openai/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        return extractText(responseJson);
    }

    private String extractText(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            return root.path("choices").get(0)
                    .path("message").path("content")
                    .asText().trim();
        } catch (Exception e) {
            return "No se pudo generar el resumen (respuesta inesperada de la IA).";
        }
    }

    private String mockSummary(String content) {
        String preview = content.length() > 60 ? content.substring(0, 60) + "..." : content;
        return "[Resumen simulado - modo mock] " + preview;
    }
}