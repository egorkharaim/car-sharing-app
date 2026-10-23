package mate.academy.mapper;

import mate.academy.dto.rental.RentalDto;
import mate.academy.model.Rental;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public abstract class RentalMapper {
    @Mapping(target = "carId", source = "car.id")
    @Mapping(target = "carBrand", source = "car.brand")
    @Mapping(target = "carModel", source = "car.model")
    @Mapping(target = "userId", source = "user.id")
    public abstract RentalDto toDto(Rental rental);
}
