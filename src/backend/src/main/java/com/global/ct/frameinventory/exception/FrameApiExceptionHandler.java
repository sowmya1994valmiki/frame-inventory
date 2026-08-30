package com.global.ct.frameinventory.exception;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
class FrameApiExceptionHandler {

    @ExceptionHandler(FrameNotFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound(
        FrameNotFoundException exception,
        HttpServletRequest request
    ) {
        return problem(HttpStatus.NOT_FOUND, "Frame not found", exception.getMessage(), request);
    }

    @ExceptionHandler(DuplicateFrameException.class)
    ResponseEntity<ProblemDetail> handleDuplicate(
        DuplicateFrameException exception,
        HttpServletRequest request
    ) {
        return problem(HttpStatus.CONFLICT, "Frame already exists", exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidFrameRequestException.class)
    ResponseEntity<ProblemDetail> handleInvalidRequest(
        InvalidFrameRequestException exception,
        HttpServletRequest request
    ) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidCsvFileException.class)
    ResponseEntity<ProblemDetail> handleInvalidCsv(
        InvalidCsvFileException exception,
        HttpServletRequest request
    ) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid CSV file", exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidation(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        List<Map<String, String>> errors = exception.getBindingResult().getAllErrors().stream()
            .map(error -> Map.of(
                "field", error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName(),
                "message", error.getDefaultMessage() == null ? "is invalid" : error.getDefaultMessage()
            ))
            .toList();
        ResponseEntity<ProblemDetail> response = problem(
            HttpStatus.BAD_REQUEST,
            "Validation failed",
            "One or more fields are invalid",
            request
        );
        response.getBody().setProperty("errors", errors);
        return response;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> handleUnreadable(
        HttpMessageNotReadableException exception,
        HttpServletRequest request
    ) {
        return problem(
            HttpStatus.BAD_REQUEST,
            "Invalid request body",
            "Request body is malformed or contains unsupported fields",
            request
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ProblemDetail> handleTypeMismatch(
        MethodArgumentTypeMismatchException exception,
        HttpServletRequest request
    ) {
        return problem(
            HttpStatus.BAD_REQUEST,
            "Invalid request parameter",
            "Parameter '" + exception.getName() + "' has an invalid value",
            request
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ProblemDetail> handleMethodNotSupported(
        HttpRequestMethodNotSupportedException exception,
        HttpServletRequest request
    ) {
        ProblemDetail problem = problemDetail(
            HttpStatus.METHOD_NOT_ALLOWED,
            "Method not allowed",
            exception.getMessage(),
            request
        );
        ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED);
        if (exception.getSupportedHttpMethods() != null) {
            response.allow(exception.getSupportedHttpMethods().toArray(HttpMethod[]::new));
        }
        return response.body(problem);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ProblemDetail> handleMediaTypeNotSupported(
        HttpMediaTypeNotSupportedException exception,
        HttpServletRequest request
    ) {
        ProblemDetail problem = problemDetail(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "Unsupported media type",
            exception.getMessage(),
            request
        );
        ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        if (!exception.getSupportedMediaTypes().isEmpty()) {
            response.header(HttpHeaders.ACCEPT, MediaType.toString(exception.getSupportedMediaTypes()));
        }
        return response.body(problem);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> handleDataIntegrityViolation(
        DataIntegrityViolationException exception,
        HttpServletRequest request
    ) {
        return problem(
            HttpStatus.BAD_REQUEST,
            "Frame data could not be persisted",
            "The frame could not be saved. Please check the entered values and try again.",
            request
        );
    }

    private ResponseEntity<ProblemDetail> problem(
        HttpStatus status,
        String title,
        String detail,
        HttpServletRequest request
    ) {
        return ResponseEntity.status(status).body(problemDetail(status, title, detail, request));
    }

    private ProblemDetail problemDetail(
        HttpStatus status,
        String title,
        String detail,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
