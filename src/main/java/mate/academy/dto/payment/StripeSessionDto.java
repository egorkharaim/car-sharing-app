package mate.academy.dto.payment;

public record StripeSessionDto(
        String sessionId,
        String sessionUrl
) {
}
