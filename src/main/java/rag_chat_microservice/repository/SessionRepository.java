package rag_chat_microservice.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rag_chat_microservice.model.ChatSession;

@Repository
public interface SessionRepository extends JpaRepository<ChatSession, UUID> {

    List<ChatSession> findByUserIdAndDeletedFalse(String userId);

    List<ChatSession> findByUserIdAndFavoriteTrueAndDeletedFalse(String userId);

    Optional<ChatSession> findByUserIdAndTitleAndDeletedFalse(String userId, String title);
    boolean existsByUserIdAndTitleAndDeletedFalse(String userId, String title);
    boolean existsByUserId(String userId);

}

