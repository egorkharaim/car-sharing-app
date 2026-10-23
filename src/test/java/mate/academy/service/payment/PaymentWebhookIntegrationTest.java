package mate.academy.service.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import mate.academy.config.AbstractIntegrationTest;
import mate.academy.exception.PaymentProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = "classpath:database/controller/add-controller-data.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:database/controller/clear-controller-data.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class PaymentWebhookIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Test
    @DisplayName("Signed paid webhook updates real database and tolerates repeated delivery")
    void webhook_SignedPaidSession_MarksPaid() throws Exception {
        // Given
        String payload = payload("paid");
        String signature = signature(payload);

        // When
        paymentService.handleStripeWebhook(payload, signature);
        paymentService.handleStripeWebhook(payload, signature);

        // Then
        assertEquals("PAID", status());
    }

    @Test
    @DisplayName("Signed unpaid webhook leaves real payment pending")
    void webhook_UnpaidSession_LeavesPending() throws Exception {
        // Given
        String payload = payload("unpaid");

        // When
        paymentService.handleStripeWebhook(payload, signature(payload));

        // Then
        assertEquals("PENDING", status());
    }

    @Test
    @DisplayName("Invalid webhook signature cannot change real payment")
    void webhook_InvalidSignature_RejectsEvent() {
        // Given
        String payload = payload("paid");

        // When
        assertThrows(PaymentProcessingException.class,
                () -> paymentService.handleStripeWebhook(payload, "invalid"));

        // Then
        assertEquals("PENDING", status());
    }

    @Test
    @DisplayName("Concurrent signed webhooks complete without duplicate state transitions")
    void webhook_ConcurrentDelivery_MarksPaid() throws Exception {
        // Given
        String payload = payload("paid");
        String signature = signature(payload);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        java.util.concurrent.Callable<Void> deliver = () -> {
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Webhook test did not start");
            }
            paymentService.handleStripeWebhook(payload, signature);
            return null;
        };

        // When
        try {
            var first = executor.submit(deliver);
            var second = executor.submit(deliver);
            start.countDown();
            first.get(30, TimeUnit.SECONDS);
            second.get(30, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        // Then
        assertEquals("PAID", status());
    }

    private String status() {
        return jdbcTemplate.queryForObject(
                "select status from payments where id = 1", String.class);
    }

    private String payload(String paymentStatus) {
        return """
                {"id":"evt_integration","object":"event","type":"checkout.session.completed",
                "data":{"object":{"id":"session-1","object":"checkout.session",
                "payment_status":"%s"}}}
                """.formatted(paymentStatus);
    }

    private String signature(String payload) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8));
        return "t=" + timestamp + ",v1=" + HexFormat.of().formatHex(hash);
    }
}
