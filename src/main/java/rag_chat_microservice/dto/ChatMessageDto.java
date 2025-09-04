package rag_chat_microservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private UUID id;
    private UUID sessionId;
    private String sender;   // "USER" or "AI"
    private String content;
    private String context;  // Optional context
}
