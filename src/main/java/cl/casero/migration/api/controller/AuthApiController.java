package cl.casero.migration.api.controller;

import cl.casero.migration.api.dto.AuthLoginRequest;
import cl.casero.migration.api.dto.AuthTokenResponse;
import cl.casero.migration.api.dto.AuthUserResponse;
import cl.casero.migration.web.security.CaseroUserDetails;
import cl.casero.migration.web.security.JwtTokenService;
import cl.casero.migration.web.security.PinAuthenticationToken;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    public AuthApiController(AuthenticationManager authenticationManager,
                             JwtTokenService jwtTokenService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
    }

    @GetMapping("/me")
    public AuthUserResponse me(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CaseroUserDetails userDetails)) {
            return new AuthUserResponse(null, null, null, false);
        }
        return new AuthUserResponse(
                userDetails.getUser().getId(),
                userDetails.getUser().getName(),
                userDetails.getUser().getRole(),
                true);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public AuthTokenResponse login(@Valid @RequestBody AuthLoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(new PinAuthenticationToken(request.pin()));
        if (!(authentication.getPrincipal() instanceof CaseroUserDetails userDetails)) {
            throw new BadCredentialsException("Credenciales inválidas");
        }
        String token = jwtTokenService.generateToken(
                userDetails.getUser().getId(),
                userDetails.getUser().getName(),
                userDetails.getUser().getRole().name(),
                userDetails.getUser().getPinFingerprint());
        return AuthTokenResponse.bearer(token);
    }
}
