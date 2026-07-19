package mate.academy.service.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import mate.academy.model.Car;
import mate.academy.model.CarType;
import mate.academy.model.Rental;
import mate.academy.model.user.User;
import mate.academy.repository.rental.RentalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OverdueRentalNotificationSchedulerTest {
    private static final String NO_OVERDUE_MESSAGE = "No rentals overdue today!";
    private static final String OVERDUE_MESSAGE = "Overdue rental detected";

    @Mock
    private RentalRepository rentalRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private NotificationMessageBuilder notificationMessageBuilder;
    @InjectMocks
    private OverdueRentalNotificationScheduler scheduler;

    @Test
    @DisplayName("Notify overdue rentals sends no overdue message when list is empty")
    void notifyAboutOverdueRentals_NoOverdueRentals_SendsNoOverdueMessage() {
        // Given
        LocalDate overdueThreshold = LocalDate.now().minusDays(1);
        when(rentalRepository.findAllByActualReturnDateIsNullAndReturnDateLessThanEqual(
                overdueThreshold
        )).thenReturn(List.of());
        when(notificationMessageBuilder.buildNoOverdueRentalsMessage())
                .thenReturn(NO_OVERDUE_MESSAGE);

        // When
        scheduler.notifyAboutOverdueRentals();

        // Then
        verify(rentalRepository).findAllByActualReturnDateIsNullAndReturnDateLessThanEqual(
                overdueThreshold
        );
        verify(notificationMessageBuilder).buildNoOverdueRentalsMessage();
        verify(notificationService).sendMessage(NO_OVERDUE_MESSAGE);
        verify(notificationMessageBuilder, never()).buildOverdueRentalMessage(any());
    }

    @Test
    @DisplayName("Notify overdue rentals sends message for each overdue rental")
    void notifyAboutOverdueRentals_OverdueRentals_SendMessages() {
        // Given
        LocalDate overdueThreshold = LocalDate.now().minusDays(1);
        Rental rental = createRental();
        when(rentalRepository.findAllByActualReturnDateIsNullAndReturnDateLessThanEqual(
                overdueThreshold
        )).thenReturn(List.of(rental));
        when(notificationMessageBuilder.buildOverdueRentalMessage(rental))
                .thenReturn(OVERDUE_MESSAGE);

        // When
        scheduler.notifyAboutOverdueRentals();

        // Then
        verify(rentalRepository).findAllByActualReturnDateIsNullAndReturnDateLessThanEqual(
                overdueThreshold
        );
        verify(notificationMessageBuilder).buildOverdueRentalMessage(rental);
        verify(notificationService).sendMessage(OVERDUE_MESSAGE);
        verify(notificationMessageBuilder, never()).buildNoOverdueRentalsMessage();
    }

    private Rental createRental() {
        Car car = new Car();
        car.setId(1L);
        car.setBrand("Toyota");
        car.setModel("Corolla");
        car.setType(CarType.SEDAN);
        car.setInventory(1);
        car.setDailyFee(BigDecimal.valueOf(50));

        User user = new User();
        user.setId(1L);
        user.setEmail("customer@example.com");

        Rental rental = new Rental();
        rental.setId(1L);
        rental.setCar(car);
        rental.setUser(user);
        rental.setRentalDate(LocalDate.now().minusDays(5));
        rental.setReturnDate(LocalDate.now().minusDays(2));
        return rental;
    }
}
