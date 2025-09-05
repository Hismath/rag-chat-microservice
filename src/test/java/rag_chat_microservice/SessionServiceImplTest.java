package rag_chat_microservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rag_chat_microservice.dto.ChatSessionDto;
import rag_chat_microservice.dto.CreateSessionRequest;
import rag_chat_microservice.model.ChatSession;
import rag_chat_microservice.repository.MessageRepository;
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
    
    @Mock
    private MessageRepository messageRepository;


    private CreateSessionRequest createRequest;
    private UUID testSessionId;

    @BeforeEach
    void setUp() {
        createRequest = new CreateSessionRequest();
        createRequest.setUserId("test-user-123");
        createRequest.setTitle("Test Session");

        testSessionId = UUID.randomUUID();
    }

    @Test
    void createSession_shouldSaveAndReturnNewSession() {
        ChatSession testSession = ChatSession.builder()
                .id(testSessionId)
                .userId(createRequest.getUserId())
                .title(createRequest.getTitle())
                .favorite(false)
                .build();

        when(sessionRepository.save(any(ChatSession.class))).thenReturn(testSession);

        ChatSession createdSession = sessionService.createSession(createRequest);

        verify(sessionRepository, times(1)).save(any(ChatSession.class));
        assertAll(
                () -> assertNotNull(createdSession.getId(), "Session ID should not be null"),
                () -> assertEquals(createRequest.getUserId(), createdSession.getUserId()),
                () -> assertEquals(createRequest.getTitle(), createdSession.getTitle())
        );
    }

    @Test
    void getSession_shouldReturnExistingSession() {
        ChatSession testSession = ChatSession.builder()
                .id(testSessionId)
                .userId("test-user-123")
                .title("Test Session")
                .build();

        when(sessionRepository.findById(testSessionId)).thenReturn(Optional.of(testSession));

        ChatSession foundSession = sessionService.getSession(testSessionId);

        verify(sessionRepository, times(1)).findById(testSessionId);
        assertNotNull(foundSession);
        assertEquals(testSessionId, foundSession.getId());
    }

    @Test
    void getSession_shouldThrowExceptionWhenNotFound() {
        when(sessionRepository.findById(testSessionId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> sessionService.getSession(testSessionId));
        verify(sessionRepository, times(1)).findById(testSessionId);
    }

    @Test
    void updateSession_shouldUpdateAndReturnUpdatedSession() {
        ChatSession testSession = ChatSession.builder()
                .id(testSessionId)
                .userId("test-user-123")
                .title("Test Session")
                .favorite(false)
                .build();

        ChatSessionDto updateDto = new ChatSessionDto();
        updateDto.setTitle("Updated Session");
        updateDto.setFavorite(true);

        when(sessionRepository.findById(testSessionId)).thenReturn(Optional.of(testSession));
        when(sessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatSession updatedSession = sessionService.updateSession(testSessionId, updateDto);

        verify(sessionRepository, times(1)).save(testSession);
        assertEquals("Updated Session", updatedSession.getTitle());
        assertTrue(updatedSession.isFavorite());
    }

    @Test
    void softDeleteSession_shouldMarkSessionAsDeleted() {
        // Mock repository to always return a session
        when(sessionRepository.findById(any(UUID.class)))
                .thenAnswer(invocation -> Optional.of(
                        ChatSession.builder()
                                .id(testSessionId)
                                .userId("test-user-123")
                                .title("Test Session")
                                .deleted(false)
                                .build()
                ));

        // Save returns the argument
        when(sessionRepository.save(any(ChatSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Call delete
        sessionService.deleteSession(testSessionId);

        // Verify
        verify(sessionRepository, times(1)).findById(testSessionId);
        verify(sessionRepository, times(1)).save(any(ChatSession.class));
    }

}
