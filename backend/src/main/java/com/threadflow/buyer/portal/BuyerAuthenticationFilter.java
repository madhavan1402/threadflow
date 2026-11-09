package com.threadflow.buyer.portal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class BuyerAuthenticationFilter extends OncePerRequestFilter {
    private final BuyerAccessTokenRepository tokenRepository;

    public BuyerAuthenticationFilter(BuyerAccessTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null && !token.isBlank()) {
            tokenRepository.findByTokenValue(token).ifPresent(accessToken -> {
                if (accessToken.isValid()) {
                    SecurityContextHolder.getContext().setAuthentication(
                            new BuyerAuthenticationToken(accessToken.getBuyer())
                    );
                }
            });
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("X-Buyer-Token");
        if (header != null && !header.isBlank()) {
            return header.trim();
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String candidate = authHeader.substring(7).trim();
            // Distinguish buyer access tokens (e.g. tf_buyer_ or buyer-token- or not a 3-part dot JWT)
            if (candidate.startsWith("tf_buyer_") || candidate.startsWith("buyer-token-") || candidate.chars().filter(ch -> ch == '.').count() != 2) {
                return candidate;
            }
        }

        String param = request.getParameter("buyerToken");
        if (param != null && !param.isBlank()) {
            return param.trim();
        }

        return null;
    }
}
