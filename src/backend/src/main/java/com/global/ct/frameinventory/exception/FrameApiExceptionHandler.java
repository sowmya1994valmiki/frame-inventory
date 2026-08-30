package com.global.ct.frameinventory.exception;

import java.net.URI;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.global.ct.frameinventory.logging.LogValueSanitizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
class FrameApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrameApiExceptionHandler.class);

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
        if (isClientDataViolation(exception)) {
            return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid frame data",
                "The supplied frame data is invalid",
                request
            );
        }
        return handleDataAccess(exception, request);
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ProblemDetail> handleDataAccess(
        DataAccessException exception,
        HttpServletRequest request
    ) {
        LOGGER.error(
            "Database failure method={} path={} exceptionType={}",
            request.getMethod(),
            LogValueSanitizer.sanitize(request.getRequestURI()),
            exception.getClass().getSimpleName()
        );
        return problem(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal server error",
            "An unexpected error occurred",
            request
        );
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(
        Exception exception,
        HttpServletRequest request
    ) {
        if (exception instanceof ErrorResponse errorResponse
            && errorResponse.getStatusCode().is4xxClientError()) {
            return ResponseEntity.status(errorResponse.getStatusCode())
                .headers(errorResponse.getHeaders())
                .body(errorResponse.getBody());
        }
        LOGGER.error(
            "Unexpected API error method={} path={}",
            request.getMethod(),
            LogValueSanitizer.sanitize(request.getRequestURI()),
            exception
        );
        return problem(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal server error",
            "An unexpected error occurred",
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

    private boolean isClientDataViolation(DataIntegrityViolationException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                String sqlState = sqlException.getSQLState();
                if ((sqlState != null && sqlState.startsWith("22"))
                    || sqlException.getErrorCode() == 1406) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
