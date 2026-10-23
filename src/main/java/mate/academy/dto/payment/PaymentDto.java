package mate.academy.dto.payment;

import java.math.BigDecimal;
import mate.academy.model.payment.PaymentStatus;
import mate.academy.model.payment.PaymentType;

public record PaymentDto(
        Long id,
        PaymentStatus status,
        PaymentType type,
        Long rentalId,
        String sessionUrl,
        String sessionId,
        BigDecimal amountToPay
) {
}
