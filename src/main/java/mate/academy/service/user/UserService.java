package mate.academy.service.user;

import mate.academy.dto.user.UpdateUserProfileRequestDto;
import mate.academy.dto.user.UpdateUserRoleRequestDto;
import mate.academy.dto.user.UserRegistrationRequestDto;
import mate.academy.dto.user.UserResponseDto;
import mate.academy.exception.RegistrationException;
import mate.academy.model.user.User;

public interface UserService {
    UserResponseDto register(UserRegistrationRequestDto requestDto) throws RegistrationException;

    UserResponseDto getProfile(String email);

    UserResponseDto updateProfile(String email, UpdateUserProfileRequestDto requestDto);

    UserResponseDto updateRole(Long userId, UpdateUserRoleRequestDto requestDto);

    User getByEmail(String email);
}
