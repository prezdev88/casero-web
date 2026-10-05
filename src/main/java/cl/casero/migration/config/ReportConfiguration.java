package cl.casero.migration.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ReportConfiguration {

    @Bean
    public Clock reportClock() {
        return Clock.systemDefaultZone();
    }
}
