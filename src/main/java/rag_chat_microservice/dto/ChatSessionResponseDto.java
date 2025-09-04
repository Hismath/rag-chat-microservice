package rag_chat_microservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rag_chat_microservice.model.ChatSession;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionResponseDto {
    private UUID sessionId;
    private String userId;
    private String title;
    private boolean favorite;
    private LocalDateTime createdAt;

    public static ChatSessionResponseDto fromEntity(ChatSession session) {
        return ChatSessionResponseDto.builder()
                .sessionId(session.getId())
                .userId(session.getUserId())
                .title(session.getTitle())
                .favorite(session.isFavorite())
                .createdAt(session.getCreatedAt())
                .build();
    }
}
