package mate.academy.mapper;

import mate.academy.dto.car.CarDto;
import mate.academy.dto.car.CreateCarRequestDto;
import mate.academy.model.Car;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public abstract class CarMapper {
    public abstract CarDto toDto(Car car);

    @Mapping(target = "id", ignore = true)
    public abstract Car toModel(CreateCarRequestDto requestDto);
}
