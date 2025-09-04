package rag_chat_microservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class EnvCheckRunner implements CommandLineRunner {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${API_KEY}")
    private String apiKey;
    
    @Value("${GEMINI_API_KEY}")
    private String geminiApiKey;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("DB URL: " + dbUrl);
        System.out.println("DB Username: " + dbUsername);
        System.out.println("API_KEY: " + apiKey);
        System.out.println("GEMINI_API_KEY: " + geminiApiKey);
        
    }
}

