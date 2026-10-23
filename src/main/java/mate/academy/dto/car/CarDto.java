package mate.academy.dto.car;

import java.math.BigDecimal;
import mate.academy.model.CarType;

public record CarDto(
        Long id,
        String model,
        String brand,
        CarType type,
        Integer inventory,
        BigDecimal dailyFee
) {
}
