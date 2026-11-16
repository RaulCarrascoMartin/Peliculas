package shop.shared.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Writes a {@code ProblemDetail} (RFC 9457) body for failures that happen in the security chain, before any
 * controller: same format as the errors produced by {@code GlobalExceptionHandler}.
 */
final class ProblemResponseWriter {

    private ProblemResponseWriter() {
    }

    /** {@code detail} must be a fixed message of the application: it is written as is. */
    static void write(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + status.getReasonPhrase()
                + "\",\"status\":" + status.value() + ",\"detail\":\"" + detail + "\"}");
    }
}
