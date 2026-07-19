package mate.academy.service.notification;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mate.academy.model.Rental;
import mate.academy.repository.rental.RentalRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OverdueRentalNotificationScheduler {
    private final RentalRepository rentalRepository;
    private final NotificationService notificationService;
    private final NotificationMessageBuilder notificationMessageBuilder;

    @Scheduled(cron = "${telegram.overdue-cron}")
    public void notifyAboutOverdueRentals() {
        LocalDate overdueThreshold = LocalDate.now().minusDays(1);
        List<Rental> overdueRentals =
                rentalRepository.findAllByActualReturnDateIsNullAndReturnDateLessThanEqual(
                        overdueThreshold
                );

        if (overdueRentals.isEmpty()) {
            notificationService.sendMessage(
                    notificationMessageBuilder.buildNoOverdueRentalsMessage()
            );
            return;
        }

        for (Rental rental : overdueRentals) {
            notificationService.sendMessage(
                    notificationMessageBuilder.buildOverdueRentalMessage(rental)
            );
        }
    }
}
