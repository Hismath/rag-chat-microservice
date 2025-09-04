package rag_chat_microservice.exception;

@SuppressWarnings("serial")
public class MessageNotFoundException extends RuntimeException {
    public MessageNotFoundException(String message) {
        super(message);
    }
}