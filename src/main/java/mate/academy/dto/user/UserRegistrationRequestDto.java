package mate.academy.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import mate.academy.validation.FieldMatch;

@FieldMatch(field = "password", fieldMatch = "repeatPassword", message = "Passwords must match")
public record UserRegistrationRequestDto(
        @Email @NotBlank String email,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @Size(min = 8) String password,
        @Size(min = 8) String repeatPassword
) {
}
