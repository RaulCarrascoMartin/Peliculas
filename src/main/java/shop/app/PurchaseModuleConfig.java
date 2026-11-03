package shop.app;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import shop.cart.api.facade.CartFacade;
import shop.catalog.api.facade.CatalogFacade;
import shop.purchase.internal.application.usecase.CompletePurchase;
import shop.purchase.internal.application.usecase.GetPurchase;
import shop.purchase.internal.application.usecase.ListPurchases;
import shop.purchase.internal.domain.contract.MovieCatalog;
import shop.purchase.internal.domain.repository.PurchaseLineRepository;
import shop.purchase.internal.domain.repository.PurchaseRepository;
import shop.purchase.internal.domain.service.PurchaseService;
import shop.purchase.internal.domain.service.PurchaseServiceImpl;
import shop.purchase.internal.domain.contract.UserCart;
import shop.purchase.internal.persistence.adapter.MovieCatalogAdapter;
import shop.purchase.internal.persistence.jpa.PurchaseJpaRepository;
import shop.purchase.internal.persistence.jpa.PurchaseLineJpaRepository;
import shop.purchase.internal.persistence.repository.PurchaseLineRepositoryImpl;
import shop.purchase.internal.persistence.repository.PurchaseRepositoryImpl;
import shop.purchase.internal.persistence.adapter.UserCartAdapter;

@Configuration
class PurchaseModuleConfig {

    @Bean
    PurchaseRepository purchaseRepository(PurchaseJpaRepository dao) {
        return new PurchaseRepositoryImpl(dao);
    }

    @Bean
    PurchaseLineRepository purchaseLineRepository(PurchaseLineJpaRepository dao) {
        return new PurchaseLineRepositoryImpl(dao);
    }

    @Bean
    MovieCatalog purchaseMovieCatalog(CatalogFacade catalog) {
        return new MovieCatalogAdapter(catalog);
    }

    @Bean
    UserCart purchaseUserCart(CartFacade cart) {
        return new UserCartAdapter(cart);
    }

    @Bean
    PurchaseService purchaseService(PurchaseRepository purchases, PurchaseLineRepository lines,
                                    UserCart purchaseUserCart, MovieCatalog purchaseMovieCatalog) {
        return new PurchaseServiceImpl(purchases, lines, purchaseUserCart, purchaseMovieCatalog);
    }

    @Bean
    CompletePurchase completePurchase(PurchaseService purchaseService) {
        return new CompletePurchase(purchaseService);
    }

    @Bean
    GetPurchase getPurchase(PurchaseService purchaseService) {
        return new GetPurchase(purchaseService);
    }

    @Bean
    ListPurchases listPurchases(PurchaseService purchaseService) {
        return new ListPurchases(purchaseService);
    }
}
