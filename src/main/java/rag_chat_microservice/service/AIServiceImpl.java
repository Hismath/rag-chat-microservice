package rag_chat_microservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIServiceImpl implements AIService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.api.url}")
    private String apiUrl;

    @Value("${ai.api.key}")
    private String apiKey;

    /**
     * Calls the Gemini API to get a generated response based on the provided prompt.
     * This method is responsible for the "Generation" part of the RAG workflow.
     *
     * @param prompt The enriched prompt created from user's message and chat history.
     * @return The AI-generated response as a String.
     */
    @Override
    public String getAIResponse(String prompt) {
        return callGeminiApi(prompt);
    }

    private String callGeminiApi(String prompt) {
        try {
            // Build the URL with the API key as a query parameter
            String urlWithKey = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .queryParam("key", apiKey)
                    .toUriString();

            // Create the JSON payload for the API request
            String payload = String.format(
                "{"
                    + "  \"contents\": [{\"parts\": [{\"text\": \"%s\"}]}], "
                    + "  \"tools\": [{\"google_search\": {}}], "
                    + "  \"systemInstruction\": {\"parts\": [{\"text\": \"You are a helpful chat assistant.\"}]} "
                + "}", prompt.replace("\"", "\\\"").replace("\n", "\\n"));

            // Set the headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            
            // Create the HTTP entity with the payload and headers
            HttpEntity<String> entity = new HttpEntity<>(payload, headers);

            // Make the POST request to the Gemini API
            ResponseEntity<String> response = restTemplate.postForEntity(urlWithKey, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Parse the JSON response from Gemini
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode contentNode = root.path("candidates").path(0).path("content");

                if (!contentNode.isMissingNode() && contentNode.has("parts")) {
                    // Extract all text parts and join them
                    String aiText = StreamSupport.stream(contentNode.path("parts").spliterator(), false)
                            .filter(part -> part.has("text"))
                            .map(part -> part.path("text").asText())
                            .collect(Collectors.joining(" "));

                    if (aiText == null || aiText.isEmpty()) {
                        log.warn("AI service returned empty response");
                        return "[AI returned no response]";
                    }
                    return aiText.trim();
                } else {
                    log.error("AI service response missing content parts: {}", response.getBody());
                    return "[AI returned malformed response]";
                }
            } else {
                log.error("AI service error. Status: {}, Body: {}",
                        response.getStatusCode(), response.getBody());
                return "[AI service failed with status " + response.getStatusCode() + "]";
            }

        } catch (Exception e) {
            log.error("Error communicating with AI service", e);
            return "[AI ERROR: " + e.getMessage() + "]";
        }
    }
}
