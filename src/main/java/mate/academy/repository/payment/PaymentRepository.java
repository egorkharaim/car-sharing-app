package mate.academy.repository.payment;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import mate.academy.model.payment.Payment;
import mate.academy.model.payment.PaymentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Page<Payment> findAllByRentalUserId(Long userId, Pageable pageable);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Payment> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Payment> findBySessionId(String sessionId);

    boolean existsByRentalIdAndType(Long rentalId, PaymentType type);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Payment> findByIdAndRentalUserId(Long paymentId, Long userId);
}
