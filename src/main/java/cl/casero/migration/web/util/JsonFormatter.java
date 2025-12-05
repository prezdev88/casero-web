package cl.casero.migration.web.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component("jsonFormatter")
public class JsonFormatter {

    private final ObjectMapper objectMapper;

    public JsonFormatter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toJson(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return payload.toString();
        }
    }
}
