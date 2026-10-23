package mate.academy.service.notification;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.StringJoiner;
import mate.academy.model.Rental;
import mate.academy.model.payment.Payment;
import org.springframework.stereotype.Component;

@Component
public class NotificationMessageBuilder {
    public String buildNewRentalMessage(Rental rental) {
        return new StringJoiner("\n")
                .add("New rental created")
                .add("Rental ID: " + rental.getId())
                .add("Customer: " + rental.getUser().getEmail())
                .add("Car: " + rental.getCar().getBrand() + " " + rental.getCar().getModel())
                .add("Rental date: " + rental.getRentalDate())
                .add("Return date: " + rental.getReturnDate())
                .toString();
    }

    public String buildOverdueRentalMessage(Rental rental) {
        long overdueDays = ChronoUnit.DAYS.between(rental.getReturnDate(), LocalDate.now());
        return new StringJoiner("\n")
                .add("Overdue rental detected")
                .add("Rental ID: " + rental.getId())
                .add("Customer: " + rental.getUser().getEmail())
                .add("Car: " + rental.getCar().getBrand() + " " + rental.getCar().getModel())
                .add("Return date: " + rental.getReturnDate())
                .add("Overdue days: " + overdueDays)
                .toString();
    }

    public String buildNoOverdueRentalsMessage() {
        return "No rentals overdue today!";
    }

    public String buildSuccessfulPaymentMessage(Payment payment) {
        return new StringJoiner("\n")
                .add("Payment completed successfully")
                .add("Payment ID: " + payment.getId())
                .add("Type: " + payment.getType())
                .add("Rental ID: " + payment.getRental().getId())
                .add("Customer: " + payment.getRental().getUser().getEmail())
                .add("Amount: $" + payment.getAmountToPay())
                .toString();
    }
}
