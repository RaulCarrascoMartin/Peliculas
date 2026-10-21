package org.example.compartido;

/** Regla de validación de negocio incumplida; se traduce a 400. */
public class ValidacionException extends RuntimeException {
    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}
