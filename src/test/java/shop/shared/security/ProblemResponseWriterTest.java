package shop.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProblemResponseWriterTest {

    @Test
    void writesStatusProblemContentTypeAndBody() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ProblemResponseWriter.write(response, HttpStatus.FORBIDDEN, "Acceso denegado");

        assertEquals(403, response.getStatus());
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON_VALUE + ";charset=UTF-8", response.getContentType());
        assertEquals("UTF-8", response.getCharacterEncoding());
        assertEquals("{\"type\":\"about:blank\",\"title\":\"Forbidden\",\"status\":403,"
                + "\"detail\":\"Acceso denegado\"}", response.getContentAsString());
    }
}
