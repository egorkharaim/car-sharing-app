package mate.academy.service.car;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import mate.academy.dto.car.CarDto;
import mate.academy.dto.car.CreateCarRequestDto;
import mate.academy.exception.EntityNotFoundException;
import mate.academy.mapper.CarMapper;
import mate.academy.model.Car;
import mate.academy.model.CarType;
import mate.academy.repository.car.CarRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CarServiceImplTest {
    private static final Long CAR_ID = 1L;

    @Mock
    private CarRepository carRepository;
    @Mock
    private CarMapper carMapper;
    @InjectMocks
    private CarServiceImpl carService;

    @Test
    @DisplayName("Save valid car request returns saved car dto")
    void save_ValidRequest_ReturnsSavedCarDto() {
        // Given
        CreateCarRequestDto requestDto = createCarRequestDto();
        Car car = createCar(null);
        Car savedCar = createCar(CAR_ID);
        CarDto expected = createCarDto();
        when(carMapper.toModel(requestDto)).thenReturn(car);
        when(carRepository.save(car)).thenReturn(savedCar);
        when(carMapper.toDto(savedCar)).thenReturn(expected);

        // When
        CarDto actual = carService.save(requestDto);

        // Then
        assertEquals(expected, actual);
        verify(carMapper).toModel(requestDto);
        verify(carRepository).save(car);
        verify(carMapper).toDto(savedCar);
    }

    @Test
    @DisplayName("Find by existing id returns car dto")
    void findById_ExistingId_ReturnsCarDto() {
        // Given
        Car car = createCar(CAR_ID);
        CarDto expected = createCarDto();
        when(carRepository.findById(CAR_ID)).thenReturn(Optional.of(car));
        when(carMapper.toDto(car)).thenReturn(expected);

        // When
        CarDto actual = carService.findById(CAR_ID);

        // Then
        assertEquals(expected, actual);
        verify(carRepository).findById(CAR_ID);
        verify(carMapper).toDto(car);
    }

    @Test
    @DisplayName("Find by missing id throws exception")
    void findById_MissingId_ThrowsException() {
        // Given
        when(carRepository.findById(CAR_ID)).thenReturn(Optional.empty());

        // When
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> carService.findById(CAR_ID)
        );

        // Then
        assertEquals("Can't find car by id: " + CAR_ID, exception.getMessage());
        verify(carRepository).findById(CAR_ID);
    }

    @Test
    @DisplayName("Update existing car changes fields and returns dto")
    void update_ExistingCar_ReturnsUpdatedCarDto() {
        // Given
        CreateCarRequestDto requestDto = new CreateCarRequestDto(
                "Model 3",
                "Tesla",
                CarType.SEDAN,
                5,
                BigDecimal.valueOf(120)
        );
        Car car = createCar(CAR_ID);
        CarDto expected = new CarDto(
                CAR_ID,
                requestDto.model(),
                requestDto.brand(),
                requestDto.type(),
                requestDto.inventory(),
                requestDto.dailyFee()
        );
        when(carRepository.findById(CAR_ID)).thenReturn(Optional.of(car));
        when(carRepository.save(car)).thenReturn(car);
        when(carMapper.toDto(car)).thenReturn(expected);

        // When
        CarDto actual = carService.update(CAR_ID, requestDto);

        // Then
        assertEquals(expected, actual);
        verify(carRepository).findById(CAR_ID);
        verify(carMapper).updateCarFromDto(requestDto, car);
        verify(carRepository).save(car);
        verify(carMapper).toDto(car);
    }

    private CreateCarRequestDto createCarRequestDto() {
        return new CreateCarRequestDto(
                "Corolla",
                "Toyota",
                CarType.SEDAN,
                2,
                BigDecimal.valueOf(50)
        );
    }

    private Car createCar(Long id) {
        Car car = new Car();
        car.setId(id);
        car.setModel("Corolla");
        car.setBrand("Toyota");
        car.setType(CarType.SEDAN);
        car.setInventory(2);
        car.setDailyFee(BigDecimal.valueOf(50));
        return car;
    }

    private CarDto createCarDto() {
        return new CarDto(
                CAR_ID,
                "Corolla",
                "Toyota",
                CarType.SEDAN,
                2,
                BigDecimal.valueOf(50)
        );
    }
}
