package cl.casero.migration.web.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService tokenService;
    private final PinAuthenticationProvider pinAuthenticationProvider;

    public JwtAuthenticationFilter(JwtTokenService tokenService,
                                   PinAuthenticationProvider pinAuthenticationProvider) {
        this.tokenService = tokenService;
        this.pinAuthenticationProvider = pinAuthenticationProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }
        String bearer = resolveBearer(request.getHeader("Authorization"));
        Optional<JwtTokenService.TokenPayload> parsed = tokenService.parse(bearer);
        if (parsed.isPresent()) {
            JwtTokenService.TokenPayload payload = parsed.get();
            CaseroUserDetails userDetails = pinAuthenticationProvider.loadUserDetails(payload.fingerprint());
            if (userDetails != null) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveBearer(String header) {
        if (!StringUtils.hasText(header)) {
            return null;
        }
        if (header.toLowerCase().startsWith("bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }
}
