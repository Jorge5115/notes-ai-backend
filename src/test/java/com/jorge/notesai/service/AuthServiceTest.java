package com.jorge.notesai.service;

import com.jorge.notesai.dto.AuthDtos.AuthResponse;
import com.jorge.notesai.dto.AuthDtos.LoginRequest;
import com.jorge.notesai.dto.AuthDtos.RegisterRequest;
import com.jorge.notesai.entity.User;
import com.jorge.notesai.repository.UserRepository;
import com.jorge.notesai.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("jorge@example.com", "password123", "Jorge");
        savedUser = User.builder()
                .id(1L)
                .email("jorge@example.com")
                .password("hashedPassword")
                .name("Jorge")
                .build();
    }

    @Test
    void register_deberiaCrearUsuarioYDevolverToken_cuandoEmailNoExiste() {
        when(userRepository.existsByEmail(registerRequest.email())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.password())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken(savedUser.getEmail())).thenReturn("fake-jwt-token");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.token()).isEqualTo("fake-jwt-token");
        assertThat(response.email()).isEqualTo("jorge@example.com");
        assertThat(response.name()).isEqualTo("Jorge");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_deberiaLanzarExcepcion_cuandoEmailYaExiste() {
        when(userRepository.existsByEmail(registerRequest.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe una cuenta");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_deberiaDevolverToken_cuandoCredencialesSonValidas() {
        LoginRequest loginRequest = new LoginRequest("jorge@example.com", "password123");

        when(userRepository.findByEmail("jorge@example.com")).thenReturn(Optional.of(savedUser));
        when(jwtUtil.generateToken("jorge@example.com")).thenReturn("fake-jwt-token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.token()).isEqualTo("fake-jwt-token");
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void login_deberiaLanzarExcepcion_cuandoUsuarioNoExisteTrasAutenticar() {
        LoginRequest loginRequest = new LoginRequest("noexiste@example.com", "password123");

        when(userRepository.findByEmail("noexiste@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Credenciales inválidas");
    }
}