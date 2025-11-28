package cl.casero.migration.web.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    /**
     * Llave secreta para firmar tokens. Establécela vía env var SECURITY_JWT_SECRET.
     */
    private String secret;

    /**
     * Duración del token. Ej: PT12H, PT24H, P1D.
     */
    private Duration ttl = Duration.ofHours(12);

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }
}
