package cl.casero.migration.api.dto;

public record AuthTokenResponse(String token, String tokenType) {

    public static AuthTokenResponse bearer(String token) {
        return new AuthTokenResponse(token, "Bearer");
    }
}
