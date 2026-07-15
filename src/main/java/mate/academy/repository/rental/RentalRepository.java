package mate.academy.repository.rental;

import java.time.LocalDate;
import java.util.List;
import mate.academy.model.Rental;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentalRepository extends JpaRepository<Rental, Long> {
    Page<Rental> findAllByUserId(Long userId, Pageable pageable);

    Page<Rental> findAllByUserIdAndActualReturnDateIsNull(Long userId, Pageable pageable);

    Page<Rental> findAllByUserIdAndActualReturnDateIsNotNull(Long userId, Pageable pageable);

    boolean existsByUserIdAndActualReturnDateIsNull(Long userId);

    List<Rental> findAllByActualReturnDateIsNullAndReturnDateLessThanEqual(LocalDate returnDate);
}
