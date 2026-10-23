package mate.academy.repository.rental;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import mate.academy.model.Rental;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RentalRepository extends JpaRepository<Rental, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Rental r where r.id = :id")
    Optional<Rental> findByIdForUpdate(@Param("id") Long id);

    Page<Rental> findAllByActualReturnDateIsNull(Pageable pageable);

    Page<Rental> findAllByActualReturnDateIsNotNull(Pageable pageable);

    Page<Rental> findAllByUserId(Long userId, Pageable pageable);

    Page<Rental> findAllByUserIdAndActualReturnDateIsNull(Long userId, Pageable pageable);

    Page<Rental> findAllByUserIdAndActualReturnDateIsNotNull(Long userId, Pageable pageable);

    boolean existsByUserIdAndActualReturnDateIsNull(Long userId);

    @EntityGraph(attributePaths = {"car", "user"})
    List<Rental> findAllByActualReturnDateIsNullAndReturnDateLessThanEqual(LocalDate returnDate);
}
