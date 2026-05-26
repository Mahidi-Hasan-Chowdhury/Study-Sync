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

        // Programmatic cleanup of environment variables and system properties to prevent trailing newlines, quotes, or whitespace
        String[] keysToClean = {
            "GROQ_API_KEY", "GROQ_MODEL", "MONGODB_URI", 
            "SPRING_PROFILES_ACTIVE", "STRIPE_PUBLIC_KEY", "STRIPE_SECRET_KEY"
        };
        for (String key : keysToClean) {
            String val = System.getenv(key);
            if (val == null) {
                val = System.getProperty(key);
            }
            if (val != null) {
                String original = val;
                val = val.trim();
                // Strip literal or escaped quotes
                if (val.startsWith("\"") && val.endsWith("\"")) {
                    val = val.substring(1, val.length() - 1).trim();
                }
                if (val.startsWith("'") && val.endsWith("'")) {
                    val = val.substring(1, val.length() - 1).trim();
                }
                // Strip escaped newline representation '\n'
                if (val.endsWith("\\n")) {
                    val = val.substring(0, val.length() - 2).trim();
                }
                // Strip actual newline characters
                val = val.replace("\n", "").replace("\r", "").trim();
                
                if (!val.equals(original)) {
                    System.setProperty(key, val);
                    System.out.println(">>> Sanitized env var: " + key + " (removed quotes/newlines)");
                }
            }
        }

        SpringApplication.run(StudySyncApplication.class, args);
    }

}
