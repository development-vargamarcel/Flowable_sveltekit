package com.demo.bpm.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle validation errors from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                        (first, second) -> first + ", " + second
                ));

        log.warn("Validation failed: {}", errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse("Validation failed", errorMessage, fieldErrors));
    }

    /**
     * Handle constraint violations from @Validated annotations
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        String errorMessage = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        log.warn("Constraint violation: {}", errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse("Constraint violation", errorMessage, null));
    }

    /**
     * Handle malformed JSON or missing request body
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        String message = "Invalid request body";
        String details;

        Throwable cause = ex.getCause();
        if (cause != null) {
            String causeMessage = cause.getMessage();
            if (causeMessage != null) {
                if (causeMessage.contains("Required request body is missing")) {
                    message = "Request body is required";
                    details = "Please provide a valid JSON body with the required fields";
                } else if (causeMessage.contains("Cannot deserialize")) {
                    message = "Invalid JSON format";
                    details = "The JSON body could not be parsed. Check for syntax errors.";
                } else if (causeMessage.contains("Unexpected character")) {
                    message = "Malformed JSON";
                    details = "The request body contains invalid JSON syntax";
                } else {
                    details = causeMessage.length() > 200 ? causeMessage.substring(0, 200) + "..." : causeMessage;
                }
            } else {
                details = "The request body could not be read";
            }
        } else {
            details = ex.getMessage() != null ? ex.getMessage() : "Unknown parsing error";
        }

        log.warn("HTTP message not readable: {}", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(message, details, null));
    }

    /**
     * Handle unsupported Content-Type
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        String contentType = ex.getContentType() != null ? ex.getContentType().toString() : "unknown";
        String supported = ex.getSupportedMediaTypes().stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "));

        String details = String.format("Content-Type '%s' is not supported. Supported types: %s", contentType, supported);

        log.warn("Unsupported media type: {}", contentType);
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(buildErrorResponse("Unsupported Content-Type", details, null));
    }

    /**
     * Handle unsupported HTTP method
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        String method = ex.getMethod();
        String[] supported = ex.getSupportedMethods();
        String supportedMethods = supported != null ? String.join(", ", supported) : "none";

        String details = String.format("HTTP method '%s' is not supported. Supported methods: %s", method, supportedMethods);

        log.warn("Unsupported HTTP method: {}", method);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(buildErrorResponse("Method not allowed", details, null));
    }

    /**
     * Handle missing required request parameters
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParameter(MissingServletRequestParameterException ex) {
        String details = String.format("Required parameter '%s' of type '%s' is missing",
                ex.getParameterName(), ex.getParameterType());

        log.warn("Missing parameter: {}", ex.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse("Missing required parameter", details, null));
    }

    /**
     * Handle type mismatch in request parameters
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String details = String.format("Parameter '%s' should be of type '%s' but received value '%s'",
                ex.getName(), requiredType, ex.getValue());

        log.warn("Type mismatch for parameter: {}", ex.getName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse("Invalid parameter type", details, null));
    }

    /**
     * Handle 404 - No handler found
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoHandlerFound(NoHandlerFoundException ex) {
        String details = String.format("No endpoint found for %s %s", ex.getHttpMethod(), ex.getRequestURL());

        log.warn("No handler found: {} {}", ex.getHttpMethod(), ex.getRequestURL());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildErrorResponse("Endpoint not found", details, null));
    }

    /**
     * Handle all other unexpected exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error: {} - {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);

        // Don't expose internal error details in production
        String details = "An internal server error occurred. Please try again later.";

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse("Internal server error", details, null));
    }

    /**
     * Build a consistent error response structure
     */
    private Map<String, Object> buildErrorResponse(String error, String message, Map<String, String> fieldErrors) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("error", error);
        response.put("message", message);
        response.put("timestamp", Instant.now().toString());

        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            response.put("fieldErrors", fieldErrors);
        }

        return response;
    }
}
