package mate.academy.service.payment;

import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import mate.academy.dto.payment.PaymentDto;
import mate.academy.dto.payment.StripeSessionDto;
import mate.academy.exception.EntityNotFoundException;
import mate.academy.exception.PaymentProcessingException;
import mate.academy.mapper.PaymentMapper;
import mate.academy.model.Rental;
import mate.academy.model.payment.Payment;
import mate.academy.model.payment.PaymentStatus;
import mate.academy.model.payment.PaymentType;
import mate.academy.model.user.RoleName;
import mate.academy.model.user.User;
import mate.academy.repository.payment.PaymentRepository;
import mate.academy.repository.rental.RentalRepository;
import mate.academy.service.notification.NotificationMessageBuilder;
import mate.academy.service.notification.NotificationService;
import mate.academy.service.stripe.StripeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private static final String CHECKOUT_SESSION_COMPLETED = "checkout.session.completed";

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final RentalRepository rentalRepository;
    private final StripeService stripeService;
    private final NotificationService notificationService;
    private final NotificationMessageBuilder notificationMessageBuilder;

    @Override
    public PaymentDto createRentalPayment(Rental rental) {
        validateRentalExists(rental);
        if (paymentRepository.existsByRentalIdAndType(
                rental.getId(),
                PaymentType.PAYMENT
        )) {
            throw new PaymentProcessingException(
                    "Rental payment already exists for rental id: " + rental.getId()
            );
        }

        Payment payment = new Payment();
        payment.setRental(rental);
        payment.setType(PaymentType.PAYMENT);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmountToPay(calculatePlannedAmount(rental));
        return paymentMapper.toDto(paymentRepository.save(payment));
    }

    @Override
    public PaymentDto createFinePayment(Rental rental) {
        validateRentalExists(rental);
        if (rental.getActualReturnDate() == null) {
            throw new PaymentProcessingException("Fine can't be created before rental return");
        }
        if (!rental.getActualReturnDate().isAfter(rental.getReturnDate())) {
            throw new PaymentProcessingException("Fine can be created only for overdue rentals");
        }
        if (paymentRepository.existsByRentalIdAndType(
                rental.getId(),
                PaymentType.FINE
        )) {
            throw new PaymentProcessingException(
                    "Fine payment already exists for rental id: " + rental.getId()
            );
        }

        Payment payment = new Payment();
        payment.setRental(rental);
        payment.setType(PaymentType.FINE);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmountToPay(calculateFineAmount(rental));
        return paymentMapper.toDto(paymentRepository.save(payment));
    }

    @Override
    public PaymentDto createPaymentSession(Long paymentId, User currentUser) {
        Payment payment = getPaymentByIdWithAccessCheck(paymentId, currentUser);

        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new PaymentProcessingException("Payment is already completed");
        }
        if (payment.getSessionId() != null && payment.getSessionUrl() != null) {
            return paymentMapper.toDto(payment);
        }

        StripeSessionDto stripeSession = stripeService.createCheckoutSession(payment);
        payment.setSessionId(stripeSession.sessionId());
        payment.setSessionUrl(stripeSession.sessionUrl());

        return paymentMapper.toDto(paymentRepository.save(payment));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentDto> getPayments(Long userId, User currentUser, Pageable pageable) {
        if (isManager(currentUser)) {
            if (userId != null) {
                return paymentRepository.findAllByRentalUserId(userId, pageable)
                        .map(paymentMapper::toDto);
            }
            return paymentRepository.findAll(pageable)
                    .map(paymentMapper::toDto);
        }

        return paymentRepository.findAllByRentalUserId(currentUser.getId(), pageable)
                .map(paymentMapper::toDto);
    }

    @Override
    public PaymentDto handleSuccessfulPayment(String sessionId) {
        Payment payment = findBySessionId(sessionId);
        if (payment.getStatus() == PaymentStatus.PAID) {
            return paymentMapper.toDto(payment);
        }
        if (!stripeService.isSessionPaid(sessionId)) {
            throw new PaymentProcessingException(
                    "Payment was not completed for session id: " + sessionId
            );
        }
        Payment savedPayment = markPaymentAsPaid(payment);
        return paymentMapper.toDto(savedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDto handleCanceledPayment(String sessionId) {
        Payment payment = findBySessionId(sessionId);
        return paymentMapper.toDto(payment);
    }

    @Override
    public void handleStripeWebhook(String payload, String signatureHeader) {
        Event webhookEvent = stripeService.constructWebhookEvent(payload, signatureHeader);
        if (!CHECKOUT_SESSION_COMPLETED.equals(webhookEvent.getType())) {
            return;
        }

        StripeObject stripeObject;
        try {
            stripeObject = webhookEvent.getDataObjectDeserializer().deserializeUnsafe();
        } catch (EventDataObjectDeserializationException e) {
            throw new PaymentProcessingException(
                    "Can't deserialize Stripe webhook checkout session",
                    e
            );
        }

        if (!(stripeObject instanceof Session session)) {
            throw new PaymentProcessingException(
                    "Stripe webhook does not contain a checkout session object"
            );
        }

        Payment payment = findBySessionId(session.getId());
        if (payment.getStatus() == PaymentStatus.PAID) {
            return;
        }

        markPaymentAsPaid(payment);
    }

    private void validateRentalExists(Rental rental) {
        if (rental == null
                || rental.getId() == null
                || !rentalRepository.existsById(rental.getId())) {
            throw new EntityNotFoundException(
                    "Can't find rental by id: " + (rental == null ? null : rental.getId())
            );
        }
    }

    private Payment findBySessionId(String sessionId) {
        return paymentRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Can't find payment by session id: " + sessionId
                ));
    }

    private Payment getPaymentByIdWithAccessCheck(Long paymentId, User currentUser) {
        if (isManager(currentUser)) {
            return paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Can't find payment by id: " + paymentId
                    ));
        }

        return paymentRepository.findByIdAndRentalUserId(paymentId, currentUser.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Can't find payment by id: " + paymentId
                ));
    }

    private boolean isManager(User currentUser) {
        return currentUser.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.MANAGER);
    }

    private BigDecimal calculatePlannedAmount(Rental rental) {
        long plannedDays = ChronoUnit.DAYS.between(rental.getRentalDate(), rental.getReturnDate());
        if (plannedDays <= 0) {
            plannedDays = 1;
        }
        return rental.getCar().getDailyFee().multiply(BigDecimal.valueOf(plannedDays));
    }

    private BigDecimal calculateFineAmount(Rental rental) {
        long overdueDays = ChronoUnit.DAYS.between(
                rental.getReturnDate(),
                rental.getActualReturnDate()
        );
        if (overdueDays <= 0) {
            throw new PaymentProcessingException("Overdue days must be positive to create a fine");
        }
        return rental.getCar().getDailyFee()
                .multiply(BigDecimal.valueOf(overdueDays))
                .multiply(BigDecimal.valueOf(2));
    }

    private Payment markPaymentAsPaid(Payment payment) {
        payment.setStatus(PaymentStatus.PAID);
        Payment savedPayment = paymentRepository.save(payment);
        notificationService.sendMessage(
                notificationMessageBuilder.buildSuccessfulPaymentMessage(savedPayment)
        );
        return savedPayment;
    }
}
