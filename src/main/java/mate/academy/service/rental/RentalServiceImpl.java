package mate.academy.service.rental;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import mate.academy.dto.rental.CreateRentalRequestDto;
import mate.academy.dto.rental.RentalDto;
import mate.academy.exception.EntityNotFoundException;
import mate.academy.exception.RentalProcessingException;
import mate.academy.mapper.RentalMapper;
import mate.academy.model.Car;
import mate.academy.model.Rental;
import mate.academy.model.user.RoleName;
import mate.academy.model.user.User;
import mate.academy.repository.car.CarRepository;
import mate.academy.repository.rental.RentalRepository;
import mate.academy.service.notification.NotificationMessageBuilder;
import mate.academy.service.notification.NotificationService;
import mate.academy.service.payment.PaymentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class RentalServiceImpl implements RentalService {
    private final RentalRepository rentalRepository;
    private final CarRepository carRepository;
    private final PaymentService paymentService;
    private final RentalMapper rentalMapper;
    private final NotificationService notificationService;
    private final NotificationMessageBuilder notificationMessageBuilder;

    @Override
    public RentalDto createRental(CreateRentalRequestDto requestDto, User currentUser) {
        if (rentalRepository.existsByUserIdAndActualReturnDateIsNull(currentUser.getId())) {
            throw new RentalProcessingException(
                    "User already has an active rental and can't create a new one");
        }

        int updatedRows = carRepository.decreaseInventoryIfAvailable(requestDto.carId());
        if (updatedRows == 0) {
            throw new RentalProcessingException("Selected car is not available");
        }

        Car car = carRepository.findById(requestDto.carId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Can't find car by id: " + requestDto.carId()));

        Rental rental = new Rental();
        rental.setCar(car);
        rental.setUser(currentUser);
        rental.setRentalDate(LocalDate.now());
        rental.setReturnDate(requestDto.returnDate());

        Rental savedRental = rentalRepository.save(rental);
        paymentService.createRentalPayment(savedRental);
        notificationService.sendMessage(
                notificationMessageBuilder.buildNewRentalMessage(savedRental)
        );
        return rentalMapper.toDto(savedRental);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RentalDto> getRentals(
            Long userId,
            Boolean isActive,
            User currentUser,
            Pageable pageable
    ) {
        Page<Rental> rentals;
        if (isManager(currentUser) && userId != null) {
            rentals = loadRentalsByActiveFlag(userId, isActive, pageable);
        } else {
            rentals = loadRentalsByActiveFlag(currentUser.getId(), isActive, pageable);
        }

        return rentals.map(rentalMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public RentalDto getRentalById(Long id, User currentUser) {
        Rental rental = rentalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Can't find rental by id: " + id));
        validateAccess(rental, currentUser);
        return rentalMapper.toDto(rental);
    }

    @Override
    public RentalDto returnRental(Long id, User currentUser) {
        Rental rental = rentalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Can't find rental by id: " + id));
        validateAccess(rental, currentUser);

        if (rental.getActualReturnDate() != null) {
            throw new RentalProcessingException("Rental has already been returned");
        }

        rental.setActualReturnDate(LocalDate.now());
        Car car = rental.getCar();
        car.setInventory(car.getInventory() + 1);
        carRepository.save(car);
        Rental savedRental = rentalRepository.save(rental);
        if (savedRental.getActualReturnDate().isAfter(savedRental.getReturnDate())) {
            paymentService.createFinePayment(savedRental);
        }
        return rentalMapper.toDto(savedRental);
    }

    private Page<Rental> loadRentalsByActiveFlag(Long userId, Boolean isActive, Pageable pageable) {
        if (Boolean.TRUE.equals(isActive)) {
            return rentalRepository.findAllByUserIdAndActualReturnDateIsNull(userId, pageable);
        }
        if (Boolean.FALSE.equals(isActive)) {
            return rentalRepository.findAllByUserIdAndActualReturnDateIsNotNull(userId, pageable);
        }
        return rentalRepository.findAllByUserId(userId, pageable);
    }

    private void validateAccess(Rental rental, User currentUser) {
        if (!isManager(currentUser) && !rental.getUser().getId().equals(currentUser.getId())) {
            throw new EntityNotFoundException("You don't have access to this rental");
        }
    }

    private boolean isManager(User currentUser) {
        return currentUser.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.MANAGER);
    }
}
