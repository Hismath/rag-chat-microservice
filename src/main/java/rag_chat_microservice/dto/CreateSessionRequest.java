package rag_chat_microservice.dto;

import lombok.Data;

@Data
public class CreateSessionRequest {
    private String title;
    private String userId;
}
