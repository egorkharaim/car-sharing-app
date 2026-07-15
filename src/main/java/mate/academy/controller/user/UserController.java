package mate.academy.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mate.academy.dto.user.UpdateUserProfileRequestDto;
import mate.academy.dto.user.UpdateUserRoleRequestDto;
import mate.academy.dto.user.UserResponseDto;
import mate.academy.service.user.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile and role management endpoints")
public class UserController {
    private final UserService userService;

    @Operation(summary = "Get current user profile")
    @GetMapping("/me")
    public UserResponseDto getProfile(Authentication authentication) {
        return userService.getProfile(authentication.getName());
    }

    @Operation(summary = "Update current user profile")
    @PatchMapping("/me")
    public UserResponseDto updateProfile(
            Authentication authentication,
            @RequestBody @Valid UpdateUserProfileRequestDto requestDto
    ) {
        return userService.updateProfile(authentication.getName(), requestDto);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Update user role by id")
    @PatchMapping("/{id}/role")
    public UserResponseDto updateRole(
            @PathVariable Long id,
            @RequestBody @Valid UpdateUserRoleRequestDto requestDto
    ) {
        return userService.updateRole(id, requestDto);
    }
}
