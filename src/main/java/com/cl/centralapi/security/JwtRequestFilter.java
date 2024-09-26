package com.cl.centralapi.security;

import com.cl.centralapi.service.CustomUserDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");
        logger.debug("Authorization Header: " + authorizationHeader);

        String username = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            logger.debug("Extracted JWT: " + jwt);
            try {
                username = jwtUtil.extractUsername(jwt);
                logger.debug("Extracted Username from JWT: " + username);
            } catch (ExpiredJwtException ex) {
                // If the token has expired, set an attribute so the entry point can handle it
                request.setAttribute("expired", "true");
                logger.debug("JWT Token has expired");
            }
        } else {
            logger.debug("JWT Token not found in Authorization Header or Bearer prefix missing.");
        }

        // Proceed only if username is available and SecurityContext doesn't have an authentication
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            var userDetails = this.userDetailsService.loadUserByUsername(username);
            if (jwtUtil.validateToken(jwt, userDetails.getUsername())) {
                logger.debug("JWT Token is valid for user: " + username);
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                logger.debug("SecurityContext updated for user: " + username);
            } else {
                logger.debug("JWT Token validation failed for user: " + username);
            }
        } else {
            logger.debug("Username is null or user already authenticated.");
        }

        chain.doFilter(request, response);
    }
}
