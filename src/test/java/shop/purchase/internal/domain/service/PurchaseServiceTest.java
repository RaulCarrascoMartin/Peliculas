package shop.purchase.internal.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.dao.OptimisticLockingFailureException;
import shop.purchase.internal.domain.contract.MovieCatalog;
import shop.purchase.internal.domain.contract.UserCart;
import shop.purchase.internal.domain.dto.CatalogMovieDto;
import shop.purchase.internal.domain.dto.PurchaseDto;
import shop.purchase.internal.domain.dto.PurchaseLineDto;
import shop.purchase.internal.domain.exception.EmptyCartException;
import shop.purchase.internal.domain.exception.MovieNoLongerAvailableException;
import shop.purchase.internal.domain.exception.MovieWithoutPriceException;
import shop.purchase.internal.domain.exception.PurchaseNotFoundException;
import shop.purchase.internal.domain.mapper.PurchaseMapper;
import shop.purchase.internal.domain.model.PurchaseStatus;
import shop.purchase.internal.domain.repository.PurchaseRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Purchase rules with the repository, the cart and the catalog mocked; the MapStruct mapper is the real one. */
class PurchaseServiceTest {

    private static final CatalogMovieDto MATRIX = new CatalogMovieDto(10L, "Matrix", new BigDecimal("7.99"));
    private static final CatalogMovieDto INCEPTION = new CatalogMovieDto(11L, "Inception", new BigDecimal("9.49"));

    private PurchaseRepository purchases;
    private UserCart carts;
    private MovieCatalog movies;
    private PurchaseService service;

    @BeforeEach
    void setUp() {
        purchases = mock(PurchaseRepository.class);
        carts = mock(UserCart.class);
        movies = mock(MovieCatalog.class);
        service = new PurchaseServiceImpl(purchases, carts, movies, Mappers.getMapper(PurchaseMapper.class));
    }

    /** The repository answers as the real one: it assigns the id of the purchase and of each line. */
    private void repositoryAssignsIds() {
        when(purchases.save(any())).thenAnswer(call -> {
            PurchaseDto p = call.getArgument(0);
            List<PurchaseLineDto> lines = new ArrayList<>();
            for (PurchaseLineDto l : p.lines()) {
                lines.add(new PurchaseLineDto(100L + lines.size(), 50L, l.movieId(), l.title(), l.unitPrice()));
            }
            return new PurchaseDto(50L, p.userId(), p.purchasedAt(), p.total(), p.status(), lines);
        });
    }

    private void cartWithTwoMovies(Long userId) {
        when(carts.findMovieIds(userId)).thenReturn(List.of(10L, 11L));
        when(movies.findByIds(List.of(10L, 11L))).thenReturn(List.of(MATRIX, INCEPTION));
    }

    private static PurchaseDto stored(Long id, Long userId) {
        return new PurchaseDto(id, userId, LocalDateTime.of(2026, 11, 2, 10, 15, 30), new BigDecimal("7.99"),
                PurchaseStatus.COMPRADA, List.of(new PurchaseLineDto(100L, id, 10L, "Matrix", new BigDecimal("7.99"))));
    }

    @Test
    void checkoutSavesPurchaseWithTitlePriceSnapshotAndTotalAndThenClearsTheCart() {
        cartWithTwoMovies(1L);
        repositoryAssignsIds();

        PurchaseDto purchase = service.checkout(1L);

        ArgumentCaptor<PurchaseDto> saved = ArgumentCaptor.forClass(PurchaseDto.class);
        verify(purchases).save(saved.capture());
        assertNull(saved.getValue().id());
        assertEquals(1L, saved.getValue().userId());
        assertEquals(PurchaseStatus.COMPRADA, saved.getValue().status());
        assertNotNull(saved.getValue().purchasedAt());
        assertEquals(new BigDecimal("17.48"), saved.getValue().total());
        assertEquals(List.of(
                new PurchaseLineDto(null, null, 10L, "Matrix", new BigDecimal("7.99")),
                new PurchaseLineDto(null, null, 11L, "Inception", new BigDecimal("9.49"))), saved.getValue().lines());

        assertEquals(50L, purchase.id());
        assertEquals(1L, purchase.userId());
        assertEquals(new BigDecimal("17.48"), purchase.total());
        assertEquals(List.of("Matrix", "Inception"), purchase.lines().stream().map(PurchaseLineDto::title).toList());
        assertEquals(List.of(100L, 101L), purchase.lines().stream().map(PurchaseLineDto::id).toList());

        InOrder order = inOrder(purchases, carts);
        order.verify(purchases).save(any());
        order.verify(carts).clear(1L);
    }

    @Test
    void checkoutUsesTheCurrentCatalogPriceNotTheOneSeenWhenAddingToTheCart() {
        when(carts.findMovieIds(1L)).thenReturn(List.of(10L, 11L));
        when(movies.findByIds(List.of(10L, 11L)))
                .thenReturn(List.of(new CatalogMovieDto(10L, "Matrix", new BigDecimal("10.00")), INCEPTION));
        repositoryAssignsIds();

        PurchaseDto purchase = service.checkout(1L);

        assertEquals(new BigDecimal("10.00"), purchase.lines().get(0).unitPrice());
        assertEquals(new BigDecimal("19.49"), purchase.total());
    }

    @Test
    void linesFollowTheOrderOfTheCartWhateverTheOrderTheCatalogAnswersIn() {
        when(carts.findMovieIds(1L)).thenReturn(List.of(11L, 10L));
        when(movies.findByIds(List.of(11L, 10L))).thenReturn(List.of(MATRIX, INCEPTION));
        repositoryAssignsIds();

        PurchaseDto purchase = service.checkout(1L);

        assertEquals(List.of(11L, 10L), purchase.lines().stream().map(PurchaseLineDto::movieId).toList());
    }

    @Test
    void singleMovieWithPriceZeroIsPurchasableAndTheTotalIsZero() {
        when(carts.findMovieIds(1L)).thenReturn(List.of(12L));
        when(movies.findByIds(List.of(12L))).thenReturn(List.of(new CatalogMovieDto(12L, "Free", new BigDecimal("0.00"))));
        repositoryAssignsIds();

        PurchaseDto purchase = service.checkout(1L);

        assertEquals(new BigDecimal("0.00"), purchase.total());
        assertEquals(1, purchase.lines().size());
        verify(carts).clear(1L);
    }

    @Test
    void checkoutWithEmptyCartIsConflictAndWritesNothing() {
        when(carts.findMovieIds(1L)).thenReturn(List.of());

        var ex = assertThrows(EmptyCartException.class, () -> service.checkout(1L));

        assertEquals("El carrito está vacío", ex.getMessage());
        verifyNoInteractions(movies, purchases);
        verify(carts, never()).clear(any());
    }

    @Test
    void checkoutWithVanishedMovieIsConflictAndLeavesTheCartIntact() {
        when(carts.findMovieIds(1L)).thenReturn(List.of(10L, 11L));
        when(movies.findByIds(List.of(10L, 11L))).thenReturn(List.of(MATRIX));

        var ex = assertThrows(MovieNoLongerAvailableException.class, () -> service.checkout(1L));

        assertTrue(ex.getMessage().contains("11"));
        verify(purchases, never()).save(any());
        verify(carts, never()).clear(any());
    }

    @Test
    void checkoutWithMovieWithoutPriceIsConflictAndLeavesTheCartIntact() {
        when(carts.findMovieIds(1L)).thenReturn(List.of(10L, 11L));
        when(movies.findByIds(List.of(10L, 11L)))
                .thenReturn(List.of(new CatalogMovieDto(10L, "Matrix", null), INCEPTION));

        var ex = assertThrows(MovieWithoutPriceException.class, () -> service.checkout(1L));

        assertTrue(ex.getMessage().contains("10"));
        verify(purchases, never()).save(any());
        verify(carts, never()).clear(any());
    }

    /**
     * The lost optimistic lock is not translated here: it propagates as it is so the transaction rolls back and
     * {@code GlobalExceptionHandler} answers 409 (see GlobalExceptionHandlerTest and ConcurrentCheckoutIntegrationTest).
     */
    @Test
    void optimisticLockOnClearPropagatesUntranslated() {
        cartWithTwoMovies(1L);
        repositoryAssignsIds();
        doThrow(new OptimisticLockingFailureException("Cart of user 1 was updated by another transaction"))
                .when(carts).clear(1L);

        assertThrows(OptimisticLockingFailureException.class, () -> service.checkout(1L));
    }

    @Test
    void historyReturnsTheOwnPurchasesAsTheRepositoryOrdersThemNewestFirst() {
        when(purchases.findByUserIdOrderByPurchasedAtDesc(1L)).thenReturn(List.of(stored(52L, 1L), stored(51L, 1L)));
        when(purchases.findByUserIdOrderByPurchasedAtDesc(2L)).thenReturn(List.of());

        assertEquals(List.of(stored(52L, 1L), stored(51L, 1L)), service.historyOf(1L));
        assertTrue(service.historyOf(2L).isEmpty());
    }

    @Test
    void detailReturnsTheStoredSnapshotWithoutAskingTheCatalogAgain() {
        when(purchases.findByIdAndUserId(50L, 1L)).thenReturn(Optional.of(stored(50L, 1L)));

        PurchaseDto detail = service.detailOf(1L, 50L);

        assertEquals(stored(50L, 1L), detail);
        assertEquals("Matrix", detail.lines().get(0).title());
        assertEquals(new BigDecimal("7.99"), detail.lines().get(0).unitPrice());
        verifyNoInteractions(movies, carts);
    }

    @Test
    void detailOfOthersOrUnknownPurchaseIsNotFound() {
        when(purchases.findByIdAndUserId(50L, 2L)).thenReturn(Optional.empty());
        when(purchases.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        var others = assertThrows(PurchaseNotFoundException.class, () -> service.detailOf(2L, 50L));
        var unknown = assertThrows(PurchaseNotFoundException.class, () -> service.detailOf(1L, 999L));

        assertEquals("La compra 50 no existe", others.getMessage());
        assertTrue(unknown.getMessage().contains("999"));
        verify(purchases).findByIdAndUserId(50L, 2L);
    }
}
