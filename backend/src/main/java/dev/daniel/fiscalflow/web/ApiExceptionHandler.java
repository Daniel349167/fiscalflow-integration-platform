package dev.daniel.fiscalflow.web;

import dev.daniel.fiscalflow.application.DocumentNotFoundException;
import dev.daniel.fiscalflow.application.IdempotencyConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(DocumentNotFoundException.class)
    ProblemDetail notFound(DocumentNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Document not found", exception.getMessage(), "document-not-found");
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    ProblemDetail idempotencyConflict(IdempotencyConflictException exception) {
        return problem(HttpStatus.CONFLICT, "Idempotency conflict", exception.getMessage(), "idempotency-conflict");
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail invalidTransition(IllegalStateException exception) {
        return problem(HttpStatus.CONFLICT, "Invalid state transition", exception.getMessage(), "invalid-transition");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Request validation failed");
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", detail, "validation-error");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail, String type) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://fiscalflow.dev/problems/" + type));
        return problem;
    }
}
