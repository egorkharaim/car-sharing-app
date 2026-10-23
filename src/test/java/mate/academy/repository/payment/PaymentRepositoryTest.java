package mate.academy.repository.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mate.academy.model.payment.PaymentType;
import mate.academy.repository.AbstractRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(
        scripts = "classpath:database/repository/add-repository-data.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
        scripts = "classpath:database/repository/clear-repository-data.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class PaymentRepositoryTest extends AbstractRepositoryTest {
    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("Find payments belonging to a user")
    void findAllByRentalUserId_ExistingUser_ReturnsUserPayments() {
        // Given
        Long userId = 1L;

        // When
        Page<?> payments = paymentRepository.findAllByRentalUserId(userId, PageRequest.of(0, 10));

        // Then
        assertEquals(2, payments.getTotalElements());
    }

    @Test
    @DisplayName("Find payment by Stripe session id")
    void findBySessionId_ExistingSessionId_ReturnsPayment() {
        // Given
        String sessionId = "session-1";

        // When
        var actual = paymentRepository.findBySessionId(sessionId);

        // Then
        assertTrue(actual.isPresent());
        assertEquals(1L, actual.get().getId());
    }

    @Test
    @DisplayName("Report existing payment of a rental and type")
    void existsByRentalIdAndType_ExistingPayment_ReturnsTrue() {
        // Given
        Long rentalId = 1L;

        // When
        boolean exists = paymentRepository.existsByRentalIdAndType(rentalId, PaymentType.PAYMENT);

        // Then
        assertTrue(exists);
    }

    @Test
    @DisplayName("Report missing payment of a rental and type")
    void existsByRentalIdAndType_MissingPayment_ReturnsFalse() {
        // Given
        Long rentalId = 1L;

        // When
        boolean exists = paymentRepository.existsByRentalIdAndType(rentalId, PaymentType.FINE);

        // Then
        assertFalse(exists);
    }

    @Test
    @DisplayName("Find payment when it belongs to the requested user")
    void findByIdAndRentalUserId_OwnPayment_ReturnsPayment() {
        // Given
        Long paymentId = 1L;
        Long userId = 1L;

        // When
        var actual = paymentRepository.findByIdAndRentalUserId(paymentId, userId);

        // Then
        assertTrue(actual.isPresent());
        assertEquals(paymentId, actual.get().getId());
    }

    @Test
    @DisplayName("Do not find payment when it belongs to another user")
    void findByIdAndRentalUserId_AnotherUsersPayment_ReturnsEmpty() {
        // Given
        Long paymentId = 1L;
        Long userId = 2L;

        // When
        var actual = paymentRepository.findByIdAndRentalUserId(paymentId, userId);

        // Then
        assertTrue(actual.isEmpty());
    }
}
