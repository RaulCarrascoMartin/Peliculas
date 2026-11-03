package shop.purchase.api.http.controller;

import shop.purchase.api.http.PurchasesHttpApi;
import shop.purchase.api.http.mapper.PurchaseWebMapper;

import shop.shared.security.CurrentUser;
import shop.purchase.internal.application.usecase.CompletePurchase;
import shop.purchase.internal.application.usecase.ListPurchases;
import shop.purchase.internal.application.usecase.GetPurchase;
import shop.purchase.api.http.dto.Purchase;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PurchaseController implements PurchasesHttpApi {

    private final CompletePurchase completePurchase;
    private final ListPurchases listPurchases;
    private final GetPurchase getPurchase;
    private final CurrentUser currentUser;

    public PurchaseController(CompletePurchase completePurchase, ListPurchases listPurchases,
                              GetPurchase getPurchase, CurrentUser currentUser) {
        this.completePurchase = completePurchase;
        this.listPurchases = listPurchases;
        this.getPurchase = getPurchase;
        this.currentUser = currentUser;
    }

    @Override
    public Purchase completePurchase() {
        return PurchaseWebMapper.toDto(completePurchase.execute(currentUser.id()));
    }

    @Override
    public List<Purchase> listPurchases() {
        return listPurchases.execute(currentUser.id()).stream()
                .map(PurchaseWebMapper::toDto)
                .toList();
    }

    @Override
    public Purchase purchaseDetail(Long purchaseId) {
        return PurchaseWebMapper.toDto(getPurchase.execute(currentUser.id(), purchaseId));
    }
}
