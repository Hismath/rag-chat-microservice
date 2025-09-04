package rag_chat_microservice.exception;

@SuppressWarnings("serial")
public class SessionNotFoundException extends RuntimeException {
    public SessionNotFoundException(String message) {
        super(message);
    }
}


