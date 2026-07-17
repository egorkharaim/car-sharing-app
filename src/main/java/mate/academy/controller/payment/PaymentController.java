package mate.academy.controller.payment;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mate.academy.dto.payment.CreatePaymentRequestDto;
import mate.academy.dto.payment.PaymentDto;
import mate.academy.model.user.User;
import mate.academy.service.payment.PaymentService;
import mate.academy.service.user.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment and Stripe integration endpoints")
public class PaymentController {
    private static final String CANCELED_PAYMENT_MESSAGE =
            "Payment was canceled. You can complete it later, "
                    + "but the Stripe session is available for 24 hours.";

    private final PaymentService paymentService;
    private final UserService userService;

    @Operation(summary = "Create Stripe checkout session for existing payment")
    @PostMapping
    public PaymentDto createPaymentSession(
            @RequestBody @Valid CreatePaymentRequestDto requestDto,
            Authentication authentication
    ) {
        User currentUser = userService.getByEmail(authentication.getName());
        return paymentService.createPaymentSession(requestDto.paymentId(), currentUser);
    }

    @Operation(summary = "Get payments with optional user filter")
    @GetMapping
    public Page<PaymentDto> getPayments(
            @RequestParam(required = false) Long userId,
            Pageable pageable,
            Authentication authentication
    ) {
        User currentUser = userService.getByEmail(authentication.getName());
        return paymentService.getPayments(userId, currentUser, pageable);
    }

    @Operation(summary = "Handle successful Stripe redirect")
    @GetMapping("/success")
    public PaymentDto handleSuccessfulPayment(@RequestParam("session_id") String sessionId) {
        return paymentService.handleSuccessfulPayment(sessionId);
    }

    @Operation(summary = "Handle canceled Stripe redirect")
    @GetMapping("/cancel")
    public ResponseEntity<String> handleCanceledPayment(
            @RequestParam("session_id") String sessionId
    ) {
        paymentService.handleCanceledPayment(sessionId);
        return ResponseEntity.ok(CANCELED_PAYMENT_MESSAGE);
    }

    @Hidden
    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signatureHeader
    ) {
        paymentService.handleStripeWebhook(payload, signatureHeader);
        return ResponseEntity.ok("Webhook processed");
    }
}
