package rag_chat_microservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<ApiError> handleSessionNotFoundException(SessionNotFoundException ex, WebRequest request) {
        ApiError apiError = new ApiError(
            LocalDateTime.now(),
            HttpStatus.NOT_FOUND,
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(apiError, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MessageNotFoundException.class)
    public ResponseEntity<ApiError> handleMessageNotFoundException(MessageNotFoundException ex, WebRequest request) {
        ApiError apiError = new ApiError(
            LocalDateTime.now(),
            HttpStatus.NOT_FOUND,
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(apiError, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));
        
        ApiError apiError = new ApiError(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST,
            "Validation failed: " + errors,
            request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleJsonParseError(HttpMessageNotReadableException ex, WebRequest request) {
        ApiError apiError = new ApiError(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST,
            "Malformed JSON request: " + ex.getLocalizedMessage(),
            request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGlobalException(Exception ex, WebRequest request) {
        ApiError apiError = new ApiError(
            LocalDateTime.now(),
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred: " + ex.getLocalizedMessage(),
            request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
