package shop.purchase.internal.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class PurchaseRepositoryTest {

    @Autowired
    private PurchaseRepository repository;

    private Purchase purchase(Long userId, LocalDateTime purchasedAt) {
        var purchase = new Purchase();
        purchase.setUserId(userId);
        purchase.setPurchasedAt(purchasedAt);
        purchase.setTotal(new BigDecimal("19.98"));
        purchase.setStatus(PurchaseStatus.COMPRADA);
        return purchase;
    }

    @Test
    void savesPurchaseWithStatusAndTotal() {
        var saved = repository.saveAndFlush(purchase(1L, LocalDateTime.of(2026, 11, 2, 9, 0)));

        var read = repository.findById(saved.getId()).orElseThrow();
        assertEquals(PurchaseStatus.COMPRADA, read.getStatus());
        assertEquals(new BigDecimal("19.98"), read.getTotal());
        assertEquals(1L, read.getUserId());
    }

    @Test
    void listsUserPurchasesByDateDescending() {
        var old = repository.saveAndFlush(purchase(1L, LocalDateTime.of(2026, 11, 1, 9, 0)));
        var recent = repository.saveAndFlush(purchase(1L, LocalDateTime.of(2026, 11, 3, 9, 0)));
        var intermediate = repository.saveAndFlush(purchase(1L, LocalDateTime.of(2026, 11, 2, 9, 0)));
        repository.saveAndFlush(purchase(2L, LocalDateTime.of(2026, 11, 4, 9, 0)));

        List<Long> ids = repository.findByUserIdOrderByPurchasedAtDesc(1L).stream().map(Purchase::getId).toList();

        assertEquals(List.of(recent.getId(), intermediate.getId(), old.getId()), ids);
    }

    @Test
    void othersPurchaseIsNotFoundByUser() {
        var others = repository.saveAndFlush(purchase(2L, LocalDateTime.of(2026, 11, 2, 9, 0)));

        assertTrue(repository.findByIdAndUserId(others.getId(), 1L).isEmpty());
        assertEquals(others.getId(), repository.findByIdAndUserId(others.getId(), 2L).orElseThrow().getId());
    }

    @Test
    void requiresDateTotalAndStatus() {
        var withoutDate = purchase(1L, null);
        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(withoutDate));
    }
}
