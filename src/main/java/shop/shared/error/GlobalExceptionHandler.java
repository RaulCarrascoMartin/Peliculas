package shop.shared.error;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
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

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String VALIDATION_MESSAGE = "Datos de entrada no válidos";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> resourceNotFound(ResourceNotFoundException ex) {
        return response(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> conflict(ConflictException ex) {
        return response(HttpStatus.CONFLICT, ex.getMessage(), Map.of());
    }

    /**
     * Lost optimistic lock (two requests on the same cart): 409 wherever the failure happens,
     * including the flush or commit that occurs outside the service body. It exposes neither the entity nor the id.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> optimisticLock(OptimisticLockingFailureException ex) {
        return response(HttpStatus.CONFLICT, "El recurso fue modificado por otra petición; inténtalo de nuevo", Map.of());
    }

    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ApiError> validation(BusinessValidationException ex) {
        return response(HttpStatus.BAD_REQUEST, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(jsonName(ex.getBindingResult().getTarget(), error.getField()), error.getDefaultMessage());
        }
        return response(HttpStatus.BAD_REQUEST, VALIDATION_MESSAGE, errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> parameterValidation(HandlerMethodValidationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            String name = result.getMethodParameter().getParameterName();
            String key = name != null ? name : "argumento" + result.getMethodParameter().getParameterIndex();
            result.getResolvableErrors().stream()
                    .findFirst()
                    .ifPresent(error -> errors.putIfAbsent(key, error.getDefaultMessage()));
        }
        return response(HttpStatus.BAD_REQUEST, VALIDATION_MESSAGE, errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> unreadableBody(HttpMessageNotReadableException ex) {
        return response(HttpStatus.BAD_REQUEST, "Cuerpo de la petición no válido", Map.of());
    }

    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ApiError> invalidType(TypeMismatchException ex) {
        return response(HttpStatus.BAD_REQUEST, "Parámetro con formato no válido", Map.of());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> invalidCredentials(InvalidCredentialsException ex) {
        return response(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas", Map.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> unauthenticated(AuthenticationException ex) {
        return response(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas", Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> accessDenied(AccessDeniedException ex) {
        return response(HttpStatus.FORBIDDEN, "Acceso denegado", Map.of());
    }

    /**
     * Remaining exceptions: Spring MVC ones that already carry a status (405, 415, route 404...)
     * keep their status; any other is a 500 that does not leak internal details.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception ex) {
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            String detail = errorResponse.getBody().getDetail();
            return ResponseEntity.status(status)
                    .body(new ApiError(status.value(), detail != null ? detail : "Petición no válida", Map.of()));
        }
        log.error("Unexpected error", ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", Map.of());
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

    private ResponseEntity<ApiError> response(HttpStatus status, String message, Map<String, String> errors) {
        return ResponseEntity.status(status).body(new ApiError(status.value(), message, errors));
    }
}
