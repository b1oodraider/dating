package com.dating.core.auth.service;


import com.dating.core.auth.api.dto.LoginRequest;
import com.dating.core.auth.domain.User;
import com.dating.core.auth.repo.RefreshTokenRepository;
import com.dating.core.auth.repo.UserRepository;
import com.dating.core.common.config.JWTProperties;
import com.dating.core.common.security.JWTService;
import com.dating.core.profile.service.ProfileCreator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JWTService jwtService;
    @Mock ProfileCreator profileCreator;
    @Mock ApplicationEventPublisher events;

    AuthService authService;

    private AuthService service() {
        var props = new JWTProperties("test-secret-at-least-32-bytes-long-xx", 15, 30);
        return new AuthService(userRepository, refreshTokenRepository, passwordEncoder, jwtService, profileCreator, props, events);
    }

    @Test
    void login_withWrongPassword_throws() {
        var user = new User("a@mail.ru", "hashed");
        when(userRepository.findByEmail("a@mail.ru")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong","hashed")).thenReturn(false);
        assertThatThrownBy(()->service().login(new LoginRequest("a@mail.ru", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_withUnknownEmail_throws() {
        when(userRepository.findByEmail("x@mail.ru")).thenReturn(Optional.empty());
        assertThatThrownBy(()-> service().login(new LoginRequest("x@mail.ru", "p")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
