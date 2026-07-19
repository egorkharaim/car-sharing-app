package mate.academy.service.stripe;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import java.math.BigDecimal;
import mate.academy.dto.payment.StripeSessionDto;
import mate.academy.exception.PaymentProcessingException;
import mate.academy.model.payment.Payment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeServiceImpl implements StripeService {
    private static final String PAYMENT_STATUS_PAID = "paid";
    private static final long SESSION_ITEM_QUANTITY = 1L;

    private final StripeClient stripeClient;
    private final String currency;
    private final String successUrl;
    private final String cancelUrl;
    private final String webhookSecret;

    public StripeServiceImpl(
            @Value("${stripe.secret-key}") String secretKey,
            @Value("${stripe.currency}") String currency,
            @Value("${stripe.success-url}") String successUrl,
            @Value("${stripe.cancel-url}") String cancelUrl,
            @Value("${stripe.webhook-secret}") String webhookSecret
    ) {
        this.stripeClient = new StripeClient(secretKey);
        this.currency = currency;
        this.successUrl = successUrl;
        this.cancelUrl = cancelUrl;
        this.webhookSecret = webhookSecret;
    }

    @Override
    public StripeSessionDto createCheckoutSession(Payment payment) {
        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .setClientReferenceId(payment.getId().toString())
                    .setCustomerEmail(payment.getRental().getUser().getEmail())
                    .putMetadata("paymentId", payment.getId().toString())
                    .putMetadata("rentalId", payment.getRental().getId().toString())
                    .putMetadata("paymentType", payment.getType().name())
                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(SESSION_ITEM_QUANTITY)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency(currency)
                                                    .setUnitAmount(
                                                            toMinorUnits(payment.getAmountToPay())
                                                    )
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData
                                                                    .ProductData.builder()
                                                                    .setName(
                                                                            buildProductName(
                                                                                    payment
                                                                            )
                                                                    )
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            Session session = stripeClient.v1().checkout()
                    .sessions()
                    .create(params);

            return new StripeSessionDto(session.getId(), session.getUrl());
        } catch (StripeException e) {
            throw new PaymentProcessingException(
                    "Can't create Stripe checkout session for payment id: "
                    + payment.getId(),
                    e
            );
        }
    }

    @Override
    public boolean isSessionPaid(String sessionId) {
        try {
            Session session = stripeClient.v1().checkout()
                    .sessions()
                    .retrieve(sessionId);

            return PAYMENT_STATUS_PAID.equals(session.getPaymentStatus());
        } catch (StripeException e) {
            throw new PaymentProcessingException(
                    "Can't verify Stripe session by id: " + sessionId,
                    e
            );
        }
    }

    @Override
    public Event constructWebhookEvent(String payload, String signatureHeader) {
        try {
            return stripeClient.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (Exception e) {
            throw new PaymentProcessingException("Can't construct Stripe webhook event", e);
        }
    }

    private long toMinorUnits(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }

    private String buildProductName(Payment payment) {
        return payment.getType().name() + " for rental #" + payment.getRental().getId();
    }

}
