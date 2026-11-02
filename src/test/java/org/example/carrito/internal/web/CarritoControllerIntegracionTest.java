package org.example.carrito.internal.web;

import com.jayway.jsonpath.JsonPath;
import org.example.compartido.seguridad.JwtTokens;
import org.example.pelicula.internal.domain.Pelicula;
import org.example.pelicula.internal.domain.PeliculaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CarritoControllerIntegracionTest {

    private static final String ALIEN = """
            {"titulo":"Alien","anio":1979,"genero":"Ciencia ficción","director":"Ridley Scott","precio":12.99}""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokens tokens;

    @Autowired
    private PeliculaRepository repositorioPeliculas;

    private final List<Long> peliculasCreadas = new ArrayList<>();

    /** La BD se comparte entre pruebas: se vacían los carritos y se borran las películas usadas. */
    @AfterEach
    void limpiar() throws Exception {
        for (long usuarioId : usuariosUsados) {
            mvc.perform(delete("/api/carrito").header("Authorization", bearer(usuarioId, "USUARIO")));
        }
        for (long id : peliculasCreadas) {
            mvc.perform(delete("/api/peliculas/" + id).header("Authorization", bearer(1, "ADMIN")));
        }
    }

    private final List<Long> usuariosUsados = new ArrayList<>();

    private String bearer(long usuarioId, String rol) {
        if (!usuariosUsados.contains(usuarioId) && rol.equals("USUARIO")) {
            usuariosUsados.add(usuarioId);
        }
        return "Bearer " + tokens.emitir(usuarioId, rol);
    }

    private long crearPelicula() throws Exception {
        String cuerpo = mvc.perform(post("/api/peliculas").header("Authorization", bearer(1, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(ALIEN))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
        peliculasCreadas.add(id);
        return id;
    }

    private long crearPeliculaSinPrecio() {
        Pelicula sinPrecio = new Pelicula();
        sinPrecio.setTitulo("Muda");
        Pelicula guardada = repositorioPeliculas.save(sinPrecio);
        peliculasCreadas.add(guardada.getId());
        return guardada.getId();
    }

    private ResultActions anadir(long usuarioId, long peliculaId) throws Exception {
        return mvc.perform(post("/api/carrito/lineas")
                .header("Authorization", bearer(usuarioId, "USUARIO"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"peliculaId\":" + peliculaId + "}"));
    }

    private ResultActions pedir(String ruta) throws Exception {
        return mvc.perform(get(ruta));
    }

    private ResultActions pedirConToken(long usuarioId, String ruta) throws Exception {
        return mvc.perform(get(ruta).header("Authorization", bearer(usuarioId, "USUARIO")));
    }

    private ResultActions quitar(long usuarioId, long peliculaId) throws Exception {
        return quitarConValor(usuarioId, "/api/carrito/lineas/" + peliculaId);
    }

    private ResultActions quitarConValor(long usuarioId, String ruta) throws Exception {
        return mvc.perform(delete(ruta).header("Authorization", bearer(usuarioId, "USUARIO")));
    }

    @Test
    void elCarritoDeUnUsuarioSinCarritoEsVacioConTotalCero() throws Exception {
        pedirConToken(2001, "/api/carrito")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas").isEmpty())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void anadirDevuelve201YVerMuestraElPrecioVivoYElTotal() throws Exception {
        long pelicula = crearPelicula();

        anadir(2002, pelicula)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.peliculaId").value(pelicula))
                .andExpect(jsonPath("$.titulo").value("Alien"))
                .andExpect(jsonPath("$.precio").value(12.99));

        pedirConToken(2002, "/api/carrito")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.lineas[0].peliculaId").value(pelicula))
                .andExpect(jsonPath("$.lineas[0].precio").value(12.99))
                .andExpect(jsonPath("$.total").value(12.99));

        String editada = """
                {"titulo":"Alien","anio":1979,"genero":"Ciencia ficción","director":"Ridley Scott","precio":15.50}""";
        mvc.perform(put("/api/peliculas/" + pelicula).header("Authorization", bearer(1, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(editada))
                .andExpect(status().isOk());

        pedirConToken(2002, "/api/carrito")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas[0].precio").value(15.50))
                .andExpect(jsonPath("$.total").value(15.50));
    }

    @Test
    void anadirDosVecesLaMismaPeliculaDevuelve409ConElCuerpoDeError() throws Exception {
        long pelicula = crearPelicula();
        anadir(2003, pelicula).andExpect(status().isCreated());

        anadir(2003, pelicula)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andExpect(jsonPath("$.errores").isMap());

        long otra = crearPelicula();
        anadir(2003, otra).andExpect(status().isCreated());
    }

    @Test
    void anadirUnaPeliculaInexistenteDevuelve404() throws Exception {
        anadir(2004, 99999999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404))
                .andExpect(jsonPath("$.mensaje").isNotEmpty());
    }

    @Test
    void anadirUnaPeliculaSinPrecioDevuelve409() throws Exception {
        long pelicula = crearPeliculaSinPrecio();

        anadir(2005, pelicula).andExpect(status().isConflict());
    }

    @Test
    void unCuerpoSinIdentificadorValidoDevuelve400() throws Exception {
        mvc.perform(post("/api/carrito/lineas").header("Authorization", bearer(2006, "USUARIO"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.errores.peliculaId").isNotEmpty());

        mvc.perform(post("/api/carrito/lineas").header("Authorization", bearer(2006, "USUARIO"))
                        .contentType(MediaType.APPLICATION_JSON).content("no es json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void quitarUnaLineaDevuelve204YElCarritoQuedaVacio() throws Exception {
        long pelicula = crearPelicula();
        anadir(2007, pelicula).andExpect(status().isCreated());

        quitar(2007, pelicula).andExpect(status().isNoContent());

        pedirConToken(2007, "/api/carrito")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas").isEmpty())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void quitarUnaPeliculaQueNoEstaEnElCarritoDevuelve404() throws Exception {
        quitar(2008, 99999999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404));
    }

    @Test
    void vaciarElCarritoDevuelve204YEsIdempotente() throws Exception {
        long primera = crearPelicula();
        long segunda = crearPelicula();
        anadir(2009, primera).andExpect(status().isCreated());
        anadir(2009, segunda).andExpect(status().isCreated());

        quitarConValor(2009, "/api/carrito").andExpect(status().isNoContent());

        pedirConToken(2009, "/api/carrito").andExpect(jsonPath("$.lineas").isEmpty());

        quitarConValor(2009, "/api/carrito").andExpect(status().isNoContent());
    }

    @Test
    void cadaUsuarioVeSoloSuCarrito() throws Exception {
        long primera = crearPelicula();
        long segunda = crearPelicula();
        anadir(2010, primera).andExpect(status().isCreated());

        pedirConToken(2011, "/api/carrito")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas").isEmpty());

        anadir(2011, segunda).andExpect(status().isCreated());

        pedirConToken(2010, "/api/carrito")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.lineas[0].peliculaId").value(primera));
    }

    @Test
    void eliminarUnaPeliculaQuitaSusLineasDeLosCarritos() throws Exception {
        long pelicula = crearPelicula();
        anadir(2012, pelicula).andExpect(status().isCreated());

        mvc.perform(delete("/api/peliculas/" + pelicula).header("Authorization", bearer(1, "ADMIN")))
                .andExpect(status().isNoContent());
        peliculasCreadas.remove((Long) pelicula);

        pedirConToken(2012, "/api/carrito")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas").isEmpty());
    }

    @Test
    void sinTokenTodasLasRutasDelCarritoDevuelven401() throws Exception {
        pedir("/api/carrito").andExpect(status().isUnauthorized());
        mvc.perform(post("/api/carrito/lineas")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"peliculaId\":1}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/carrito/lineas/1")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/carrito")).andExpect(status().isUnauthorized());
    }

    @Test
    void unUsuarioIdEnElCuerpoSeIgnoraYLaLineaVaAlCarritoDelToken() throws Exception {
        long pelicula = crearPelicula();

        mvc.perform(post("/api/carrito/lineas").header("Authorization", bearer(2013, "USUARIO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"peliculaId\":" + pelicula + ",\"usuarioId\":2014}"))
                .andExpect(status().isCreated());

        pedirConToken(2013, "/api/carrito").andExpect(jsonPath("$.lineas", org.hamcrest.Matchers.hasSize(1)));
        pedirConToken(2014, "/api/carrito").andExpect(jsonPath("$.lineas").isEmpty());
    }
}
