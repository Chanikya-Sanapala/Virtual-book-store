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

    @Value("${groq.api.key:}")
    private String apiKey;

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

    public String getChatResponse(String userMessage) {
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.startsWith("${")) {
            return "I'm currently resting. Please set GROQ_API_KEY in the server environment variables to wake me up!";
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

            String[] candidateModels = {
                "meta-llama/llama-3.3-70b-versatile",
                "llama-3.3-70b-versatile",
                "groq/compound-mini",
                "openai/gpt-oss-20b",
                "llama-3.1-8b-instant",
                "gemma2-9b-it",
                "mixtral-8x7b-32768"
            };
            String cleanApiKey = apiKey.replaceAll("\\s+", "");

            for (String modelName : candidateModels) {
                try {
                    Map<String, Object> requestBody = Map.of(
                        "model", modelName,
                        "messages", List.of(
                            Map.of("role", "system", "content", systemInstruction),
                            Map.of("role", "user", "content", userMessage)
                        ),
                        "temperature", 0.7
                    );

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.set(HttpHeaders.USER_AGENT, "LeafyBooks/1.0 (Java Spring Boot)");
                    headers.set("Authorization", "Bearer " + cleanApiKey);
                    
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
                } catch (org.springframework.web.client.HttpClientErrorException e) {
                    System.err.println("Groq Model [" + modelName + "] Error [" + e.getStatusCode() + "]: " + e.getResponseBodyAsString());
                    if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                        return "The AI assistant key is invalid or unauthorized. Please verify GROQ_API_KEY in server environment settings.";
                    }
                    // Continue to next model if not found or model error
                }
            }
            return generateSmartLocalResponse(userMessage, books);
        } catch (Exception e) {
            e.printStackTrace();
            return generateSmartLocalResponse(userMessage, null);
        }
    }

    private String generateSmartLocalResponse(String userMessage, List<Book> books) {
        if (books == null || books.isEmpty()) {
            return "Hi there! Welcome to LeafyBooks. We have a wonderful collection of stories and classics waiting for you. How can I assist your reading journey today?";
        }
        String lower = userMessage.toLowerCase();
        List<Book> matched = books.stream()
            .filter(b -> (b.getCategory() != null && lower.contains(b.getCategory().toLowerCase())) ||
                         (b.getTitle() != null && lower.contains(b.getTitle().toLowerCase())) ||
                         (b.getAuthor() != null && lower.contains(b.getAuthor().toLowerCase())))
            .collect(Collectors.toList());

        List<Book> display = !matched.isEmpty() ? matched : books.stream().limit(3).collect(Collectors.toList());
        StringBuilder sb = new StringBuilder();
        sb.append("Hi! I'm Leafy 🌿 Here are some great recommendations from our bookstore:\n\n");
        for (Book b : display) {
            sb.append("• **").append(b.getTitle()).append("** by ").append(b.getAuthor());
            if (b.getCategory() != null) {
                sb.append(" (").append(b.getCategory()).append(")");
            }
            if (b.getPrice() != null) {
                sb.append(" - $").append(b.getPrice());
            }
            sb.append("\n");
        }
        sb.append("\nFeel free to ask about any specific genre or topic!");
        return sb.toString();
    }
}
