package mate.academy.controller.rental;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mate.academy.dto.rental.CreateRentalRequestDto;
import mate.academy.dto.rental.RentalDto;
import mate.academy.model.user.User;
import mate.academy.service.rental.RentalService;
import mate.academy.service.user.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rentals")
@RequiredArgsConstructor
@Tag(name = "Rentals", description = "Rental management endpoints")
public class RentalController {
    private final RentalService rentalService;
    private final UserService userService;

    @Operation(summary = "Create a new rental")
    @PostMapping
    public RentalDto createRental(
            @RequestBody @Valid CreateRentalRequestDto requestDto,
            org.springframework.security.core.Authentication authentication
    ) {
        User currentUser = userService.getByEmail(authentication.getName());
        return rentalService.createRental(requestDto, currentUser);
    }

    @Operation(summary = "Get rentals with optional filters")
    @GetMapping
    public Page<RentalDto> getRentals(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Boolean isActive,
            Pageable pageable,
            org.springframework.security.core.Authentication authentication
    ) {
        User currentUser = userService.getByEmail(authentication.getName());
        return rentalService.getRentals(userId, isActive, currentUser, pageable);
    }

    @Operation(summary = "Get rental by id")
    @GetMapping("/{id}")
    public RentalDto getRentalById(
            @PathVariable Long id,
            org.springframework.security.core.Authentication authentication
    ) {
        User currentUser = userService.getByEmail(authentication.getName());
        return rentalService.getRentalById(id, currentUser);
    }

    @Operation(summary = "Return rented car")
    @PatchMapping("/{id}/return")
    public RentalDto returnRental(
            @PathVariable Long id,
            org.springframework.security.core.Authentication authentication
    ) {
        User currentUser = userService.getByEmail(authentication.getName());
        return rentalService.returnRental(id, currentUser);
    }
}
