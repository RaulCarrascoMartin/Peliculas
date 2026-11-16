package shop.orders.api.http.controller;

import shop.orders.api.http.OrdersHttpApi;
import shop.orders.api.http.mapper.OrdersWebMapper;

import org.springframework.web.bind.annotation.RestController;
import shop.orders.internal.application.usecase.CancelOrder;
import shop.orders.internal.application.usecase.CreateOrder;
import shop.orders.internal.application.usecase.GetOrder;
import shop.orders.internal.application.dto.NewOrder;
import shop.orders.internal.application.dto.NewOrderLine;
import shop.orders.api.http.dto.Order;
import shop.orders.api.http.dto.OrderLine;
import shop.orders.api.http.dto.OrderRequest;

import java.util.List;

@RestController
public class OrdersController implements OrdersHttpApi {

    private final CreateOrder createOrder;
    private final GetOrder getOrder;
    private final CancelOrder cancelOrder;

    public OrdersController(CreateOrder createOrder, GetOrder getOrder, CancelOrder cancelOrder) {
        this.createOrder = createOrder;
        this.getOrder = getOrder;
        this.cancelOrder = cancelOrder;
    }

    @Override
    public Order createOrder(OrderRequest orderRequest) {
        List<NewOrderLine> lines = orderRequest.lines().stream()
                .map(l -> new NewOrderLine(l.productId(), l.quantity()))
                .toList();
        return OrdersWebMapper.toDto(createOrder.execute(new NewOrder(orderRequest.userId(), lines)));
    }

    @Override
    public Order getOrder(Long id) {
        return OrdersWebMapper.toDto(getOrder.execute(id));
    }

    @Override
    public Order cancelOrder(Long id) {
        return OrdersWebMapper.toDto(cancelOrder.execute(id));
    }
}
