package shop.rating.internal.domain.exception;

import shop.shared.error.BusinessValidationException;

/** The score is outside the allowed range (400). */
public class InvalidScoreException extends BusinessValidationException {

    public InvalidScoreException(int min, int max) {
        super("La puntuación debe estar entre " + min + " y " + max);
    }
}
