package org.example.compartido;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
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

    private static final String MENSAJE_VALIDACION = "Datos de entrada no válidos";

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorRespuesta> recursoNoEncontrado(RecursoNoEncontradoException ex) {
        return respuesta(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ErrorRespuesta> conflicto(ConflictoException ex) {
        return respuesta(HttpStatus.CONFLICT, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(ValidacionException.class)
    public ResponseEntity<ErrorRespuesta> validacion(ValidacionException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return respuesta(HttpStatus.BAD_REQUEST, MENSAJE_VALIDACION, errores);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorRespuesta> validacionDeParametros(HandlerMethodValidationException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (ParameterValidationResult resultado : ex.getParameterValidationResults()) {
            String nombre = resultado.getMethodParameter().getParameterName();
            String clave = nombre != null ? nombre : "argumento" + resultado.getMethodParameter().getParameterIndex();
            resultado.getResolvableErrors().stream()
                    .findFirst()
                    .ifPresent(error -> errores.putIfAbsent(clave, error.getDefaultMessage()));
        }
        return respuesta(HttpStatus.BAD_REQUEST, MENSAJE_VALIDACION, errores);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespuesta> cuerpoNoLegible(HttpMessageNotReadableException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "Cuerpo de la petición no válido", Map.of());
    }

    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ErrorRespuesta> tipoNoValido(TypeMismatchException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "Parámetro con formato no válido", Map.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorRespuesta> noAutenticado(AuthenticationException ex) {
        return respuesta(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas", Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorRespuesta> accesoDenegado(AccessDeniedException ex) {
        return respuesta(HttpStatus.FORBIDDEN, "Acceso denegado", Map.of());
    }

    /**
     * Resto de excepciones: las de Spring MVC que ya traen estado (405, 415, 404 de ruta...)
     * conservan su estado; cualquier otra es un 500 que no filtra detalles internos.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> inesperada(Exception ex) {
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatusCode estado = errorResponse.getStatusCode();
            String detalle = errorResponse.getBody().getDetail();
            return ResponseEntity.status(estado)
                    .body(new ErrorRespuesta(estado.value(), detalle != null ? detalle : "Petición no válida", Map.of()));
        }
        log.error("Error inesperado", ex);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", Map.of());
    }

    private ResponseEntity<ErrorRespuesta> respuesta(HttpStatus estado, String mensaje, Map<String, String> errores) {
        return ResponseEntity.status(estado).body(new ErrorRespuesta(estado.value(), mensaje, errores));
    }
}
