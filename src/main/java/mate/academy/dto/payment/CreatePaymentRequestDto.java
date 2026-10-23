package mate.academy.dto.payment;

import jakarta.validation.constraints.NotNull;

public record CreatePaymentRequestDto(@NotNull Long paymentId) {
}
