package com.bookstore.service;

import com.bookstore.model.Book;
import com.bookstore.repository.BookRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookImportService {

    @Autowired
    private BookRepository bookRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public int importBooksFromCsv(MultipartFile file, String sellerId) throws Exception {
        int count = 0;
        try (CSVReader reader = new CSVReaderBuilder(new InputStreamReader(file.getInputStream())).build()) {
            String[] header = reader.readNext();
            if (header == null) return 0;

            // Print headers for debugging
            System.out.println(">>> BookImportService: Headers found: " + String.join(", ", header));

            // Find column indices
            int isbnIndex = -1;
            int priceIndex = -1;
            int stockIndex = -1;
            int titleIndex = -1;
            int authorIndex = -1;
            int categoryIndex = -1;
            int descriptionIndex = -1;
            int imageIndex = -1;
            
            for (int i = 0; i < header.length; i++) {
                String h = header[i].toLowerCase().replaceAll("[^a-z0-9]", ""); // Clean header
                if (h.equals("isbn13")) {
                    isbnIndex = i; // Prefer ISBN13
                } else if (h.contains("isbn") && isbnIndex == -1) {
                    isbnIndex = i; // Fallback to any ISBN column
                } else if (h.equals("price") || h.contains("amount")) {
                    priceIndex = i;
                } else if (h.equals("stock") || h.contains("count") || h.contains("quantity")) {
                    stockIndex = i;
                } else if (h.equals("title") || h.contains("booktitle") || h.equals("originaltitle")) {
                    titleIndex = i;
                } else if (h.contains("author")) {
                    authorIndex = i;
                } else if (h.contains("category") || h.contains("genre") || h.contains("subject")) {
                    categoryIndex = i;
                } else if (h.contains("description") || h.contains("summary") || h.contains("synopsis")) {
                    descriptionIndex = i;
                } else if (h.contains("image") || h.contains("cover") || h.contains("thumbnail") || h.contains("picture")) {
                    imageIndex = i;
                }
            }

            if (isbnIndex == -1 && titleIndex == -1) {
                System.err.println(">>> BookImportService: No 'isbn' or 'title' column found!");
                throw new Exception("CSV file must contain an 'isbn' or 'title' column. Found columns: " + String.join(", ", header));
            }

            System.out.println(">>> BookImportService: Found indices - ISBN:" + isbnIndex + " Title:" + titleIndex);

            String[] line;
            int rowNum = 0;
            while ((line = reader.readNext()) != null) {
                rowNum++;
                if (line.length <= isbnIndex) {
                    System.err.println(">>> BookImportService: Row " + rowNum + " is too short (" + line.length + " columns)");
                    continue;
                }
                
                String rawIsbn = line[isbnIndex].trim();
                String isbn = rawIsbn;
                
                // Handle scientific notation (e.g. 9.78E+12)
                if (isbn.contains("E+") || isbn.contains("e+")) {
                    try {
                        isbn = new java.math.BigDecimal(isbn).toPlainString();
                        System.out.println(">>> BookImportService: Converted Sci-Notation " + rawIsbn + " to " + isbn);
                    } catch (Exception e) {}
                }
                
                isbn = isbn.replaceAll("[^0-9X]", ""); // Clean ISBN
                
                if (isbn.isEmpty()) {
                    System.err.println(">>> BookImportService: Row " + rowNum + " has empty ISBN (Raw: " + rawIsbn + ")");
                    continue;
                }

                try {
                    System.out.println(">>> BookImportService: Row " + rowNum + " | Processing book: " + rawIsbn);
                    
                    Book book = new Book();
                    book.setIsbn(isbn);
                    book.setSellerId(sellerId);
                    
                    boolean useGoogleApi = true;
                    
                    if (titleIndex != -1 && line.length > titleIndex && !line[titleIndex].trim().isEmpty()) {
                        book.setTitle(line[titleIndex].trim());
                        book.setAuthor(authorIndex != -1 && line.length > authorIndex ? line[authorIndex].trim() : "Unknown Author");
                        book.setCategory(categoryIndex != -1 && line.length > categoryIndex ? line[categoryIndex].trim() : "General");
                        book.setDescription(descriptionIndex != -1 && line.length > descriptionIndex ? line[descriptionIndex].trim() : "No description available.");
                        
                        String imageUrl = imageIndex != -1 && line.length > imageIndex ? line[imageIndex].trim() : "";
                        if (imageUrl.isEmpty() || !imageUrl.startsWith("http")) {
                            imageUrl = "https://placehold.co/300x400/e2e8f0/64748b?text=" + book.getTitle().replace(" ", "+");
                        }
                        book.setImageUrl(imageUrl);
                        
                        useGoogleApi = false; // We have data from CSV, skip Google API
                    }

                    if (useGoogleApi) {
                        Book fetched = fetchBookDetailsFromGoogle(isbn);
                        if (fetched != null) {
                            book.setTitle(fetched.getTitle());
                            book.setAuthor(fetched.getAuthor());
                            book.setCategory(fetched.getCategory());
                            book.setDescription(fetched.getDescription());
                            book.setImageUrl(fetched.getImageUrl().isEmpty() ? "https://placehold.co/300x400/e2e8f0/64748b?text=No+Cover" : fetched.getImageUrl());
                            Thread.sleep(500); // Respect rate limits on success
                        } else {
                            System.err.println(">>> BookImportService: No details found for ISBN: " + isbn);
                            Thread.sleep(500); // Sleep even on failure to avoid stampeding the rate limit
                            continue;
                        }
                    }

                    // Set price from CSV if available, else default
                    if (priceIndex != -1 && line.length > priceIndex) {
                        try {
                            book.setPrice(Double.parseDouble(line[priceIndex].replaceAll("[^0-9.]", "")));
                        } catch (Exception e) { book.setPrice(19.99); }
                    } else {
                        book.setPrice(19.99);
                    }

                    // Set stock from CSV if available, else default
                    if (stockIndex != -1 && line.length > stockIndex) {
                        try {
                            book.setStock(Integer.parseInt(line[stockIndex].replaceAll("[^0-9]", "")));
                        } catch (Exception e) { book.setStock(10); }
                    } else {
                        book.setStock(10);
                    }
                    
                    bookRepository.save(book);
                    count++;
                    System.out.println(">>> BookImportService: Successfully imported: " + book.getTitle());

                } catch (Exception e) {
                    System.err.println(">>> BookImportService: Error processing row: " + e.getMessage());
                }
            }
        }
        return count;
    }

    @org.springframework.beans.factory.annotation.Value("${google.books.api.key:}")
    private String googleApiKey;

    private Book fetchBookDetailsFromGoogle(String isbn) {
        String url = "https://www.googleapis.com/books/v1/volumes?q=isbn:" + isbn;
        if (googleApiKey != null && !googleApiKey.isEmpty()) {
            url += "&key=" + googleApiKey;
        }

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            
            if (root.has("error")) {
                System.err.println(">>> BookImportService: Google API Error: " + root.get("error").get("message").asText());
                return null;
            }

            if (root.has("items") && root.get("items").isArray() && root.get("items").size() > 0) {
                JsonNode volumeInfo = root.get("items").get(0).get("volumeInfo");
                
                Book book = new Book();
                book.setTitle(volumeInfo.path("title").asText("Unknown Title"));
                
                // Author
                if (volumeInfo.has("authors") && volumeInfo.get("authors").isArray()) {
                    book.setAuthor(volumeInfo.get("authors").get(0).asText());
                } else {
                    book.setAuthor("Unknown Author");
                }
                
                book.setDescription(volumeInfo.path("description").asText("No description available."));
                
                // Category
                if (volumeInfo.has("categories") && volumeInfo.get("categories").isArray()) {
                    book.setCategory(volumeInfo.get("categories").get(0).asText());
                } else {
                    book.setCategory("General");
                }
                
                // Image
                if (volumeInfo.has("imageLinks")) {
                    book.setImageUrl(volumeInfo.get("imageLinks").path("thumbnail").asText("").replace("http://", "https://"));
                }
                
                return book;
            }
        } catch (Exception e) {
            System.err.println(">>> BookImportService: Google Books API connection error: " + e.getMessage());
            if (e.getMessage().contains("429")) {
                System.err.println(">>> BookImportService: RATE LIMIT EXCEEDED. Please add an API Key.");
            }
        }
        return null;
    }
}
