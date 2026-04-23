package com.task.all.exception;

import com.task.all.model.dto.ApiErrorDto;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * Manejador centralizado de errores. Devuelve respuestas JSON coherentes
 * ({@link ApiErrorDto}) para todas las excepciones esperadas.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ApiErrorDto> handleInvalidTransition(InvalidStatusTransitionException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorDto> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Error de validación en la solicitud", request, details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorDto> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud es inválido o está mal formado", request, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorDto> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = "El parámetro '" + ex.getName() + "' tiene un valor inválido";
        return build(HttpStatus.BAD_REQUEST, message, request, List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorDto> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDto> handleGeneric(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor: " + ex.getMessage(), request, List.of());
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private ResponseEntity<ApiErrorDto> build(HttpStatus status, String message, HttpServletRequest request, List<String> details) {
        ApiErrorDto dto = new ApiErrorDto(status.value(), status.getReasonPhrase(), message, request.getRequestURI());
        dto.setDetails(details);
        return ResponseEntity.status(status).body(dto);
    }
}
