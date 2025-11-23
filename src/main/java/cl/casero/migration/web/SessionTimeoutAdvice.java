package cl.casero.migration.web;

import java.time.Duration;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class SessionTimeoutAdvice {

    private final Long sessionTimeoutSeconds;

    public SessionTimeoutAdvice(ServerProperties serverProperties) {
        Duration timeout = serverProperties.getServlet().getSession().getTimeout();
        this.sessionTimeoutSeconds = timeout != null ? timeout.toSeconds() : null;
    }

    @ModelAttribute("sessionTimeoutSeconds")
    public Long sessionTimeoutSeconds() {
        return sessionTimeoutSeconds;
    }
}
