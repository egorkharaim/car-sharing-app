package mate.academy.service.stripe;

import com.stripe.model.Event;
import mate.academy.dto.payment.StripeSessionDto;
import mate.academy.model.payment.Payment;

public interface StripeService {

    StripeSessionDto createCheckoutSession(Payment payment);

    boolean isSessionPaid(String sessionId);

    Event constructWebhookEvent(String payload, String signatureHeader);
}
