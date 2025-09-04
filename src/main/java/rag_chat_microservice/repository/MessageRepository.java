package rag_chat_microservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import rag_chat_microservice.model.ChatMessage;
import rag_chat_microservice.model.ChatSession;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<ChatMessage, UUID> {

    // Find all messages in a session, sorted by createdAt
    List<ChatMessage> findByChatSessionOrderByCreatedAtAsc(ChatSession chatSession);

    // Paginated version
    Page<ChatMessage> findByChatSessionOrderByCreatedAtAsc(ChatSession chatSession, Pageable pageable);

 
    List<ChatMessage> findByChatSession_IdOrderByCreatedAtAsc(UUID sessionId);


    Optional<ChatMessage> findByChatSession_IdAndSenderAndContentHash(
            UUID sessionId, ChatMessage.Sender sender, String contentHash);
    
 
    @Modifying
    @Query("update ChatMessage m set m.deleted = true where m.chatSession.id = :sessionId")
    int softDeleteBySessionId(@Param("sessionId") UUID sessionId);
    
    List<ChatMessage> findByChatSession_IdAndDeletedFalseOrderByCreatedAtAsc(UUID sessionId);

    Page<ChatMessage> findByChatSessionAndDeletedFalseOrderByCreatedAtAsc(ChatSession session, Pageable pageable);
    
    Optional<ChatMessage> findByIdAndChatSession_Id(UUID messageId, UUID sessionId);
    boolean existsByIdAndChatSession_Id(UUID messageId, UUID sessionId);



}
