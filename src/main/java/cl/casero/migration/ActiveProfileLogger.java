package cl.casero.migration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ActiveProfileLogger implements CommandLineRunner {

    private final String profileMessage;

    public ActiveProfileLogger(@Value("${active.profile.message:Perfil no definido}") String profileMessage) {
        this.profileMessage = profileMessage;
    }

    @Override
    public void run(String... args) {
        log.info("Mensaje de perfil activo: {}", profileMessage);
    }
}
