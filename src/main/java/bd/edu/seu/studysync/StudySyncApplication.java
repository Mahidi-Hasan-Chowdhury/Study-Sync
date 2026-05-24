package bd.edu.seu.studysync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StudySyncApplication {

    public static void main(String[] args) {
        // Programmatic dotenv loader to make local development seamless
        try {
            java.io.File envFile = new java.io.File(".env");
            if (envFile.exists()) {
                java.nio.file.Files.lines(envFile.toPath())
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .forEach(line -> {
                        int delim = line.indexOf('=');
                        if (delim > 0) {
                            String key = line.substring(0, delim).trim();
                            String value = line.substring(delim + 1).trim();
                            // Strip quotes if present
                            if (value.startsWith("\"") && value.endsWith("\"")) {
                                value = value.substring(1, value.length() - 1);
                            } else if (value.startsWith("'") && value.endsWith("'")) {
                                value = value.substring(1, value.length() - 1);
                            }
                            System.setProperty(key, value);
                        }
                    });
                System.out.println(">>> Loaded environment variables from .env file successfully.");
            }
        } catch (Exception e) {
            System.err.println(">>> Failed to load .env file: " + e.getMessage());
        }
        SpringApplication.run(StudySyncApplication.class, args);
    }

}
