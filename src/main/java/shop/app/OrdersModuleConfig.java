package shop.app;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import shop.catalog.api.facade.CatalogFacade;
import shop.orders.internal.application.usecase.CancelOrder;
import shop.orders.internal.application.usecase.CreateOrder;
import shop.orders.internal.application.usecase.GetOrder;
import shop.orders.internal.domain.repository.OrderRepository;
import shop.orders.internal.domain.service.OrderService;
import shop.orders.internal.domain.service.OrderServiceImpl;
import shop.orders.internal.domain.contract.StockReservation;
import shop.orders.internal.persistence.repository.JdbcOrderRepository;
import shop.orders.internal.persistence.adapter.StockReservationAdapter;

@Configuration
class OrdersModuleConfig {

    @Bean
    OrderRepository orderRepository(JdbcClient jdbc) {
        return new JdbcOrderRepository(jdbc);
    }

    @Bean
    StockReservation stockReservation(CatalogFacade catalog) {
        return new StockReservationAdapter(catalog);
    }

    @Bean
    OrderService orderService(StockReservation stockReservation, OrderRepository orders) {
        return new OrderServiceImpl(stockReservation, orders);
    }

    @Bean
    CreateOrder createOrder(OrderService orderService) {
        return new CreateOrder(orderService);
    }

    @Bean
    GetOrder getOrder(OrderService orderService) {
        return new GetOrder(orderService);
    }

    @Bean
    CancelOrder cancelOrder(OrderService orderService) {
        return new CancelOrder(orderService);
    }
}
