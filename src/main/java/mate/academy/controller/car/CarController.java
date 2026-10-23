package mate.academy.controller.car;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mate.academy.dto.car.CarDto;
import mate.academy.dto.car.CreateCarRequestDto;
import mate.academy.service.car.CarService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cars")
@RequiredArgsConstructor
@Tag(name = "Cars", description = "Car catalog management endpoints")
public class CarController {
    private final CarService carService;

    @Operation(summary = "Get all cars with pagination")
    @GetMapping
    public Page<CarDto> getAll(Pageable pageable) {
        return carService.findAll(pageable);
    }

    @Operation(summary = "Get car by id")
    @GetMapping("/{id}")
    public CarDto getById(@PathVariable Long id) {
        return carService.findById(id);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Create a new car")
    @PostMapping
    public CarDto create(@RequestBody @Valid CreateCarRequestDto requestDto) {
        return carService.save(requestDto);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Update car by id")
    @PutMapping("/{id}")
    public CarDto update(
            @PathVariable Long id,
            @RequestBody @Valid CreateCarRequestDto requestDto
    ) {
        return carService.update(id, requestDto);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Delete car by id")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        carService.deleteById(id);
    }
}
