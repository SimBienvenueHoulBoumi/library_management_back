package com.simdev.library.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom entry point to prevent browser basic auth popups.
 * Returns a JSON payload so the frontend can render its own modal/page.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "");

        Map<String, Object> payload = new HashMap<>();
        payload.put("status", HttpStatus.UNAUTHORIZED.value());
        payload.put("error", "Unauthorized");
        payload.put("message", "Authentication is required to access this resource.");
        payload.put("path", request.getRequestURI());

        response.getOutputStream().write(objectMapper.writeValueAsBytes(payload));
    }
}


