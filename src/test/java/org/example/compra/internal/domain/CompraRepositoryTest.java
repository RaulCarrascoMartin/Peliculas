package org.example.compra.internal.domain;

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
class CompraRepositoryTest {

    @Autowired
    private CompraRepository repositorio;

    private Compra compra(Long usuarioId, LocalDateTime fecha) {
        var compra = new Compra();
        compra.setUsuarioId(usuarioId);
        compra.setFecha(fecha);
        compra.setTotal(new BigDecimal("19.98"));
        compra.setEstado(EstadoCompra.COMPRADA);
        return compra;
    }

    @Test
    void guardaLaCompraConEstadoYTotal() {
        var guardada = repositorio.saveAndFlush(compra(1L, LocalDateTime.of(2026, 11, 2, 9, 0)));

        var leida = repositorio.findById(guardada.getId()).orElseThrow();
        assertEquals(EstadoCompra.COMPRADA, leida.getEstado());
        assertEquals(new BigDecimal("19.98"), leida.getTotal());
        assertEquals(1L, leida.getUsuarioId());
    }

    @Test
    void listaLasComprasDeUnUsuarioPorFechaDescendente() {
        var antigua = repositorio.saveAndFlush(compra(1L, LocalDateTime.of(2026, 11, 1, 9, 0)));
        var reciente = repositorio.saveAndFlush(compra(1L, LocalDateTime.of(2026, 11, 3, 9, 0)));
        var intermedia = repositorio.saveAndFlush(compra(1L, LocalDateTime.of(2026, 11, 2, 9, 0)));
        repositorio.saveAndFlush(compra(2L, LocalDateTime.of(2026, 11, 4, 9, 0)));

        List<Long> ids = repositorio.findByUsuarioIdOrderByFechaDesc(1L).stream().map(Compra::getId).toList();

        assertEquals(List.of(reciente.getId(), intermedia.getId(), antigua.getId()), ids);
    }

    @Test
    void laCompraAjenaNoSeEncuentraPorUsuario() {
        var ajena = repositorio.saveAndFlush(compra(2L, LocalDateTime.of(2026, 11, 2, 9, 0)));

        assertTrue(repositorio.findByIdAndUsuarioId(ajena.getId(), 1L).isEmpty());
        assertEquals(ajena.getId(), repositorio.findByIdAndUsuarioId(ajena.getId(), 2L).orElseThrow().getId());
    }

    @Test
    void exigeFechaTotalYEstado() {
        var sinFecha = compra(1L, null);
        assertThrows(DataIntegrityViolationException.class, () -> repositorio.saveAndFlush(sinFecha));
    }
}
