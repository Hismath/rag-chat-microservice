package rag_chat_microservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession chatSession;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender", nullable = false)
    private Sender sender; // USER / ASSISTANT / SYSTEM / AI

    @Lob
    @Column(name = "content", columnDefinition = "LONGTEXT", nullable = false)
    private String content;

    @Lob
    @Column(name = "context", columnDefinition = "LONGTEXT")
    private String context;


    // 👇 add this column and keep it NOT NULL in DB
    @Column(name = "content_hash", length = 64, nullable = false)
    private String contentHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        recomputeHash();
    }

    @PreUpdate
    void onUpdate() {
        recomputeHash();
    }

    private void recomputeHash() {
        // normalize content and compute SHA-256
        String normalized = normalize(this.content);
        this.content = normalized;
        this.contentHash = sha256(normalized);
    }

    private static String normalize(String s) {
        return (s == null) ? "" : s.trim().replaceAll("\\s+", " ");
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute content hash", e);
        }
    }
    
    @Column(name = "is_deleted", columnDefinition = "boolean default false")
    private boolean deleted;


    public enum Sender { USER, ASSISTANT, SYSTEM, AI }
}
