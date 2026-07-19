package mate.academy.service.rental;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import mate.academy.dto.rental.CreateRentalRequestDto;
import mate.academy.dto.rental.RentalDto;
import mate.academy.exception.EntityNotFoundException;
import mate.academy.exception.RentalProcessingException;
import mate.academy.mapper.RentalMapper;
import mate.academy.model.Car;
import mate.academy.model.CarType;
import mate.academy.model.Rental;
import mate.academy.model.user.Role;
import mate.academy.model.user.RoleName;
import mate.academy.model.user.User;
import mate.academy.repository.car.CarRepository;
import mate.academy.repository.rental.RentalRepository;
import mate.academy.service.notification.NotificationMessageBuilder;
import mate.academy.service.notification.NotificationService;
import mate.academy.service.payment.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RentalServiceImplTest {
    private static final Long RENTAL_ID = 1L;
    private static final Long CAR_ID = 10L;
    private static final Long USER_ID = 100L;
    private static final String RENTAL_MESSAGE = "New rental created";

    @Mock
    private RentalRepository rentalRepository;
    @Mock
    private CarRepository carRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private RentalMapper rentalMapper;
    @Mock
    private NotificationService notificationService;
    @Mock
    private NotificationMessageBuilder notificationMessageBuilder;
    @InjectMocks
    private RentalServiceImpl rentalService;

    @Test
    @DisplayName("Create rental decreases inventory, creates payment and sends notification")
    void createRental_ValidRequest_ReturnsRentalDto() {
        // Given
        User user = createUser(USER_ID, RoleName.CUSTOMER);
        Car car = createCar(2);
        CreateRentalRequestDto requestDto = new CreateRentalRequestDto(
                CAR_ID,
                LocalDate.now().plusDays(3)
        );
        Rental savedRental = createRental(user, car);
        RentalDto expected = createRentalDto(savedRental);
        when(rentalRepository.existsByUserIdAndActualReturnDateIsNull(USER_ID))
                .thenReturn(false);
        when(carRepository.decreaseInventoryIfAvailable(CAR_ID)).thenReturn(1);
        when(carRepository.findById(CAR_ID)).thenReturn(Optional.of(car));
        when(rentalRepository.save(org.mockito.ArgumentMatchers.any(Rental.class)))
                .thenReturn(savedRental);
        when(notificationMessageBuilder.buildNewRentalMessage(savedRental))
                .thenReturn(RENTAL_MESSAGE);
        when(rentalMapper.toDto(savedRental)).thenReturn(expected);

        // When
        RentalDto actual = rentalService.createRental(requestDto, user);

        // Then
        assertEquals(expected, actual);
        verify(rentalRepository).existsByUserIdAndActualReturnDateIsNull(USER_ID);
        verify(carRepository).decreaseInventoryIfAvailable(CAR_ID);
        verify(carRepository).findById(CAR_ID);
        verify(rentalRepository).save(org.mockito.ArgumentMatchers.any(Rental.class));
        verify(paymentService).createRentalPayment(savedRental);
        verify(notificationMessageBuilder).buildNewRentalMessage(savedRental);
        verify(notificationService).sendMessage(RENTAL_MESSAGE);
        verify(rentalMapper).toDto(savedRental);
    }

    @Test
    @DisplayName("Create rental for user with active rental throws exception")
    void createRental_UserHasActiveRental_ThrowsException() {
        // Given
        User user = createUser(USER_ID, RoleName.CUSTOMER);
        CreateRentalRequestDto requestDto = new CreateRentalRequestDto(
                CAR_ID,
                LocalDate.now().plusDays(3)
        );
        when(rentalRepository.existsByUserIdAndActualReturnDateIsNull(USER_ID))
                .thenReturn(true);

        // When
        RentalProcessingException exception = assertThrows(
                RentalProcessingException.class,
                () -> rentalService.createRental(requestDto, user)
        );

        // Then
        assertEquals(
                "User already has an active rental and can't create a new one",
                exception.getMessage()
        );
        verify(rentalRepository).existsByUserIdAndActualReturnDateIsNull(USER_ID);
        verify(carRepository, never()).decreaseInventoryIfAvailable(CAR_ID);
    }

    @Test
    @DisplayName("Create rental for unavailable car throws exception")
    void createRental_UnavailableCar_ThrowsException() {
        // Given
        User user = createUser(USER_ID, RoleName.CUSTOMER);
        CreateRentalRequestDto requestDto = new CreateRentalRequestDto(
                CAR_ID,
                LocalDate.now().plusDays(3)
        );
        when(rentalRepository.existsByUserIdAndActualReturnDateIsNull(USER_ID))
                .thenReturn(false);
        when(carRepository.decreaseInventoryIfAvailable(CAR_ID)).thenReturn(0);

        // When
        RentalProcessingException exception = assertThrows(
                RentalProcessingException.class,
                () -> rentalService.createRental(requestDto, user)
        );

        // Then
        assertEquals("Selected car is not available", exception.getMessage());
        verify(carRepository).decreaseInventoryIfAvailable(CAR_ID);
        verify(rentalRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Return overdue rental increases inventory and creates fine payment")
    void returnRental_OverdueRental_CreatesFinePayment() {
        // Given
        User user = createUser(USER_ID, RoleName.CUSTOMER);
        Car car = createCar(1);
        Rental rental = createRental(user, car);
        rental.setReturnDate(LocalDate.now().minusDays(2));
        RentalDto expected = createRentalDto(rental);
        when(rentalRepository.findById(RENTAL_ID)).thenReturn(Optional.of(rental));
        when(carRepository.save(car)).thenReturn(car);
        when(rentalRepository.save(rental)).thenReturn(rental);
        when(rentalMapper.toDto(rental)).thenReturn(expected);

        // When
        RentalDto actual = rentalService.returnRental(RENTAL_ID, user);

        // Then
        assertEquals(expected, actual);
        assertEquals(LocalDate.now(), rental.getActualReturnDate());
        assertEquals(2, car.getInventory());
        verify(rentalRepository).findById(RENTAL_ID);
        verify(carRepository).save(car);
        verify(rentalRepository).save(rental);
        verify(paymentService).createFinePayment(rental);
        verify(rentalMapper).toDto(rental);
    }

    @Test
    @DisplayName("Get rental by another customer throws not found exception")
    void getRentalById_AnotherCustomerRental_ThrowsException() {
        // Given
        User owner = createUser(USER_ID, RoleName.CUSTOMER);
        User anotherUser = createUser(200L, RoleName.CUSTOMER);
        Rental rental = createRental(owner, createCar(1));
        when(rentalRepository.findById(RENTAL_ID)).thenReturn(Optional.of(rental));

        // When
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> rentalService.getRentalById(RENTAL_ID, anotherUser)
        );

        // Then
        assertEquals("You don't have access to this rental", exception.getMessage());
        verify(rentalRepository).findById(RENTAL_ID);
    }

    private Rental createRental(User user, Car car) {
        Rental rental = new Rental();
        rental.setId(RENTAL_ID);
        rental.setUser(user);
        rental.setCar(car);
        rental.setRentalDate(LocalDate.now());
        rental.setReturnDate(LocalDate.now().plusDays(3));
        return rental;
    }

    private RentalDto createRentalDto(Rental rental) {
        return new RentalDto(
                rental.getId(),
                rental.getCar().getId(),
                rental.getCar().getBrand(),
                rental.getCar().getModel(),
                rental.getUser().getId(),
                rental.getRentalDate(),
                rental.getReturnDate(),
                rental.getActualReturnDate()
        );
    }

    private Car createCar(int inventory) {
        Car car = new Car();
        car.setId(CAR_ID);
        car.setBrand("Toyota");
        car.setModel("Corolla");
        car.setType(CarType.SEDAN);
        car.setInventory(inventory);
        car.setDailyFee(BigDecimal.valueOf(50));
        return car;
    }

    private User createUser(Long id, RoleName roleName) {
        Role role = new Role();
        role.setId(roleName == RoleName.MANAGER ? 1L : 2L);
        role.setName(roleName);

        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setPassword("password");
        user.setRoles(Set.of(role));
        return user;
    }
}
