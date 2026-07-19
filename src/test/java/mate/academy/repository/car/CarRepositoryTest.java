package mate.academy.repository.car;

import static org.junit.jupiter.api.Assertions.assertEquals;

import mate.academy.model.Car;
import mate.academy.repository.AbstractRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(
        scripts = "classpath:database/repository/add-repository-data.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
        scripts = "classpath:database/repository/clear-repository-data.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class CarRepositoryTest extends AbstractRepositoryTest {
    @Autowired
    private CarRepository carRepository;

    @Test
    @DisplayName("Decrease inventory when car is available")
    void decreaseInventoryIfAvailable_ValidCarWithInventory_DecreasesInventory() {
        // Given
        Long carId = 1L;

        // When
        int updatedRows = carRepository.decreaseInventoryIfAvailable(carId);
        Car car = carRepository.findById(carId).orElseThrow();

        // Then
        assertEquals(1, updatedRows);
        assertEquals(1, car.getInventory());
    }

    @Test
    @DisplayName("Do not decrease inventory when car is unavailable")
    void decreaseInventoryIfAvailable_CarWithoutInventory_DoesNotUpdateCar() {
        // Given
        Long carId = 2L;

        // When
        int updatedRows = carRepository.decreaseInventoryIfAvailable(carId);
        Car car = carRepository.findById(carId).orElseThrow();

        // Then
        assertEquals(0, updatedRows);
        assertEquals(0, car.getInventory());
    }
}
