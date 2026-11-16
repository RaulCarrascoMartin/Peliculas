package shop.purchase.api.http.controller;

import org.springframework.web.bind.annotation.RestController;
import shop.purchase.api.http.PurchaseHttpApi;
import shop.purchase.api.http.dto.PurchaseResponse;
import shop.purchase.api.http.mapper.PurchaseWebMapper;
import shop.purchase.internal.domain.service.PurchaseService;
import shop.shared.security.CurrentUser;

import java.util.List;

/**
 * HTTP entry of purchases: implements the contract generated from {@code compras-api.yml} and delegates. The user
 * always comes from the token, never from the request.
 */
@RestController
public class PurchaseController implements PurchaseHttpApi {

    private final PurchaseService purchaseService;
    private final PurchaseWebMapper mapper;
    private final CurrentUser currentUser;

    public PurchaseController(PurchaseService purchaseService, PurchaseWebMapper mapper, CurrentUser currentUser) {
        this.purchaseService = purchaseService;
        this.mapper = mapper;
        this.currentUser = currentUser;
    }

    @Override
    public PurchaseResponse completePurchase() {
        return mapper.toResponse(purchaseService.checkout(currentUser.id()));
    }

    @Override
    public List<PurchaseResponse> listPurchases() {
        return mapper.toResponses(purchaseService.historyOf(currentUser.id()));
    }

    @Override
    public PurchaseResponse purchaseDetail(Long purchaseId) {
        return mapper.toResponse(purchaseService.detailOf(currentUser.id(), purchaseId));
    }
}
