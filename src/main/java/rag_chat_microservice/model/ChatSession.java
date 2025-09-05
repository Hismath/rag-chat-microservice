package rag_chat_microservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
  name = "chat_sessions",
  uniqueConstraints = @UniqueConstraint(
    name = "uk_chat_session_user_title_deleted",
    columnNames = {"user_id", "title", "is_deleted"}   // 👈 must match @Column names below
  )
)
public class ChatSession {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)   // 👈 make explicit
    private String userId;

    @Column(name = "title", nullable = false)     // 👈 make explicit (optional but clearer)
    private String title;

    @Version
    private Long version;

    @Column(name = "favorite")
    private boolean favorite;

    @Column(name = "is_deleted", columnDefinition = "boolean default false") // 👈 already explicit
    private Boolean deleted;

    @OneToMany(mappedBy = "chatSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> messages;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
