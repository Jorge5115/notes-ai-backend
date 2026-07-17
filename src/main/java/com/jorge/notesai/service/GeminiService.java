package com.jorge.notesai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class GeminiService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    public GeminiService() {
        this.restClient = RestClient.create("https://generativelanguage.googleapis.com");
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
                "contents", new Object[]{
                        Map.of("parts", new Object[]{Map.of("text", prompt)})
                },
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "maxOutputTokens", 200
                )
        );

        String responseJson = restClient.post()
                .uri("/v1beta/models/{model}:generateContent", model)
                .header("x-goog-api-key", apiKey)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        return extractText(responseJson);
    }

    private String extractText(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            return root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText().trim();
        } catch (Exception e) {
            return "No se pudo generar el resumen (respuesta inesperada de la IA).";
        }
    }

    private String mockSummary(String content) {
        String preview = content.length() > 60 ? content.substring(0, 60) + "..." : content;
        return "[Resumen simulado - modo mock] " + preview;
    }
}