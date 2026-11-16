package shop.orders.api.http.controller;

import org.springframework.web.bind.annotation.RestController;
import shop.orders.api.http.OrderHttpApi;
import shop.orders.api.http.dto.CreateOrderRequest;
import shop.orders.api.http.dto.OrderResponse;
import shop.orders.api.http.mapper.OrderWebMapper;
import shop.orders.internal.domain.service.OrderService;
import shop.shared.security.CurrentUser;

/** HTTP entry of orders: implements the contract generated from {@code orders-api.yml} and delegates. The user always comes from the token. */
@RestController
public class OrderController implements OrderHttpApi {

    private final OrderService orderService;
    private final OrderWebMapper mapper;
    private final CurrentUser currentUser;

    public OrderController(OrderService orderService, OrderWebMapper mapper, CurrentUser currentUser) {
        this.orderService = orderService;
        this.mapper = mapper;
        this.currentUser = currentUser;
    }

    @Override
    public OrderResponse createOrder(CreateOrderRequest createOrderRequest) {
        return mapper.toResponse(orderService.create(mapper.toDto(currentUser.id(), createOrderRequest)));
    }

    @Override
    public OrderResponse getOrder(Long id) {
        return mapper.toResponse(orderService.find(currentUser.id(), id));
    }

    @Override
    public OrderResponse cancelOrder(Long id) {
        return mapper.toResponse(orderService.cancel(currentUser.id(), id));
    }
}
