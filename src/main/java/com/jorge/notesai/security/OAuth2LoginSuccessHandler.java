package com.jorge.notesai.security;

import com.jorge.notesai.entity.User;
import com.jorge.notesai.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        // Busca el usuario por email; si no existe (primera vez que entra con Google), lo crea.
        // Si ya existía por registro normal (email+password), simplemente lo reutiliza:
        // así una misma persona puede entrar con contraseña o con Google indistintamente.
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .email(email)
                                .name(name != null ? name : email)
                                .password(null) // sin password propia, solo entra vía Google
                                .build()
                ));

        String token = jwtUtil.generateToken(user.getEmail());

        // Redirige al frontend con el token en la URL, para que el frontend lo guarde
        // (mismo patrón que el login normal, solo que aquí llega por query param en vez de JSON)
        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/redirect")
                .queryParam("token", token)
                .queryParam("email", user.getEmail())
                .queryParam("name", user.getName())
                .encode(java.nio.charset.StandardCharsets.UTF_8)
                .build().toUriString();

        response.sendRedirect(targetUrl);
    }
}