package rag_chat_microservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rag_chat_microservice.model.ChatSession;

import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor 
@Builder
@Data
public class ChatSessionDto {
    private UUID id;
    private String userId;
    private String title;
    private Boolean favorite;
    private Boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ChatSessionDto from(ChatSession entity) {
        return ChatSessionDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .title(entity.getTitle())
                .favorite(entity.isFavorite())
                .deleted(entity.getDeleted())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

