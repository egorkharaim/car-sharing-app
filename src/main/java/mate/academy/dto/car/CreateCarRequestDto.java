package mate.academy.dto.car;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import mate.academy.model.CarType;

public record CreateCarRequestDto(
        @NotBlank String model,
        @NotBlank String brand,
        @NotNull CarType type,
        @NotNull @Min(0) Integer inventory,
        @NotNull @Min(0) BigDecimal dailyFee
) {
}
