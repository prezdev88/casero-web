package cl.casero.migration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class WebVersionHolder {

    private String version;

    public WebVersionHolder() {}

    public void init() {
        version = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
    }

    public String getVersion() {
        return version;
    }
}
