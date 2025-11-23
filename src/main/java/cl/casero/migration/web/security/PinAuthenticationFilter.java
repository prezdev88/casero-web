package cl.casero.migration.web.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

public class PinAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    public PinAuthenticationFilter() {
        super(new AntPathRequestMatcher("/login", "POST"));
        SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setDefaultTargetUrl("/customers");
        successHandler.setAlwaysUseDefaultTargetUrl(true);
        setAuthenticationSuccessHandler(successHandler);
        setAuthenticationFailureHandler(new SimpleUrlAuthenticationFailureHandler("/login?error"));
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException, ServletException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            throw new AuthenticationServiceException("Método no soportado: " + request.getMethod());
        }
        String pin = obtainPin(request);
        if (pin == null || pin.isBlank()) {
            throw new BadCredentialsException("Debes ingresar tu PIN");
        }
        PinAuthenticationToken authRequest = new PinAuthenticationToken(pin.trim());
        setDetails(request, authRequest);
        return this.getAuthenticationManager().authenticate(authRequest);
    }

    protected void setDetails(HttpServletRequest request, PinAuthenticationToken authRequest) {
        authRequest.setDetails(authenticationDetailsSource.buildDetails(request));
    }

    protected String obtainPin(HttpServletRequest request) {
        return request.getParameter("pin");
    }
}
