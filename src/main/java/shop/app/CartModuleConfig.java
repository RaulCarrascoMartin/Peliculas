package shop.app;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import shop.cart.api.facade.CartFacade;
import shop.cart.internal.application.usecase.AddToCart;
import shop.cart.internal.application.facade.CartFacadeImpl;
import shop.cart.internal.application.usecase.ClearCart;
import shop.cart.internal.application.usecase.RemoveFromCart;
import shop.cart.internal.application.usecase.ViewCart;
import shop.cart.internal.domain.repository.CartLineRepository;
import shop.cart.internal.domain.repository.CartRepository;
import shop.cart.internal.domain.service.CartService;
import shop.cart.internal.domain.service.CartServiceImpl;
import shop.cart.internal.domain.contract.MovieCatalog;
import shop.cart.internal.persistence.repository.JdbcCartLineRepository;
import shop.cart.internal.persistence.repository.JdbcCartRepository;
import shop.cart.internal.persistence.adapter.MovieCatalogAdapter;
import shop.catalog.api.facade.CatalogFacade;

@Configuration
class CartModuleConfig {

    @Bean
    CartRepository cartRepository(JdbcClient jdbc) {
        return new JdbcCartRepository(jdbc);
    }

    @Bean
    CartLineRepository cartLineRepository(JdbcClient jdbc) {
        return new JdbcCartLineRepository(jdbc);
    }

    @Bean
    MovieCatalog cartMovieCatalog(CatalogFacade catalog) {
        return new MovieCatalogAdapter(catalog);
    }

    @Bean
    CartService cartService(CartRepository carts, CartLineRepository lines, MovieCatalog cartMovieCatalog) {
        return new CartServiceImpl(carts, lines, cartMovieCatalog);
    }

    @Bean
    ViewCart viewCart(CartService cartService) {
        return new ViewCart(cartService);
    }

    @Bean
    AddToCart addToCart(CartService cartService) {
        return new AddToCart(cartService);
    }

    @Bean
    RemoveFromCart removeFromCart(CartService cartService) {
        return new RemoveFromCart(cartService);
    }

    @Bean
    ClearCart clearCart(CartService cartService) {
        return new ClearCart(cartService);
    }

    /** Public face of the module: the only bean other modules reach (through {@link CartFacade}). */
    @Bean
    CartFacade cartFacade(CartService cartService, ClearCart clearCart) {
        return new CartFacadeImpl(cartService, clearCart);
    }
}
