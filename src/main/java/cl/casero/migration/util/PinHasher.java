package cl.casero.migration.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class PinHasher {

    private static final int DEFAULT_SALT_BYTES = 8;
    private final SecureRandom secureRandom = new SecureRandom();

    public String fingerprint(String pin) {
        return sha256(pin == null ? "" : pin.trim());
    }

    public boolean matches(String rawPin, String salt, String expectedHash) {
        if (rawPin == null || salt == null || expectedHash == null) {
            return false;
        }
        String actualHash = hashWithSalt(rawPin, salt);
        return constantTimeEquals(actualHash, expectedHash);
    }

    public String hashWithSalt(String pin, String salt) {
        return sha256((pin == null ? "" : pin.trim()) + salt);
    }

    public String generateSalt() {
        byte[] salt = new byte[DEFAULT_SALT_BYTES];
        secureRandom.nextBytes(salt);
        return HexFormat.of().formatHex(salt);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }

        int result = 0;

        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        
        return result == 0;
    }
}
