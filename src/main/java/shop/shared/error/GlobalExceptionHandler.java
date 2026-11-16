package shop.shared.error;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Single translation point of exceptions to HTTP: every error of the API is a {@link ProblemDetail} (RFC 9457).
 * It lives in {@code shared} because it serves every module and cannot see their internals: each module's
 * exceptions extend the shared categories ({@link ResourceNotFoundException}, {@link ConflictException}...).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String VALIDATION_MESSAGE = "Datos de entrada no válidos";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail resourceNotFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail conflict(ConflictException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Lost optimistic lock (two requests on the same cart) or a lock the database could not grant because of a
     * concurrent transaction: 409 wherever the failure happens, since retrying is the right answer. It exposes neither
     * the SQL nor the id.
     */
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ProblemDetail optimisticLock(ConcurrencyFailureException ex) {
        return problem(HttpStatus.CONFLICT, "El recurso fue modificado por otra petición; inténtalo de nuevo");
    }

    @ExceptionHandler(BusinessValidationException.class)
    public ProblemDetail validation(BusinessValidationException ex) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(jsonName(ex.getBindingResult().getTarget(), error.getField()), error.getDefaultMessage());
        }
        return validationProblem(errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail parameterValidation(HandlerMethodValidationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            String name = result.getMethodParameter().getParameterName();
            String key = name != null ? name : "argumento" + result.getMethodParameter().getParameterIndex();
            result.getResolvableErrors().stream()
                    .findFirst()
                    .ifPresent(error -> errors.putIfAbsent(key, error.getDefaultMessage()));
        }
        return validationProblem(errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail unreadableBody(HttpMessageNotReadableException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Cuerpo de la petición no válido");
    }

    @ExceptionHandler(TypeMismatchException.class)
    public ProblemDetail invalidType(TypeMismatchException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Parámetro con formato no válido");
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail invalidCredentials(InvalidCredentialsException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail unauthenticated(AuthenticationException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail accessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "Acceso denegado");
    }

    /**
     * Remaining exceptions: Spring MVC ones that already carry a status (405, 415, route 404...)
     * keep their status; any other is a 500 that does not leak internal details.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail unexpected(Exception ex) {
        if (ex instanceof ErrorResponse errorResponse) {
            String detail = errorResponse.getBody().getDetail();
            return ProblemDetail.forStatusAndDetail(errorResponse.getStatusCode(),
                    detail != null ? detail : "Petición no válida");
        }
        log.error("Unexpected error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }

    /** Field errors are reported under the JSON property name clients send, not the Java field name. */
    private static String jsonName(Object target, String field) {
        if (target != null) {
            try {
                JsonProperty property = target.getClass().getDeclaredField(field).getAnnotation(JsonProperty.class);
                if (property != null) {
                    return property.value();
                }
            } catch (NoSuchFieldException e) {
                // nested or computed path: report it as is
            }
        }
        return field;
    }

    private static ProblemDetail problem(HttpStatus status, String detail) {
        return ProblemDetail.forStatusAndDetail(status, detail);
    }

    /** 400 with the property {@code errors}: one {@code "field: message"} entry per rejected field. */
    private static ProblemDetail validationProblem(Map<String, String> errors) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, VALIDATION_MESSAGE);
        problem.setProperty("errors", errors.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .toList());
        return problem;
    }
}
