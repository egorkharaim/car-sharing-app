package mate.academy.dto.user;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserProfileRequestDto(
        @NotBlank String firstName,
        @NotBlank String lastName
) {
}
