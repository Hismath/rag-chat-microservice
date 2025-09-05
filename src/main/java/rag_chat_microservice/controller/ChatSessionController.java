package rag_chat_microservice.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import rag_chat_microservice.dto.ChatSessionDto;
import rag_chat_microservice.dto.ChatSessionResponseDto;
import rag_chat_microservice.dto.CreateSessionRequest;
import rag_chat_microservice.model.ChatSession;
import rag_chat_microservice.repository.SessionRepository;
import rag_chat_microservice.service.SessionService;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class ChatSessionController {

    private final SessionService sessionService;
    private final SessionRepository sessionRepository;

    @PostMapping("/api/sessions")
    public ResponseEntity<ChatSessionDto> create(@RequestBody CreateSessionRequest req, UriComponentsBuilder uri) {
        // --- validate input ---
        if (req.getUserId() == null || req.getUserId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId must not be empty");
        }
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title must not be empty");
        }

        String normTitle = req.getTitle().trim().replaceAll("\\s+", " ");
        boolean existed = sessionRepository.existsByUserIdAndTitleAndDeletedFalse(req.getUserId(), normTitle);

        ChatSession saved = sessionService.createSession(req);
        ChatSessionDto dto = ChatSessionDto.from(saved); // map entity → DTO

        if (existed) {
            return ResponseEntity.ok(dto); // 200
        } else {
            URI location = uri.path("/api/sessions/{id}").buildAndExpand(saved.getId()).toUri();
            return ResponseEntity.created(location).body(dto); // 201
        }
    }



    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ChatSessionResponseDto>> getUserSessions(@PathVariable String userId) {
    	if (userId == null || userId.isBlank()) {
    	    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId must not be blank");
    	}
        List<ChatSession> sessions = sessionService.getUserSessions(userId);
        List<ChatSessionResponseDto> responseDtos = sessions.stream()
                .map(ChatSessionResponseDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDtos);
    }

    @GetMapping("/user/{userId}/favorites")
    public ResponseEntity<List<ChatSessionResponseDto>> getFavoriteSessions(@PathVariable String userId) {
        List<ChatSession> favoriteSessions = sessionService.getFavoriteSessions(userId);
        List<ChatSessionResponseDto> responseDtos = favoriteSessions.stream()
                .map(ChatSessionResponseDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDtos);
    }

    @PatchMapping("/{sessionId}/rename")
    public ResponseEntity<ChatSessionResponseDto> renameSession(@PathVariable UUID sessionId, @RequestParam String newTitle) {
        ChatSession updatedSession = sessionService.renameSession(sessionId, newTitle);
        ChatSessionResponseDto responseDto = ChatSessionResponseDto.fromEntity(updatedSession);
        return ResponseEntity.ok(responseDto);
    }

    @PatchMapping("/{sessionId}/favorite")
    public ResponseEntity<ChatSessionResponseDto> markFavorite(@PathVariable UUID sessionId, @RequestParam boolean favorite) {
        ChatSession updatedSession = sessionService.markFavorite(sessionId, favorite);
        ChatSessionResponseDto responseDto = ChatSessionResponseDto.fromEntity(updatedSession);
        return ResponseEntity.ok(responseDto);
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Map<String, String>> deleteSession(@PathVariable UUID sessionId) {
        sessionService.deleteSession(sessionId);
        return ResponseEntity.ok(Map.of(
            "message", "Session " + sessionId + " deleted successfully"
        ));
    }

}
