package com.global.ct.frameinventory.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;

import com.global.ct.frameinventory.logging.LogValueSanitizer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(OutputCaptureExtension.class)
class FrameApiExceptionHandlerTest {

    private final FrameApiExceptionHandler handler = new FrameApiExceptionHandler();

    @Test
    void logsUnexpectedErrorsAndReturnsSafeProblemDetail(CapturedOutput output) {
        HttpServletRequest request = request("GET", "/api/frames/broken");

        ResponseEntity<ProblemDetail> response = handler.handleUnexpected(
            new IllegalStateException("simulated failure"),
            request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Internal server error");
        assertThat(response.getBody().getDetail()).isEqualTo("An unexpected error occurred");
        assertThat(output).contains(
            "Unexpected API error method=GET path=/api/frames/broken",
            "java.lang.IllegalStateException: simulated failure"
        );
    }

    @Test
    void treatsUnexpectedDataIntegrityFailuresAsSafeServerErrors(CapturedOutput output) {
        HttpServletRequest request = request("PUT", "/api/frames/frame-123");

        ResponseEntity<ProblemDetail> response = handler.handleDataIntegrityViolation(
            new DataIntegrityViolationException("simulated persistence failure"),
            request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Internal server error");
        assertThat(response.getBody().getDetail()).isEqualTo("An unexpected error occurred");
        assertThat(output).contains(
            "Database failure method=PUT path=/api/frames/frame-123",
            "exceptionType=DataIntegrityViolationException"
        );
        assertThat(output).doesNotContain("simulated persistence failure");
    }

    @Test
    void treatsRecognisedClientDataViolationsAsBadRequests(CapturedOutput output) {
        HttpServletRequest request = request("POST", "/api/frames");
        SQLException cause = new SQLException("sensitive database detail", "22001");

        ResponseEntity<ProblemDetail> response = handler.handleDataIntegrityViolation(
            new DataIntegrityViolationException("sensitive persistence detail", cause),
            request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Invalid frame data");
        assertThat(response.getBody().getDetail()).isEqualTo(
            "The supplied frame data is invalid"
        );
        assertThat(output).doesNotContain(
            "sensitive database detail",
            "sensitive persistence detail"
        );
    }

    @Test
    void truncatesRequestPathsBeforeLogging(CapturedOutput output) {
        String requestPath = "/" + "x".repeat(300);
        HttpServletRequest request = request("GET", requestPath);

        handler.handleDataIntegrityViolation(
            new DataIntegrityViolationException("simulated persistence failure"),
            request
        );

        assertThat(output)
            .contains("path=" + LogValueSanitizer.sanitize(requestPath))
            .doesNotContain("path=" + requestPath);
    }

    @Test
    void doesNotLogFrameworkClientErrorsAsUnexpected(CapturedOutput output) {
        HttpServletRequest request = request("GET", "/api/frames");

        ResponseEntity<ProblemDetail> response = handler.handleUnexpected(
            new MissingServletRequestParameterException("required", "String"),
            request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(output).doesNotContain("Unexpected API error");
    }

    private HttpServletRequest request(String method, String path) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getRequestURI()).thenReturn(path);
        return request;
    }
}
