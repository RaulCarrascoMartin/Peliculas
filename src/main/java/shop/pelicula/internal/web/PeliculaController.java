package shop.pelicula.internal.web;

import jakarta.validation.Valid;
import shop.pelicula.internal.usecase.BuscarPeliculas;
import shop.pelicula.internal.usecase.CrearPelicula;
import shop.pelicula.internal.usecase.EditarPelicula;
import shop.pelicula.internal.usecase.EliminarPelicula;
import shop.pelicula.internal.usecase.ObtenerPelicula;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/peliculas")
public class PeliculaController {

    private static final String SOLO_ADMIN = "hasRole('ADMIN')";

    private final BuscarPeliculas buscar;
    private final ObtenerPelicula obtener;
    private final CrearPelicula crear;
    private final EditarPelicula editar;
    private final EliminarPelicula eliminar;

    public PeliculaController(BuscarPeliculas buscar, ObtenerPelicula obtener, CrearPelicula crear,
                              EditarPelicula editar, EliminarPelicula eliminar) {
        this.buscar = buscar;
        this.obtener = obtener;
        this.crear = crear;
        this.editar = editar;
        this.eliminar = eliminar;
    }

    @GetMapping
    public PagedModel<PeliculaResponse> listar(@RequestParam(required = false) String titulo,
                                               @RequestParam(required = false) String genero,
                                               @RequestParam(required = false) Integer anio,
                                               @RequestParam(required = false) String director,
                                               @PageableDefault(sort = "id") Pageable paginacion) {
        return new PagedModel<>(buscar.ejecutar(titulo, genero, anio, director, paginacion)
                .map(PeliculaResponse::desde));
    }

    @GetMapping("/{id}")
    public PeliculaResponse detalle(@PathVariable Long id) {
        return PeliculaResponse.desde(obtener.ejecutar(id));
    }

    @PostMapping
    @PreAuthorize(SOLO_ADMIN)
    @ResponseStatus(HttpStatus.CREATED)
    public PeliculaResponse crear(@Valid @RequestBody PeliculaRequest peticion) {
        return PeliculaResponse.desde(crear.ejecutar(peticion.aDatos()));
    }

    @PutMapping("/{id}")
    @PreAuthorize(SOLO_ADMIN)
    public PeliculaResponse editar(@PathVariable Long id, @Valid @RequestBody PeliculaRequest peticion) {
        return PeliculaResponse.desde(editar.ejecutar(id, peticion.aDatos()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SOLO_ADMIN)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        eliminar.ejecutar(id);
    }
}
