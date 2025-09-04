package rag_chat_microservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import rag_chat_microservice.model.ChatMessage;
import rag_chat_microservice.model.ChatMessage.Sender;
import rag_chat_microservice.model.ChatSession;
import java.util.UUID;

public interface MessageService {

	ChatMessage addMessage(UUID sessionId, Sender sender, String content, String context);

	ChatMessage getAIResponse(UUID sessionId, String userMessage);

	Page<ChatMessage> getMessagesBySession(ChatSession session, Pageable pageable);

	ChatMessage getMessageById(UUID messageId);

	ChatMessage updateMessage(UUID sessionId, UUID messageId, String newContent);

	void deleteMessage(UUID sessionId, UUID messageId);
}
