package com.bookstore.service;

import com.bookstore.model.Book;
import com.bookstore.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiService {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${groq.api.key}")
    private String apiKey;

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

    public String getChatResponse(String userMessage) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("${")) {
            return "I'm currently resting. Please set my API key in the configuration to wake me up!";
        }

        try {
            // Fetch recent books for context
            List<Book> books = bookRepository.findTop20ByOrderByIdDesc();
            String bookContext = books.stream()
                    .map(b -> String.format("- %s by %s (%s)", 
                        b.getTitle(), b.getAuthor(), b.getCategory()))
                    .collect(Collectors.joining("\n"));

            String systemInstruction = "You are 'Leafy', a friendly AI assistant for LeafyBooks bookstore. " +
                    "Your goal is to help users find books they will love. " +
                    "Be conversational, enthusiastic about reading, and helpful. " +
                    "Here is a list of some books available in our store:\n" + bookContext + "\n\n" +
                    "If a user asks for a recommendation, prioritize these books if they fit. " +
                    "If they don't fit, you can recommend other famous books but mention they might not be in stock.";

            // Prepare Groq request (OpenAI format)
            Map<String, Object> requestBody = Map.of(
                "model", "llama-3.1-8b-instant",
                "messages", List.of(
                    Map.of("role", "system", "content", systemInstruction),
                    Map.of("role", "user", "content", userMessage)
                ),
                "temperature", 0.7
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey.trim());
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(GROQ_API_URL, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List choices = (List) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map firstChoice = (Map) choices.get(0);
                    Map message = (Map) firstChoice.get("message");
                    return (String) message.get("content");
                }
            }
            return "I'm sorry, I'm having trouble thinking right now. Status: " + response.getStatusCode();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("Groq API Error: " + e.getResponseBodyAsString());
            return "Oops! I hit a snag while thinking. Please try again in a moment.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Oops! My brain is a bit fuzzy. Please try again soon.";
        }
    }
}
