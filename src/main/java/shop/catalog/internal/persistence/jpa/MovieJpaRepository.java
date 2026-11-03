package shop.catalog.internal.persistence.jpa;

import shop.catalog.internal.persistence.entity.MovieEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface MovieJpaRepository extends JpaRepository<MovieEntity, Long> {

    /** {@code null} parameters do not filter; the title is matched by fragment, ignoring case. */
    @Query("""
            select m from MovieEntity m
            where (:title is null or lower(m.title) like lower(concat('%', cast(:title as string), '%')))
              and (:genre is null or m.genre = :genre)
              and (:year is null or m.year = :year)
              and (:director is null or m.director = :director)
            """)
    Page<MovieEntity> search(@Param("title") String title, @Param("genre") String genre,
                       @Param("year") Integer year, @Param("director") String director, Pageable pageable);

    /** Subtracts units only if enough remain; returns the affected rows (0 if there was not enough stock). */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update MovieEntity m set m.stock = m.stock - :quantity where m.id = :id and m.stock >= :quantity")
    int decrementStock(@Param("id") Long id, @Param("quantity") int quantity);

    /** Adds units to the stock; returns the affected rows (0 if the movie no longer exists). */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update MovieEntity m set m.stock = m.stock + :quantity where m.id = :id")
    int incrementStock(@Param("id") Long id, @Param("quantity") int quantity);
}
