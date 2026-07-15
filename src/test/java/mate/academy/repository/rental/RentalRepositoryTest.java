package mate.academy.repository.rental;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
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
class RentalRepositoryTest extends AbstractRepositoryTest {
    @Autowired
    private RentalRepository rentalRepository;

    @Test
    @DisplayName("Find all rentals of a user")
    void findAllByUserId_ExistingUser_ReturnsUserRentals() {
        // Given
        Long userId = 1L;

        // When
        Page<?> rentals = rentalRepository.findAllByUserId(userId, PageRequest.of(0, 10));

        // Then
        assertEquals(2, rentals.getTotalElements());
    }

    @Test
    @DisplayName("Find active rentals of a user")
    void findAllByUserIdAndActualReturnDateIsNull_ExistingUser_ReturnsActiveRentals() {
        // Given
        Long userId = 1L;

        // When
        Page<?> rentals = rentalRepository.findAllByUserIdAndActualReturnDateIsNull(
                userId,
                PageRequest.of(0, 10)
        );

        // Then
        assertEquals(1, rentals.getTotalElements());
    }

    @Test
    @DisplayName("Find returned rentals of a user")
    void findAllByUserIdAndActualReturnDateIsNotNull_ExistingUser_ReturnsReturnedRentals() {
        // Given
        Long userId = 1L;

        // When
        Page<?> rentals = rentalRepository.findAllByUserIdAndActualReturnDateIsNotNull(
                userId,
                PageRequest.of(0, 10)
        );

        // Then
        assertEquals(1, rentals.getTotalElements());
    }

    @Test
    @DisplayName("Report active rental when user has one")
    void existsByUserIdAndActualReturnDateIsNull_UserWithActiveRental_ReturnsTrue() {
        // Given
        Long userId = 1L;

        // When
        boolean hasActiveRental = rentalRepository.existsByUserIdAndActualReturnDateIsNull(userId);

        // Then
        assertTrue(hasActiveRental);
    }

    @Test
    @DisplayName("Report no active rental when user has none")
    void existsByUserIdAndActualReturnDateIsNull_UserWithoutActiveRental_ReturnsFalse() {
        // Given
        Long userId = 999L;

        // When
        boolean hasActiveRental = rentalRepository.existsByUserIdAndActualReturnDateIsNull(userId);

        // Then
        assertFalse(hasActiveRental);
    }

    @Test
    @DisplayName("Find not returned rentals with return date before threshold")
    void findAllByActualReturnDateIsNullAndReturnDateLessThanEqual_OverdueThreshold_ReturnsRentals() {
        // Given
        LocalDate threshold = LocalDate.of(2026, 7, 10);

        // When
        var rentals = rentalRepository
                .findAllByActualReturnDateIsNullAndReturnDateLessThanEqual(threshold);

        // Then
        assertEquals(1, rentals.size());
        assertEquals(1L, rentals.get(0).getId());
    }
}
