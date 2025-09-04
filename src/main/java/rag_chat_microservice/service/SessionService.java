package rag_chat_microservice.service;

import rag_chat_microservice.dto.ChatSessionDto;
import rag_chat_microservice.dto.CreateSessionRequest;
import rag_chat_microservice.model.ChatSession;

import java.util.List;
import java.util.UUID;

public interface SessionService {

	ChatSession createSession(CreateSessionRequest request);

	ChatSession updateSession(UUID sessionId, ChatSessionDto updateDto);

	ChatSession renameSession(UUID sessionId, String newTitle);

	ChatSession markFavorite(UUID sessionId, boolean favorite);

	void deleteSession(UUID sessionId);

	List<ChatSession> getUserSessions(String userId);

	List<ChatSession> getFavoriteSessions(String userId);

	ChatSession getSession(UUID sessionId);
}
