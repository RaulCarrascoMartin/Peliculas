package shop.catalog.internal.domain.exception;

import shop.shared.error.BusinessValidationException;

/** The search asks to sort by something that is not a sortable property of a movie (400). */
public class InvalidSortPropertyException extends BusinessValidationException {

    public InvalidSortPropertyException(String property) {
        super("Propiedad de ordenación no válida: " + property);
    }
}
