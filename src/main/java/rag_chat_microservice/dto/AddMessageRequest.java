package rag_chat_microservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddMessageRequest {
    private String sender;   // "USER" or "AI"
    private String content;  // The actual message content
    private String context;  // Optional context, can be null
}
