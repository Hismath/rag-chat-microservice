package rag_chat_microservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import rag_chat_microservice.dto.AddMessageRequest;
import rag_chat_microservice.dto.ChatMessageDto;
import rag_chat_microservice.dto.UpdateMessageRequest;
import rag_chat_microservice.model.ChatMessage;
import rag_chat_microservice.model.ChatSession;
import rag_chat_microservice.service.MessageService;
import rag_chat_microservice.service.SessionService;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sessions/{sessionId}/messages")
@RequiredArgsConstructor
@Slf4j
public class ChatMessageController {

	private final MessageService messageService;
	private final SessionService sessionService;

	// Add a new message to a session and get an AI response
	@PostMapping
	public ResponseEntity<List<ChatMessageDto>> addMessage(@PathVariable UUID sessionId,
			@RequestBody AddMessageRequest request) {

		log.info("Received request to add message to session: {}", sessionId);

		// --- minimal validation & normalization ---
		if (request.getContent() == null || request.getContent().trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content must not be empty");
		}
		final String normalized = request.getContent().trim().replaceAll("\\s+", " ");
		request.setContent(normalized); // so downstream uses normalized content

		// Save the user's message (always as USER)
		messageService.addMessage(sessionId, ChatMessage.Sender.USER, request.getContent(), request.getContext());

		// Get AI response and save it
		try {
			messageService.getAIResponse(sessionId, request.getContent());
		} catch (Exception e) {
			// surface a cleaner error if upstream model times out/fails
			log.error("AI response failed for session {}", sessionId, e);
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI service failed: " + e.getMessage());
		}

		// Return the full conversation history (first page size 10)
		return ResponseEntity.status(HttpStatus.CREATED).body(getMessages(sessionId, 0, 10).getBody());
	}

	// Get all messages in a session (paginated or not)
	@GetMapping
	public ResponseEntity<List<ChatMessageDto>> getMessages(@PathVariable UUID sessionId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

		ChatSession session = sessionService.getSession(sessionId);

		Page<ChatMessage> messages = messageService.getMessagesBySession(session, PageRequest.of(page, size));

		if (messages.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No messages found for session: " + sessionId);
		}

		List<ChatMessageDto> dtoList = messages.getContent().stream() // 👈 use getContent()
				.map(this::toDto) // 👈 instance method ref
				.toList();

		return ResponseEntity.ok(dtoList);
	}

	@DeleteMapping("/{messageId}")
	public ResponseEntity<Map<String, String>> deleteMessage(@PathVariable UUID sessionId,
			@PathVariable UUID messageId) {
		messageService.deleteMessage(sessionId, messageId);
		return ResponseEntity.ok(Map.of("message", "Message " + messageId + " deleted successfully"));
	}

	@PatchMapping("/{messageId}")
	public ResponseEntity<ChatMessageDto> updateMessage(@PathVariable UUID sessionId, @PathVariable UUID messageId,
			@RequestBody UpdateMessageRequest request) {
		if (request.getContent() == null || request.getContent().trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content must not be empty");
		}
		ChatMessage updated = messageService.updateMessage(sessionId, messageId, request.getContent());
		return ResponseEntity.ok(toDto(updated));
	}

	// Convert entity to DTO
	private ChatMessageDto toDto(ChatMessage message) {
		return new ChatMessageDto(message.getId(), message.getChatSession().getId(), message.getSender().name(),
				message.getContent(), message.getContext());
	}
}
