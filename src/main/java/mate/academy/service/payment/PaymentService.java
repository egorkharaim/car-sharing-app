package mate.academy.service.payment;

import mate.academy.dto.payment.PaymentDto;
import mate.academy.model.Rental;
import mate.academy.model.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {

    PaymentDto createRentalPayment(Rental rental);

    PaymentDto createFinePayment(Rental rental);

    PaymentDto createPaymentSession(Long paymentId, User currentUser);

    Page<PaymentDto> getPayments(Long userId, User currentUser, Pageable pageable);

    PaymentDto handleSuccessfulPayment(String sessionId);

    PaymentDto handleCanceledPayment(String sessionId);

    void handleStripeWebhook(String payload, String signatureHeader);
}
