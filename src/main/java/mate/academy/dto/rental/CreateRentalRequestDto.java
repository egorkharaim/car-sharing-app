package mate.academy.dto.rental;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateRentalRequestDto(
        @NotNull Long carId,
        @NotNull @Future LocalDate returnDate
) {
}
