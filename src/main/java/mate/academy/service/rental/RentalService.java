package mate.academy.service.rental;

import mate.academy.dto.rental.CreateRentalRequestDto;
import mate.academy.dto.rental.RentalDto;
import mate.academy.model.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RentalService {
    RentalDto createRental(CreateRentalRequestDto requestDto, User currentUser);

    Page<RentalDto> getRentals(Long userId, Boolean isActive, User currentUser, Pageable pageable);

    RentalDto getRentalById(Long id, User currentUser);

    RentalDto returnRental(Long id, User currentUser);
}
