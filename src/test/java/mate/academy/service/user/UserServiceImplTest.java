package mate.academy.service.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    private static final Long USER_ID = 1L;
    private static final String EMAIL = "customer@example.com";
    private static final String PASSWORD = "password123";
    private static final String ENCODED_PASSWORD = "encodedPassword";

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapper userMapper;
    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("Register new user assigns customer role and encoded password")
    void register_NewUser_ReturnsUserResponseDto() throws RegistrationException {
        // Given
        UserRegistrationRequestDto requestDto = createRegistrationRequestDto();
        Role customerRole = createRole(RoleName.CUSTOMER);
        User user = createUser(null);
        User savedUser = createUser(USER_ID);
        UserResponseDto expected = createUserResponseDto();
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userMapper.toModel(requestDto)).thenReturn(user);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(roleRepository.findByName(RoleName.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(userRepository.save(user)).thenReturn(savedUser);
        when(userMapper.toDto(savedUser)).thenReturn(expected);

        // When
        UserResponseDto actual = userService.register(requestDto);

        // Then
        assertEquals(expected, actual);
        assertEquals(ENCODED_PASSWORD, user.getPassword());
        assertEquals(Set.of(customerRole), user.getRoles());
        verify(userRepository).existsByEmail(EMAIL);
        verify(userMapper).toModel(requestDto);
        verify(passwordEncoder).encode(PASSWORD);
        verify(roleRepository).findByName(RoleName.CUSTOMER);
        verify(userRepository).save(user);
        verify(userMapper).toDto(savedUser);
    }

    @Test
    @DisplayName("Register existing email throws registration exception")
    void register_ExistingEmail_ThrowsException() {
        // Given
        UserRegistrationRequestDto requestDto = createRegistrationRequestDto();
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        // When
        RegistrationException exception = assertThrows(
                RegistrationException.class,
                () -> userService.register(requestDto)
        );

        // Then
        assertEquals("This email is already taken: " + EMAIL, exception.getMessage());
        verify(userRepository).existsByEmail(EMAIL);
    }

    @Test
    @DisplayName("Get profile by existing email returns user dto")
    void getProfile_ExistingEmail_ReturnsUserResponseDto() {
        // Given
        User user = createUser(USER_ID);
        UserResponseDto expected = createUserResponseDto();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(expected);

        // When
        UserResponseDto actual = userService.getProfile(EMAIL);

        // Then
        assertEquals(expected, actual);
        verify(userRepository).findByEmail(EMAIL);
        verify(userMapper).toDto(user);
    }

    @Test
    @DisplayName("Update role for existing user saves new role")
    void updateRole_ExistingUserAndRole_ReturnsUserResponseDto() {
        // Given
        UpdateUserRoleRequestDto requestDto = new UpdateUserRoleRequestDto(RoleName.MANAGER);
        User user = createUser(USER_ID);
        Role managerRole = createRole(RoleName.MANAGER);
        UserResponseDto expected = new UserResponseDto(
                USER_ID,
                EMAIL,
                "John",
                "Doe",
                Set.of(RoleName.MANAGER.name())
        );
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleName.MANAGER)).thenReturn(Optional.of(managerRole));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(expected);

        // When
        UserResponseDto actual = userService.updateRole(USER_ID, requestDto);

        // Then
        assertEquals(expected, actual);
        assertEquals(Set.of(managerRole), user.getRoles());
        verify(userRepository).findById(USER_ID);
        verify(roleRepository).findByName(RoleName.MANAGER);
        verify(userRepository).save(user);
        verify(userMapper).toDto(user);
    }

    @Test
    @DisplayName("Get by missing email throws exception")
    void getByEmail_MissingEmail_ThrowsException() {
        // Given
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // When
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> userService.getByEmail(EMAIL)
        );

        // Then
        assertEquals("Can't find user by email: " + EMAIL, exception.getMessage());
        verify(userRepository).findByEmail(EMAIL);
    }

    private UserRegistrationRequestDto createRegistrationRequestDto() {
        return new UserRegistrationRequestDto(
                EMAIL,
                "John",
                "Doe",
                PASSWORD,
                PASSWORD
        );
    }

    private User createUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail(EMAIL);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setPassword(PASSWORD);
        user.setRoles(new HashSet<>(Set.of(createRole(RoleName.CUSTOMER))));
        return user;
    }

    private Role createRole(RoleName roleName) {
        Role role = new Role();
        role.setId(roleName == RoleName.MANAGER ? 1L : 2L);
        role.setName(roleName);
        return role;
    }

    private UserResponseDto createUserResponseDto() {
        return new UserResponseDto(
                USER_ID,
                EMAIL,
                "John",
                "Doe",
                Set.of(RoleName.CUSTOMER.name())
        );
    }
}
