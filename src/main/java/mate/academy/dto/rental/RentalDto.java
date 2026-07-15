package mate.academy.dto.rental;

import java.time.LocalDate;

public record RentalDto(
        Long id,
        Long carId,
        String carBrand,
        String carModel,
        Long userId,
        LocalDate rentalDate,
        LocalDate returnDate,
        LocalDate actualReturnDate
) {
}
