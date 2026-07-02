package com.dating.core.common.security;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Фильтр, проверяющий JWT в заголовке {@code Authorization: Bearer ...}
 * и устанавливающий аутентификацию в {@link SecurityContextHolder}.
 *
 * <p>Выполняется один раз на запрос ({@link OncePerRequestFilter}).
 * Если токена нет или он невалиден — фильтр просто не аутентифицирует
 * запрос; решение о доступе примет цепочка Spring Security.
 */
@Component
public class JWTAuthFilter extends OncePerRequestFilter {
    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JWTService jwtService;

    public JWTAuthFilter(JWTService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal( @NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain chain)
            throws IOException, ServletException {
        String header = request.getHeader(HEADER);

        if(header != null && header.startsWith(PREFIX)) {
            String token = header.substring(PREFIX.length());

            // parseClaims — единственная проверка подписи; subject и role
            // достаём из уже распарсенных claims, не парся токен повторно
            jwtService.parseClaims(token).ifPresent(claims -> {
                UUID userId = UUID.fromString(claims.getSubject());
                String role = claims.get("role", String.class);

                var authority = new SimpleGrantedAuthority("ROLE_" + role);
                var principal = new AuthPrincipal(userId, role);

                var authentication = new UsernamePasswordAuthenticationToken(principal,
                        null, List.of(authority));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }
        chain.doFilter(request, response);
    }
}
