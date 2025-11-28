package cl.casero.migration.web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtTokenService {

    private static final String HMAC_ALG = "HmacSHA256";
    private static final String TYP = "JWT";
    private static final String ALG = "HS256";

    private final JwtProperties properties;
    private final ObjectMapper objectMapper;

    public JwtTokenService(JwtProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public String generateToken(Long userId, String name, String role, String fingerprint) {
        String secret = properties.getSecret();
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("security.jwt.secret no está configurado");
        }
        Instant now = Instant.now();
        Instant exp = now.plus(properties.getTtl());

        Map<String, Object> header = Map.of("alg", ALG, "typ", TYP);
        Map<String, Object> payload = new HashMap<>();
        payload.put("sub", fingerprint);
        payload.put("uid", userId);
        payload.put("name", name);
        payload.put("role", role);
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", exp.getEpochSecond());

        String headerJson = toBase64Url(header);
        String payloadJson = toBase64Url(payload);
        String signingInput = headerJson + "." + payloadJson;
        String signature = sign(signingInput, secret);
        return signingInput + "." + signature;
    }

    public Optional<TokenPayload> parse(String token) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return Optional.empty();
        }
        String signingInput = parts[0] + "." + parts[1];
        String secret = properties.getSecret();
        if (!StringUtils.hasText(secret)) {
            return Optional.empty();
        }
        String expectedSignature = sign(signingInput, secret);
        if (!constantTimeEquals(expectedSignature, parts[2])) {
            return Optional.empty();
        }
        Map<String, Object> payload = fromBase64Url(parts[1]);
        Long exp = toLong(payload.get("exp"));
        if (exp != null && Instant.now().getEpochSecond() >= exp) {
            return Optional.empty();
        }
        String sub = toString(payload.get("sub"));
        Long userId = toLong(payload.get("uid"));
        String name = toString(payload.get("name"));
        String role = toString(payload.get("role"));
        if (!StringUtils.hasText(sub)) {
            return Optional.empty();
        }
        return Optional.of(new TokenPayload(userId, name, role, sub));
    }

    private String toBase64Url(Map<String, Object> content) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(content);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (Exception ex) {
            throw new IllegalStateException("Error serializando token", ex);
        }
    }

    private Map<String, Object> fromBase64Url(String part) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(part);
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(decoded, Map.class);
            return map;
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String sign(String signingInput, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALG));
            byte[] signature = mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception ex) {
            throw new IllegalStateException("Error firmando token", ex);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        if (aBytes.length != bBytes.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        return result == 0;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String toString(Object value) {
        return value == null ? null : value.toString();
    }

    public record TokenPayload(Long userId, String name, String role, String fingerprint) {
    }
}
