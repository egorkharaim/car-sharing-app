package mate.academy.service.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import mate.academy.dto.user.UserLoginRequestDto;
import mate.academy.dto.user.UserLoginResponseDto;
import mate.academy.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {
    private static final String EMAIL = "customer@example.com";
    private static final String PASSWORD = "password123";
    private static final String TOKEN = "jwt-token";

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private Authentication authentication;
    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    @Test
    @DisplayName("Authenticate valid credentials returns JWT token")
    void authenticate_ValidCredentials_ReturnsToken() {
        // Given
        UserLoginRequestDto requestDto = new UserLoginRequestDto(EMAIL, PASSWORD);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getName()).thenReturn(EMAIL);
        when(jwtUtil.generateToken(EMAIL)).thenReturn(TOKEN);

        // When
        UserLoginResponseDto actual = authenticationService.authenticate(requestDto);

        // Then
        assertEquals(new UserLoginResponseDto(TOKEN), actual);
        verify(authenticationManager).authenticate(any());
        verify(authentication).getName();
        verify(jwtUtil).generateToken(EMAIL);
    }
}
