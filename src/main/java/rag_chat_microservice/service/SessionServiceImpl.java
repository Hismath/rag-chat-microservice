package rag_chat_microservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import rag_chat_microservice.dto.ChatSessionDto;
import rag_chat_microservice.dto.CreateSessionRequest;
import rag_chat_microservice.exception.SessionNotFoundException;
import rag_chat_microservice.model.ChatSession;
import rag_chat_microservice.repository.MessageRepository;
import rag_chat_microservice.repository.SessionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;
    private final MessageRepository messageRepository;

    @Override
    public ChatSession createSession(CreateSessionRequest request) {
        final String userId = request.getUserId();
        final String title  = normalize(request.getTitle());

        log.info("Creating new session for user: {} with title: {}", userId, title);

        // Idempotent create: return existing active session if present
        return sessionRepository.findByUserIdAndTitleAndDeletedFalse(userId, title)
                .orElseGet(() -> {
                    ChatSession session = ChatSession.builder()
                            .userId(userId)
                            .title(title)
                            .favorite(false)
                            .deleted(false)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    try {
                        ChatSession saved = sessionRepository.save(session);
                        log.info("Session created with ID: {}", saved.getId());
                        return saved;
                    } catch (DataIntegrityViolationException e) {
                        // Race safety: DB unique constraint on (user_id, title, is_deleted)
                        log.warn("Unique constraint hit on create for userId={}, title='{}'", userId, title, e);
                        throw conflict("Session already exists for this user and title");
                    }
                });
    }

    @Override
    @Transactional(readOnly = true)
    public ChatSession getSession(UUID sessionId) {
        log.debug("Attempting to retrieve session with ID: {}", sessionId);
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> {
                    log.error("Session not found with id: {}", sessionId);
                    return new SessionNotFoundException("Session not found with id: " + sessionId);
                });
    }

    @Override
    public ChatSession updateSession(UUID sessionId, ChatSessionDto updateDto) {
        log.info("Updating session with ID: {}", sessionId);
        ChatSession session = getSession(sessionId);

        if (updateDto.getTitle() != null) {
            final String newTitle = normalize(updateDto.getTitle());
            if (!newTitle.equals(session.getTitle()) &&
                sessionRepository.existsByUserIdAndTitleAndDeletedFalse(session.getUserId(), newTitle)) {
                log.warn("Duplicate rename blocked for userId={}, title='{}'", session.getUserId(), newTitle);
                throw conflict("Another session with the same title already exists for this user");
            }
            session.setTitle(newTitle);
        }

        if (updateDto.getFavorite() != null) {
            session.setFavorite(updateDto.getFavorite());
        }

        session.setUpdatedAt(LocalDateTime.now());

        try {
            ChatSession saved = sessionRepository.save(session);
            log.info("Session {} updated", sessionId);
            return saved;
        } catch (DataIntegrityViolationException e) {
            log.warn("Unique constraint hit on update for session={}, userId={}, title='{}'",
                    sessionId, session.getUserId(), session.getTitle(), e);
            throw conflict("Another session with the same title already exists for this user");
        }
    }

    /*@Override
    public void deleteSession(UUID sessionId) {
        log.warn("Soft deleting session with ID: {}", sessionId);
        ChatSession session = getSession(sessionId);
        session.setDeleted(true);
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);

        // Soft delete all messages under this session
        messageRepository.softDeleteBySessionId(sessionId);

        log.info("Session with ID: {} and its messages successfully soft deleted.", sessionId);
    }*/
    @Override
    public void deleteSession(UUID sessionId) {
        // Fetch session even if already soft deleted
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (Boolean.TRUE.equals(session.getDeleted())) {
            log.info("Session {} is already deleted.", sessionId);
            return;
        }

        log.warn("Soft deleting session with ID: {}", sessionId);
        session.setDeleted(true);
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);

        // Soft delete all messages under this session
        messageRepository.softDeleteBySessionId(sessionId);

        log.info("Session with ID: {} and its messages successfully soft deleted.", sessionId);
    }



    @Override
    @Transactional(readOnly = true)
    public List<ChatSession> getUserSessions(String userId) {
        log.info("Retrieving all active sessions for user: {}", userId);

        // Check if user exists in session history
        boolean userExists = sessionRepository.existsByUserId(userId);
        if (!userExists) {
            log.warn("User {} not found", userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId);
        }

        // Get non-deleted sessions
        List<ChatSession> sessions = sessionRepository.findByUserIdAndDeletedFalse(userId);
        if (sessions.isEmpty()) {
            log.warn("No sessions found for user {}", userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No active sessions found for user: " + userId);
        }

        return sessions;
    }


    @Override
    @Transactional(readOnly = true)
    public List<ChatSession> getFavoriteSessions(String userId) {
        log.info("Retrieving favorite sessions for user: {}", userId);

        // Check if userId exists at all (in any session)
        boolean userExists = sessionRepository.existsByUserId(userId);
        if (!userExists) {
            log.warn("User {} not found", userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId);
        }

        // Then check favorites
        List<ChatSession> favorites = sessionRepository.findByUserIdAndFavoriteTrueAndDeletedFalse(userId);
        if (favorites.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No favorite sessions found for user: " + userId);
        }

        return favorites;
    }



    @Override
    public ChatSession renameSession(UUID sessionId, String newTitle) {
        log.info("Renaming session with ID: {} to: {}", sessionId, newTitle);
        ChatSession session = getSession(sessionId);

        final String normalized = normalize(newTitle);
        if (!normalized.equals(session.getTitle()) &&
            sessionRepository.existsByUserIdAndTitleAndDeletedFalse(session.getUserId(), normalized)) {
            log.warn("Duplicate rename blocked for userId={}, title='{}'", session.getUserId(), normalized);
            throw conflict("Another session with the same title already exists for this user");
        }

        session.setTitle(normalized);
        session.setUpdatedAt(LocalDateTime.now());

        try {
            ChatSession updated = sessionRepository.save(session);
            log.info("Session with ID: {} renamed successfully.", sessionId);
            return updated;
        } catch (DataIntegrityViolationException e) {
            log.warn("Unique constraint hit on rename for session={}, userId={}, title='{}'",
                    sessionId, session.getUserId(), normalized, e);
            throw conflict("Another session with the same title already exists for this user");
        }
    }

    @Override
    public ChatSession markFavorite(UUID sessionId, boolean favorite) {
        log.info("Marking session with ID: {} as favorite: {}", sessionId, favorite);
        ChatSession session = getSession(sessionId);
        session.setFavorite(favorite);
        session.setUpdatedAt(LocalDateTime.now());
        ChatSession updatedSession = sessionRepository.save(session);
        log.info("Session with ID: {} favorite status updated to {}.", sessionId, favorite);
        return updatedSession;
    }

    // --- helpers ---

    private String normalize(String s) {
        if (s == null) return null;
        // trim + collapse internal whitespace
        String t = s.trim().replaceAll("\\s+", " ");
        // If you need case-insensitive uniqueness regardless of DB collation, uncomment:
        // t = t.toLowerCase(Locale.ROOT);
        return t;
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
