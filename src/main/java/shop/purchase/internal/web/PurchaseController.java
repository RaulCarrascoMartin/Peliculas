package shop.purchase.internal.web;

import shop.shared.security.CurrentUser;
import shop.purchase.internal.usecase.CompletePurchase;
import shop.purchase.internal.usecase.PurchaseLineView;
import shop.purchase.internal.usecase.ListPurchases;
import shop.purchase.internal.usecase.GetPurchase;
import shop.purchase.internal.usecase.PurchaseView;
import shop.purchase.internal.web.api.PurchasesRestApi;
import shop.purchase.internal.web.dto.Purchase;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.util.List;

@RestController
public class PurchaseController implements PurchasesRestApi {

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
        return toDto(completePurchase.execute(currentUser.id()));
    }

    @Override
    public List<Purchase> listPurchases() {
        return listPurchases.execute(currentUser.id()).stream()
                .map(PurchaseController::toDto)
                .toList();
    }

    @Override
    public Purchase purchaseDetail(Long purchaseId) {
        return toDto(getPurchase.execute(currentUser.id(), purchaseId));
    }

    /** The record holds a {@code LocalDateTime} (server time); the contract publishes {@code date-time} with an offset. */
    private static Purchase toDto(PurchaseView view) {
        List<shop.purchase.internal.web.dto.PurchaseLine> lines = view.lines().stream()
                .map(PurchaseController::toDto)
                .toList();
        return new Purchase(view.id(), view.purchasedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime(),
                Purchase.EstadoEnum.valueOf(view.status()), lines, view.total());
    }

    private static shop.purchase.internal.web.dto.PurchaseLine toDto(PurchaseLineView line) {
        return new shop.purchase.internal.web.dto.PurchaseLine(line.movieId(), line.title(),
                line.unitPrice());
    }
}
