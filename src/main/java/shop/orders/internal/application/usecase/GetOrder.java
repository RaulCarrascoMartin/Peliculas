package shop.orders.internal.application.usecase;

import shop.orders.internal.application.dto.OrderView;

import shop.orders.internal.domain.service.OrderService;

public class GetOrder {

    private final OrderService orders;

    public GetOrder(OrderService orders) {
        this.orders = orders;
    }

    public OrderView execute(Long id) {
        return OrderView.of(orders.find(id));
    }
}
