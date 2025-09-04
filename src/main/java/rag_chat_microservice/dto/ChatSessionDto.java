package rag_chat_microservice.dto;

import lombok.Builder;
import lombok.Data;
import rag_chat_microservice.model.ChatSession;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ChatSessionDto {
    private UUID id;
    private String userId;
    private String title;
    private Boolean favorite;
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 👇 static factory to map Entity → DTO
    public static ChatSessionDto from(ChatSession entity) {
        return ChatSessionDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .title(entity.getTitle())
                .favorite(entity.isFavorite())
                .deleted(entity.isDeleted())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
