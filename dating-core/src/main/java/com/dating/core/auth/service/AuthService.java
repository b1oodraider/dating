package com.dating.core.auth.service;

import com.dating.core.auth.api.dto.LoginRequest;
import com.dating.core.auth.api.dto.LoginResponse;
import com.dating.core.auth.api.dto.RegisterRequest;
import com.dating.core.auth.domain.RefreshToken;
import com.dating.core.auth.domain.User;
import com.dating.core.auth.repo.RefreshTokenRepository;
import com.dating.core.auth.repo.UserRepository;
import com.dating.core.common.config.JWTProperties;
import com.dating.core.common.security.JWTService;
import com.dating.core.profile.service.ProfileCreator;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;


/**
 * Сценарии аутентификации: регистрация, вход, обновление токенов, выход.
 */

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;
    private final ProfileCreator profileCreator;
    private final Duration refreshTtl;

    public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository, PasswordEncoder passwordEncoder, JWTService jwtService, ProfileCreator profileCreator, JWTProperties jwtProperties) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.profileCreator = profileCreator;
        this.refreshTtl = Duration.ofDays(jwtProperties.refreshTtlDays());
    }

    /**
     * Регистрирует нового пользователя и создаёт ему пустой профиль.
     *
     * @throws IllegalStateException если email уже занят
     */
    @Transactional
    public void register (RegisterRequest request) {
        if(userRepository.existsByEmail(request.email())) {
            throw new IllegalStateException("Email already in use");
        }
        String hashedPassword = passwordEncoder.encode(request.password());
        User user = userRepository.save(new User(request.email(), hashedPassword));

        profileCreator.createInitialProfile(user.getId(), request.displayName());
    }


    /**
     * Проверяет учётные данные и выдаёт пару токенов.
     *
     * @throws BadCredentialsException если email или пароль неверны
     */
    @Transactional
    public LoginResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.email())
                .orElseThrow(() -> new BadCredentialsException("Wrong email or password") );
        if(!passwordEncoder.matches(loginRequest.password(), user.getPasswordHash())){
            throw new BadCredentialsException("Wrong email or password");
        }
        return issueTokens(user);
    }

    /**
     * Обменивает действующий refresh-токен на новую пару (с ротацией).
     *
     * @throws BadCredentialsException если токен неизвестен или недействителен
     */
    @Transactional
    public LoginResponse refresh(String refreshToken) {
        String hash = sha256(refreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if(!stored.isActive()) {
            throw new BadCredentialsException("Refresh token is inactive or revoked");
        }
        stored.revoke();

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        return issueTokens(user);
    }

    /** Отзывает refresh-токен (logout). Молча игнорирует неизвестный токен. */
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByTokenHash(sha256(refreshToken))
                .ifPresent(RefreshToken::revoke);
    }

    private LoginResponse issueTokens(User user) {
        String access = jwtService.createAccessToken(user);
        String refresh = UUID.randomUUID().toString();

        refreshTokenRepository.save(new RefreshToken(user.getId(),
                                                    sha256(refresh),
                                                    Instant.now().plus(refreshTtl)));
        return new LoginResponse(access, refresh);
    }

    /** SHA-256 хэш токена для хранения (сам токен в БД не пишем). */
    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

}
