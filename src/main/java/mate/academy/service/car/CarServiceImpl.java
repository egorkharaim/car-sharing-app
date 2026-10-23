package mate.academy.service.car;

import lombok.RequiredArgsConstructor;
import mate.academy.dto.car.CarDto;
import mate.academy.dto.car.CreateCarRequestDto;
import mate.academy.exception.EntityNotFoundException;
import mate.academy.mapper.CarMapper;
import mate.academy.model.Car;
import mate.academy.repository.car.CarRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class CarServiceImpl implements CarService {
    private final CarRepository carRepository;
    private final CarMapper carMapper;

    @Override
    public CarDto save(CreateCarRequestDto requestDto) {
        Car car = carMapper.toModel(requestDto);
        return carMapper.toDto(carRepository.save(car));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CarDto> findAll(Pageable pageable) {
        return carRepository.findAll(pageable)
                .map(carMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public CarDto findById(Long id) {
        return carMapper.toDto(getCarEntityById(id));
    }

    @Override
    public CarDto update(Long id, CreateCarRequestDto requestDto) {
        Car car = getCarEntityById(id);
        carMapper.updateCarFromDto(requestDto, car);
        return carMapper.toDto(carRepository.save(car));
    }

    @Override
    public void deleteById(Long id) {
        carRepository.deleteById(id);
    }

    private Car getCarEntityById(Long id) {
        return carRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Can't find car by id: " + id));
    }
}
