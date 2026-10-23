package mate.academy.repository.car;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import mate.academy.model.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CarRepository extends JpaRepository<Car, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Car c where c.id = :id")
    Optional<Car> findByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("""
            UPDATE Car c
            SET c.inventory = c.inventory - 1
            WHERE c.id = :id AND c.inventory > 0
            """)
    int decreaseInventoryIfAvailable(@Param("id") Long id);
}
