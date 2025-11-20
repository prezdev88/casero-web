package cl.casero.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CaseroWebApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CaseroWebApplication.class);

    @Value("${casero.demo.message:Perfil no definido}")
    private String demoMessage;

    public static void main(String[] args) {
        SpringApplication.run(CaseroWebApplication.class, args);
    }

    @Override
    public void run(String... args) {
        log.info("Mensaje de perfil activo: {}", demoMessage);
    }
}
