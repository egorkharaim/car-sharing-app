package mate.academy.service.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import mate.academy.dto.payment.PaymentDto;
import mate.academy.dto.payment.StripeSessionDto;
import mate.academy.exception.EntityNotFoundException;
import mate.academy.exception.PaymentProcessingException;
import mate.academy.mapper.PaymentMapper;
import mate.academy.model.Car;
import mate.academy.model.CarType;
import mate.academy.model.Rental;
import mate.academy.model.payment.Payment;
import mate.academy.model.payment.PaymentStatus;
import mate.academy.model.payment.PaymentType;
import mate.academy.model.user.Role;
import mate.academy.model.user.RoleName;
import mate.academy.model.user.User;
import mate.academy.repository.payment.PaymentRepository;
import mate.academy.repository.rental.RentalRepository;
import mate.academy.service.notification.NotificationMessageBuilder;
import mate.academy.service.notification.NotificationService;
import mate.academy.service.stripe.StripeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {
    private static final Long PAYMENT_ID = 1L;
    private static final Long RENTAL_ID = 10L;
    private static final Long USER_ID = 100L;
    private static final String SESSION_ID = "cs_test_123";
    private static final String SESSION_URL = "https://checkout.stripe.com/session";
    private static final String PAYMENT_MESSAGE = "Payment completed successfully";

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private RentalRepository rentalRepository;
    @Mock
    private StripeService stripeService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private NotificationMessageBuilder notificationMessageBuilder;
    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    @DisplayName("Create rental payment calculates planned amount and saves pending payment")
    void createRentalPayment_ValidRental_ReturnsPaymentDto() {
        // Given
        Rental rental = createRental();
        Payment savedPayment = createPayment(PaymentType.PAYMENT, PaymentStatus.PENDING);
        savedPayment.setAmountToPay(BigDecimal.valueOf(150));
        PaymentDto expected = createPaymentDto(savedPayment);
        when(rentalRepository.existsById(RENTAL_ID)).thenReturn(true);
        when(paymentRepository.existsByRentalIdAndType(RENTAL_ID, PaymentType.PAYMENT))
                .thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(paymentMapper.toDto(savedPayment)).thenReturn(expected);

        // When
        PaymentDto actual = paymentService.createRentalPayment(rental);

        // Then
        assertEquals(expected, actual);
        verify(rentalRepository).existsById(RENTAL_ID);
        verify(paymentRepository).existsByRentalIdAndType(RENTAL_ID, PaymentType.PAYMENT);
        verify(paymentRepository).save(argThat(payment ->
                payment.getType() == PaymentType.PAYMENT
                        && payment.getStatus() == PaymentStatus.PENDING
                        && BigDecimal.valueOf(150).compareTo(payment.getAmountToPay()) == 0
        ));
        verify(paymentMapper).toDto(savedPayment);
    }

    @Test
    @DisplayName("Create rental payment duplicate throws exception")
    void createRentalPayment_DuplicatePayment_ThrowsException() {
        // Given
        Rental rental = createRental();
        when(rentalRepository.existsById(RENTAL_ID)).thenReturn(true);
        when(paymentRepository.existsByRentalIdAndType(RENTAL_ID, PaymentType.PAYMENT))
                .thenReturn(true);

        // When
        PaymentProcessingException exception = assertThrows(
                PaymentProcessingException.class,
                () -> paymentService.createRentalPayment(rental)
        );

        // Then
        assertEquals(
                "Rental payment already exists for rental id: " + RENTAL_ID,
                exception.getMessage()
        );
        verify(rentalRepository).existsById(RENTAL_ID);
        verify(paymentRepository).existsByRentalIdAndType(RENTAL_ID, PaymentType.PAYMENT);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create fine payment for overdue rental saves fine amount")
    void createFinePayment_OverdueRental_ReturnsPaymentDto() {
        // Given
        Rental rental = createRental();
        rental.setReturnDate(LocalDate.now().minusDays(2));
        rental.setActualReturnDate(LocalDate.now());
        Payment savedPayment = createPayment(PaymentType.FINE, PaymentStatus.PENDING);
        savedPayment.setAmountToPay(BigDecimal.valueOf(200));
        PaymentDto expected = createPaymentDto(savedPayment);
        when(rentalRepository.existsById(RENTAL_ID)).thenReturn(true);
        when(paymentRepository.existsByRentalIdAndType(RENTAL_ID, PaymentType.FINE))
                .thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(paymentMapper.toDto(savedPayment)).thenReturn(expected);

        // When
        PaymentDto actual = paymentService.createFinePayment(rental);

        // Then
        assertEquals(expected, actual);
        verify(paymentRepository).save(argThat(payment ->
                payment.getType() == PaymentType.FINE
                        && payment.getStatus() == PaymentStatus.PENDING
                        && BigDecimal.valueOf(200).compareTo(payment.getAmountToPay()) == 0
        ));
        verify(paymentMapper).toDto(savedPayment);
    }

    @Test
    @DisplayName("Create payment session calls Stripe and stores session data")
    void createPaymentSession_PendingPaymentWithoutSession_ReturnsPaymentDto() {
        // Given
        User user = createUser(USER_ID, RoleName.CUSTOMER);
        Payment payment = createPayment(PaymentType.PAYMENT, PaymentStatus.PENDING);
        StripeSessionDto stripeSession = new StripeSessionDto(SESSION_ID, SESSION_URL);
        PaymentDto expected = createPaymentDto(payment);
        when(paymentRepository.findByIdAndRentalUserId(PAYMENT_ID, USER_ID))
                .thenReturn(Optional.of(payment));
        when(stripeService.createCheckoutSession(payment)).thenReturn(stripeSession);
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(paymentMapper.toDto(payment)).thenReturn(expected);

        // When
        PaymentDto actual = paymentService.createPaymentSession(PAYMENT_ID, user);

        // Then
        assertEquals(expected, actual);
        assertEquals(SESSION_ID, payment.getSessionId());
        assertEquals(SESSION_URL, payment.getSessionUrl());
        verify(paymentRepository).findByIdAndRentalUserId(PAYMENT_ID, USER_ID);
        verify(stripeService).createCheckoutSession(payment);
        verify(paymentRepository).save(payment);
        verify(paymentMapper).toDto(payment);
    }

    @Test
    @DisplayName("Handle successful payment checks Stripe and marks payment as paid")
    void handleSuccessfulPayment_PaidStripeSession_ReturnsPaidPaymentDto() {
        // Given
        Payment payment = createPayment(PaymentType.PAYMENT, PaymentStatus.PENDING);
        payment.setSessionId(SESSION_ID);
        PaymentDto expected = createPaymentDto(payment);
        when(paymentRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(payment));
        when(stripeService.isSessionPaid(SESSION_ID)).thenReturn(true);
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(notificationMessageBuilder.buildSuccessfulPaymentMessage(payment))
                .thenReturn(PAYMENT_MESSAGE);
        when(paymentMapper.toDto(payment)).thenReturn(expected);

        // When
        PaymentDto actual = paymentService.handleSuccessfulPayment(SESSION_ID);

        // Then
        assertEquals(expected, actual);
        assertEquals(PaymentStatus.PAID, payment.getStatus());
        verify(paymentRepository).findBySessionId(SESSION_ID);
        verify(stripeService).isSessionPaid(SESSION_ID);
        verify(paymentRepository).save(payment);
        verify(notificationMessageBuilder).buildSuccessfulPaymentMessage(payment);
        verify(notificationService).sendMessage(PAYMENT_MESSAGE);
        verify(paymentMapper).toDto(payment);
    }

    @Test
    @DisplayName("Handle successful payment with unpaid Stripe session throws exception")
    void handleSuccessfulPayment_UnpaidStripeSession_ThrowsException() {
        // Given
        Payment payment = createPayment(PaymentType.PAYMENT, PaymentStatus.PENDING);
        payment.setSessionId(SESSION_ID);
        when(paymentRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(payment));
        when(stripeService.isSessionPaid(SESSION_ID)).thenReturn(false);

        // When
        PaymentProcessingException exception = assertThrows(
                PaymentProcessingException.class,
                () -> paymentService.handleSuccessfulPayment(SESSION_ID)
        );

        // Then
        assertEquals(
                "Payment was not completed for session id: " + SESSION_ID,
                exception.getMessage()
        );
        verify(paymentRepository).findBySessionId(SESSION_ID);
        verify(stripeService).isSessionPaid(SESSION_ID);
        verify(paymentRepository, never()).save(payment);
    }

    @Test
    @DisplayName("Create session for missing customer payment throws not found exception")
    void createPaymentSession_MissingCustomerPayment_ThrowsException() {
        // Given
        User user = createUser(USER_ID, RoleName.CUSTOMER);
        when(paymentRepository.findByIdAndRentalUserId(PAYMENT_ID, USER_ID))
                .thenReturn(Optional.empty());

        // When
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> paymentService.createPaymentSession(PAYMENT_ID, user)
        );

        // Then
        assertEquals("Can't find payment by id: " + PAYMENT_ID, exception.getMessage());
        verify(paymentRepository).findByIdAndRentalUserId(PAYMENT_ID, USER_ID);
        verify(stripeService, never()).createCheckoutSession(any());
    }

    private Payment createPayment(PaymentType type, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(PAYMENT_ID);
        payment.setRental(createRental());
        payment.setType(type);
        payment.setStatus(status);
        payment.setAmountToPay(BigDecimal.valueOf(150));
        return payment;
    }

    private PaymentDto createPaymentDto(Payment payment) {
        return new PaymentDto(
                payment.getId(),
                payment.getStatus(),
                payment.getType(),
                payment.getRental().getId(),
                payment.getSessionUrl(),
                payment.getSessionId(),
                payment.getAmountToPay()
        );
    }

    private Rental createRental() {
        Rental rental = new Rental();
        rental.setId(RENTAL_ID);
        rental.setUser(createUser(USER_ID, RoleName.CUSTOMER));
        rental.setCar(createCar());
        rental.setRentalDate(LocalDate.now());
        rental.setReturnDate(LocalDate.now().plusDays(3));
        return rental;
    }

    private Car createCar() {
        Car car = new Car();
        car.setId(20L);
        car.setBrand("Toyota");
        car.setModel("Corolla");
        car.setType(CarType.SEDAN);
        car.setInventory(2);
        car.setDailyFee(BigDecimal.valueOf(50));
        return car;
    }

    private User createUser(Long id, RoleName roleName) {
        Role role = new Role();
        role.setId(roleName == RoleName.MANAGER ? 1L : 2L);
        role.setName(roleName);

        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setPassword("password");
        user.setRoles(Set.of(role));
        return user;
    }
}
