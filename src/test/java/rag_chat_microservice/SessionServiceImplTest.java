package rag_chat_microservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rag_chat_microservice.dto.CreateSessionRequest;
import rag_chat_microservice.model.ChatSession;
import rag_chat_microservice.repository.SessionRepository;
import rag_chat_microservice.service.SessionServiceImpl;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    @InjectMocks
    private SessionServiceImpl sessionService;

    @Mock
    private SessionRepository sessionRepository;

    private CreateSessionRequest createRequest;
    private ChatSession testSession;
    private final UUID testSessionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        createRequest = new CreateSessionRequest();
        createRequest.setUserId("test-user-123");
        createRequest.setTitle("Test Session");

        testSession = ChatSession.builder()
                .id(testSessionId)
                .userId("test-user-123")
                .title("Test Session")
                .build();
    }

    @Test
    void createSession_shouldSaveAndReturnNewSession() {
        // Given: The repository is configured to return our mocked session when its 'save' method is called.
        when(sessionRepository.save(any(ChatSession.class))).thenReturn(testSession);

        // When: We call the method on our service that we want to test.
        ChatSession createdSession = sessionService.createSession(createRequest);

        // Then: We verify that the repository's 'save' method was called exactly once with any ChatSession object.
        verify(sessionRepository, times(1)).save(any(ChatSession.class));

        // And: We assert that the returned session has the correct ID and user.
        assertNotNull(createdSession.getId());
        assertEquals(createRequest.getUserId(), createdSession.getUserId());
    }

    @Test
    void getSession_shouldReturnExistingSession() {
        // Given: The repository is configured to return an optional containing our session when 'findById' is called with the correct ID.
        when(sessionRepository.findById(testSessionId)).thenReturn(Optional.of(testSession));

        // When: We call the method on our service.
        ChatSession foundSession = sessionService.getSession(testSessionId);

        // Then: We verify that the repository's 'findById' method was called once with the correct ID.
        verify(sessionRepository, times(1)).findById(testSessionId);

        // And: We assert that the returned session is the one we expected.
        assertNotNull(foundSession);
        assertEquals(testSessionId, foundSession.getId());
    }
}
