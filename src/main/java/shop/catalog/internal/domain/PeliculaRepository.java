package shop.catalog.internal.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PeliculaRepository extends JpaRepository<Pelicula, Long> {

    /** Los parámetros {@code null} no filtran; el título se busca por fragmento sin distinguir mayúsculas. */
    @Query("""
            select p from Pelicula p
            where (:titulo is null or lower(p.titulo) like lower(concat('%', cast(:titulo as string), '%')))
              and (:genero is null or p.genero = :genero)
              and (:anio is null or p.anio = :anio)
              and (:director is null or p.director = :director)
            """)
    Page<Pelicula> buscar(@Param("titulo") String titulo, @Param("genero") String genero,
                          @Param("anio") Integer anio, @Param("director") String director, Pageable paginacion);
}
