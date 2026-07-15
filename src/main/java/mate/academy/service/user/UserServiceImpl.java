package mate.academy.service.user;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import mate.academy.dto.user.UpdateUserProfileRequestDto;
import mate.academy.dto.user.UpdateUserRoleRequestDto;
import mate.academy.dto.user.UserRegistrationRequestDto;
import mate.academy.dto.user.UserResponseDto;
import mate.academy.exception.EntityNotFoundException;
import mate.academy.exception.RegistrationException;
import mate.academy.mapper.UserMapper;
import mate.academy.model.user.Role;
import mate.academy.model.user.RoleName;
import mate.academy.model.user.User;
import mate.academy.repository.user.RoleRepository;
import mate.academy.repository.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    public UserResponseDto register(UserRegistrationRequestDto requestDto)
            throws RegistrationException {
        if (userRepository.existsByEmail(requestDto.email())) {
            throw new RegistrationException("This email is already taken: " + requestDto.email());
        }

        User user = userMapper.toModel(requestDto);
        user.setPassword(passwordEncoder.encode(requestDto.password()));

        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseThrow(() ->
                        new RegistrationException("Can't find default role CUSTOMER"));
        user.setRoles(Set.of(customerRole));

        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getProfile(String email) {
        return userMapper.toDto(getByEmail(email));
    }

    @Override
    public UserResponseDto updateProfile(String email, UpdateUserProfileRequestDto requestDto) {
        User user = getByEmail(email);
        user.setFirstName(requestDto.firstName());
        user.setLastName(requestDto.lastName());
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    public UserResponseDto updateRole(Long userId, UpdateUserRoleRequestDto requestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Can't find user by id: " + userId));
        Role role = roleRepository.findByName(requestDto.role())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Can't find role: " + requestDto.role()));
        user.setRoles(Set.of(role));
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Can't find user by email: " + email));
    }
}
