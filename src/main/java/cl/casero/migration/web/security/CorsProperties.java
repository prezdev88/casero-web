package cl.casero.migration.web.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.cors")
public class CorsProperties {

    /**
     * Orígenes permitidos para consumir el API. Ej: http://localhost:5173,http://localhost:3000
     */
    private List<String> allowedOrigins = List.of("http://localhost:5173", "http://localhost:3000");

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
}
