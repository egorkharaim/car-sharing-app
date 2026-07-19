package mate.academy.dto.user;

import jakarta.validation.constraints.NotNull;
import mate.academy.model.user.RoleName;

public record UpdateUserRoleRequestDto(@NotNull RoleName role) {
}
