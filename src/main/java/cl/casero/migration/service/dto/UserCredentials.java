package cl.casero.migration.service.dto;

public record UserCredentials(UserIdentity identity, String pinFingerprint, String pinHash, String pinSalt) {

    @Override
    public String toString() {
        return "UserCredentials[identity=" + identity + "]";
    }
}
