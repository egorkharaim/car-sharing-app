package mate.academy.mapper;

import mate.academy.dto.payment.PaymentDto;
import mate.academy.model.payment.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public abstract class PaymentMapper {
    @Mapping(target = "rentalId", source = "rental.id")
    public abstract PaymentDto toDto(Payment payment);
}
