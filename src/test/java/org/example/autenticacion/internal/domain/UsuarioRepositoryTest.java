package org.example.autenticacion.internal.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository repositorio;

    private Usuario usuario(String email, Rol rol) {
        var usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setContrasena("hash-bcrypt");
        usuario.setRol(rol);
        return usuario;
    }

    @Test
    void guardaUsuarioYAsignaId() {
        var guardado = repositorio.saveAndFlush(usuario("ana@correo.com", Rol.USUARIO));

        assertNotNull(guardado.getId());
    }

    @Test
    void buscaPorEmailConSusDatos() {
        repositorio.saveAndFlush(usuario("ana@correo.com", Rol.ADMIN));

        var encontrado = repositorio.findByEmail("ana@correo.com").orElseThrow();

        assertEquals("hash-bcrypt", encontrado.getContrasena());
        assertEquals(Rol.ADMIN, encontrado.getRol());
    }

    @Test
    void buscarEmailInexistenteDevuelveVacio() {
        assertTrue(repositorio.findByEmail("nadie@correo.com").isEmpty());
    }

    @Test
    void existsByEmailDistingueRegistrados() {
        repositorio.saveAndFlush(usuario("ana@correo.com", Rol.USUARIO));

        assertTrue(repositorio.existsByEmail("ana@correo.com"));
        assertFalse(repositorio.existsByEmail("otra@correo.com"));
    }

    @Test
    void elEmailEsUnico() {
        repositorio.saveAndFlush(usuario("ana@correo.com", Rol.USUARIO));

        assertThrows(DataIntegrityViolationException.class,
                () -> repositorio.saveAndFlush(usuario("ana@correo.com", Rol.USUARIO)));
    }
}
