package com.demo.bpm.exception;

import com.demo.bpm.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleAllExceptions(Exception ex, WebRequest request) {
        String traceId = UUID.randomUUID().toString();
        log.error("Unhandled exception [TraceID: {}]: ", traceId, ex);

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String error = "Internal Server Error";
        String message = "An unexpected error occurred. Please contact support with the Trace ID.";

        // In production, we should NOT return the stack trace.
        // We log it (above) and return the traceId for correlation.
        return buildResponse(status, error, message, null, request, traceId);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), null, request, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        String traceId = UUID.randomUUID().toString();
        log.warn("Illegal argument [TraceID: {}]: {}", traceId, ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), null, request, traceId);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalStateException(IllegalStateException ex, WebRequest request) {
        String traceId = UUID.randomUUID().toString();
        log.warn("Illegal state [TraceID: {}]: {}", traceId, ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), null, request, traceId);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        String traceId = UUID.randomUUID().toString();
        log.warn("Access denied [TraceID: {}]: {}", traceId, ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, "Forbidden", "Access is denied", null, request, traceId);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNoHandlerFoundException(NoHandlerFoundException ex, WebRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "Not Found", "The requested resource was not found", null, request, null);
    }

    // Handle JSON parsing errors, bad types, etc.
    @ExceptionHandler({HttpMessageNotReadableException.class, TypeMismatchException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponseDTO> handleBadRequestExceptions(Exception ex, WebRequest request) {
        String traceId = UUID.randomUUID().toString();
        log.warn("Bad request [TraceID: {}]: {}", traceId, ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", "Invalid request format or parameters", null, request, traceId);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        return handleBindingErrors(ex.getBindingResult(), request);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponseDTO> handleBindException(BindException ex, WebRequest request) {
        return handleBindingErrors(ex, request);
    }

    private ResponseEntity<ErrorResponseDTO> handleBindingErrors(org.springframework.validation.BindingResult bindingResult, WebRequest request) {
        String traceId = UUID.randomUUID().toString();
        log.warn("Validation failed [TraceID: {}]", traceId);

        Map<String, String> errors = new HashMap<>();
        bindingResult.getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Error")
                .message("Input validation failed")
                .fieldErrors(errors)
                .path(getPath(request))
                .details("Trace ID: " + traceId)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // Helper methods

    private ResponseEntity<ErrorResponseDTO> buildResponse(HttpStatus status, String error, String message, String trace, WebRequest request, String traceId) {
        String path = getPath(request);

        String details = null;
        if (traceId != null) {
            details = "Trace ID: " + traceId;
        }

        ErrorResponseDTO response = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(error)
                .message(message)
                .path(path)
                .trace(trace)
                .details(details)
                .build();

        return new ResponseEntity<>(response, status);
    }

    private String getPath(WebRequest request) {
        if (request instanceof ServletWebRequest) {
            return ((ServletWebRequest) request).getRequest().getRequestURI();
        }
        return request.getContextPath();
    }

    private String getStackTrace(Exception ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        return sw.toString();
    }
}
