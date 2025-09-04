package rag_chat_microservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import rag_chat_microservice.model.ChatMessage;
import rag_chat_microservice.model.ChatMessage.Sender;
import rag_chat_microservice.model.ChatSession;
import rag_chat_microservice.repository.MessageRepository;
import rag_chat_microservice.repository.SessionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageServiceImpl implements MessageService {

	private final MessageRepository messageRepository;
	private final SessionRepository sessionRepository;
	private final AIService aiService;

	/**
	 * Adds a message. USER messages are idempotent per (sessionId, normalized
	 * content): if the same USER content already exists in the session, returns
	 * that row instead of inserting a duplicate. AI/ASSISTANT messages are always
	 * appended.
	 */
	@Override
	public ChatMessage addMessage(UUID sessionId, Sender sender, String content, String context) {
		ChatSession chatSession = sessionRepository.findById(sessionId)
				.orElseThrow(() -> new IllegalArgumentException("Session not found with ID: " + sessionId));

		String normalized = normalize(content);
		String hash = sha256(normalized);

		if (sender == Sender.USER) {
			Optional<ChatMessage> existing = messageRepository.findByChatSession_IdAndSenderAndContentHash(sessionId,
					Sender.USER, hash);
			if (existing.isPresent()) {
				log.debug("Reusing existing USER message {} for identical content in session {}",
						existing.get().getId(), sessionId);
				return existing.get();
			}
		}

		ChatMessage msg = ChatMessage.builder().chatSession(chatSession).sender(sender).deleted(false)
				.content(normalized) // entity @PrePersist will recompute hash too; harmless
				.context(context).build();

		return messageRepository.save(msg);
	}

	/**
	 * Full RAG flow: 
	 * 1) Upsert the USER message (idempotent) 
	 * 2) Build prompt from history 
	 * 3) Call AI 
	 * 4) Save AI reply
	 */
	@Override
	public ChatMessage getAIResponse(UUID sessionId, String userMessage) {
		log.info("Generating AI response for session: {}", sessionId);

		ChatSession chatSession = sessionRepository.findById(sessionId)
				.orElseThrow(() -> new IllegalArgumentException("Session not found with ID: " + sessionId));

		// 1) Upsert USER message (prevents duplicate question rows)
		ChatMessage userMsg = addMessage(sessionId, Sender.USER, userMessage, null);

		// 2) Fetch conversation history (ordered)
		List<ChatMessage> history = messageRepository.findByChatSession_IdOrderByCreatedAtAsc(sessionId);

		// 3) Build prompt
		String prompt = history.stream().map(m -> {
			StringBuilder sb = new StringBuilder();
			sb.append(m.getSender()).append(": ").append(m.getContent());
			if (m.getContext() != null && !m.getContext().isEmpty()) {
				sb.append(" [Context: ").append(m.getContext()).append("]");
			}
			return sb.toString();
		}).collect(Collectors.joining("\n"));

		// 4) Call AI
		String aiResponse;
		try {
			log.debug("Calling AI service with prompt length {}", prompt.length());
			aiResponse = aiService.getAIResponse(prompt);
		} catch (Exception e) {
			log.error("AI service error for session {}", sessionId, e);
			aiResponse = "[AI ERROR: " + e.getMessage() + "]";
		}

		// 5) Save AI reply (always append)
		ChatMessage aiMsg = ChatMessage.builder().chatSession(chatSession).sender(Sender.AI) // or Sender.ASSISTANT if
																								// you prefer
				.content(aiResponse).build();

		ChatMessage saved = messageRepository.save(aiMsg);
		log.info("AI response saved for session: {}", sessionId);
		return saved;
	}

	@Override
	public Page<ChatMessage> getMessagesBySession(ChatSession session, Pageable pageable) {
		return messageRepository.findByChatSessionOrderByCreatedAtAsc(session, pageable);
	}

	@Override
	public ChatMessage getMessageById(UUID messageId) {
		return messageRepository.findById(messageId)
				.orElseThrow(() -> new IllegalArgumentException("Message not found with ID: " + messageId));
	}

	@Override
	public void deleteMessage(UUID sessionId, UUID messageId) {
		ChatMessage toDelete = loadInSession(sessionId, messageId);

		// If it's a USER message, delete the immediate next AI reply (your existing
		// rule)
		if (toDelete.getSender() == Sender.USER) {
			List<ChatMessage> messages = messageRepository
					.findByChatSessionOrderByCreatedAtAsc(toDelete.getChatSession());
			int idx = messages.indexOf(toDelete);
			if (idx + 1 < messages.size()) {
				ChatMessage next = messages.get(idx + 1);
				if (next.getSender() == Sender.AI) {
					messageRepository.deleteById(next.getId());
				}
			}
		}

		messageRepository.deleteById(toDelete.getId());
	}

	@Override
	@org.springframework.transaction.annotation.Transactional
	public ChatMessage updateMessage(UUID sessionId, UUID messageId, String newContent) {
		// 0) Ensure (sessionId, messageId) pair is valid
		ChatMessage message = loadInSession(sessionId, messageId); // uses repo: findByIdAndChatSession_Id

		// Only USER messages can be edited (keeps conversation semantics)
		if (message.getSender() != ChatMessage.Sender.USER) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only USER messages can be edited");
		}

		// Normalize input and short-circuit if unchanged
		String normalized = (newContent == null) ? "" : newContent.trim().replaceAll("\\s+", " ");
		if (normalized.equals(message.getContent())) {
			log.info("Content unchanged for message {}; skipping regeneration", messageId);
			return message;
		}

		// 1) Update the USER message (entity @PreUpdate can recompute hash if present)
		message.setContent(normalized);
		ChatMessage updatedUser = messageRepository.save(message);

		// 2) Remove ALL subsequent AI replies after this USER message, until next
		// USER/SYSTEM message
		List<ChatMessage> ordered = messageRepository
				.findByChatSessionOrderByCreatedAtAsc(updatedUser.getChatSession());

		int idx = ordered.indexOf(updatedUser);
		if (idx >= 0) {
			for (int i = idx + 1; i < ordered.size(); i++) {
				ChatMessage next = ordered.get(i);
				// stop sweep when the next turn starts
				if (next.getSender() == ChatMessage.Sender.USER || next.getSender() == ChatMessage.Sender.SYSTEM) {
					break;
				}
				if (next.getSender() == ChatMessage.Sender.AI) {
					// hard delete; use soft-delete if your model has a flag
					messageRepository.deleteById(next.getId());
					log.debug("Deleted stale AI reply {} after user message {}", next.getId(), messageId);
				}
			}
		}

		// 3) Rebuild prompt from current history & generate a fresh AI reply
		List<ChatMessage> history = messageRepository.findByChatSession_IdOrderByCreatedAtAsc(sessionId);

		String prompt = history.stream().map(m -> {
			StringBuilder sb = new StringBuilder();
			sb.append(m.getSender()).append(": ").append(m.getContent());
			if (m.getContext() != null && !m.getContext().isEmpty()) {
				sb.append(" [Context: ").append(m.getContext()).append("]");
			}
			return sb.toString();
		}).collect(java.util.stream.Collectors.joining("\n"));

		String aiResponse;
		try {
			aiResponse = aiService.getAIResponse(prompt);
		} catch (Exception e) {
			log.error("AI service error during regeneration for message {}", messageId, e);
			aiResponse = "[AI ERROR: " + e.getMessage() + "]";
		}

		ChatMessage newAi = ChatMessage.builder().chatSession(updatedUser.getChatSession())
				.sender(ChatMessage.Sender.AI).content(aiResponse).deleted(false).build();

		messageRepository.save(newAi);
		log.info("Regenerated AI reply after editing user message {}", messageId);

		return updatedUser;
	}

	// ---- helpers ----

	private ChatMessage loadInSession(UUID sessionId, UUID messageId) {
		return messageRepository.findByIdAndChatSession_Id(messageId, sessionId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Message " + messageId + " not found in session " + sessionId));
	}

	private String normalize(String s) {
		return (s == null) ? "" : s.trim().replaceAll("\\s+", " ");
	}

	private String sha256(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] bytes = md.digest(s.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(64);
			try (Formatter fmt = new Formatter(sb)) {
				for (byte b : bytes)
					fmt.format("%02x", b);
			}
			return sb.toString();
		} catch (Exception e) {
			throw new IllegalStateException("Failed to compute SHA-256 hash", e);
		}
	}

}
