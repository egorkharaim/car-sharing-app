package mate.academy.repository.payment;

import java.util.Optional;
import mate.academy.model.payment.Payment;
import mate.academy.model.payment.PaymentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Page<Payment> findAllByRentalUserId(Long userId, Pageable pageable);

    Optional<Payment> findBySessionId(String sessionId);

    boolean existsByRentalIdAndType(Long rentalId, PaymentType type);

    Optional<Payment> findByIdAndRentalUserId(Long paymentId, Long userId);
}
