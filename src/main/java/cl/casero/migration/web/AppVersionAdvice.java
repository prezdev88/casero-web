package cl.casero.migration.web;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@ControllerAdvice
public class AppVersionAdvice {

    private final String version;

    public AppVersionAdvice() {
        this.version = resolveVersion();
    }

    @ModelAttribute("appVersion")
    public String appVersion() {
        return version;
    }

    private String resolveVersion() {
        String resolved = readPomProperties("/META-INF/maven/cl.casero/casero-web/pom.properties");

        if (resolved != null) {
            return resolved;
        }

        resolved = readPomProperties("target/maven-archiver/pom.properties");

        if (resolved != null) {
            return resolved;
        }

        resolved = readPomVersionFromPomXml();

        return resolved != null ? resolved : "0.0.0";
    }

    private String readPomProperties(String location) {
        boolean classpathResource = location.startsWith("/");

        try (InputStream inputStream = classpathResource
                ? getClass().getResourceAsStream(location)
                : Files.exists(Path.of(location)) ? Files.newInputStream(Path.of(location)) : null) {
            if (inputStream == null) {
                return null;
            }
            Properties properties = new Properties();
            properties.load(inputStream);
            String value = properties.getProperty("version");

            return value != null && !value.isBlank() ? value.trim() : null;
        } catch (IOException ignored) {
            return null;
        }
    }

    private String readPomVersionFromPomXml() {
        Path pomPath = Path.of("pom.xml");

        if (!Files.exists(pomPath)) {
            return null;
        }

        try (InputStream inputStream = Files.newInputStream(pomPath)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            Document document = factory.newDocumentBuilder().parse(inputStream);
            NodeList versions = document.getDocumentElement().getElementsByTagName("version");
            for (int i = 0; i < versions.getLength(); i++) {
                Node node = versions.item(i);
                if (node.getParentNode() == document.getDocumentElement()) {
                    String value = node.getTextContent();
                    if (value != null && !value.isBlank()) {
                        return value.trim();
                    }
                    break;
                }
            }
        } catch (Exception ignored) {
        }
        
        return null;
    }
}
