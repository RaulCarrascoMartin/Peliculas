package shop.orders.internal.domain;

import java.util.Optional;

/** Persistencia de pedidos; la implementación usa JDBC y vive en {@code internal.persistence}. */
public interface OrderRepository {

    /** Inserta el pedido y sus líneas y devuelve el pedido con el id asignado. */
    Order save(Order order);

    Optional<Order> findById(Long id);

    /** Pasa a CANCELLED solo si sigue en CREATED; devuelve {@code false} si no había nada que cancelar. */
    boolean cancelIfCreated(Long id);
}
