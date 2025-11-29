package com.simdev.library.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre pour ignorer silencieusement les requêtes vers des ressources statiques inexistantes
 * qui génèrent des warnings inutiles (ex: /wsagents, /api/pool/metrics)
 */
@Component
@Order(1)
public class ResourceNotFoundFilter extends OncePerRequestFilter {
    
    private static final String[] IGNORED_PATHS = {
        "/wsagents",
        "/api/pool/metrics"
    };
    
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, 
                                   @NonNull HttpServletResponse response, 
                                   @NonNull FilterChain filterChain) 
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // Ignorer silencieusement les chemins connus qui génèrent des warnings
        for (String ignoredPath : IGNORED_PATHS) {
            if (path.startsWith(ignoredPath)) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
}

