package bd.edu.seu.studysync.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;




import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

@Configuration
public class ApiKeyLogger {

    @Value("${spring.ai.openai.api-key:<<MISSING>>}")
    private String apiKey;

    @PostConstruct
    void log() {
        System.out.println("🔑 API key resolved = " + (apiKey == null || apiKey.isBlank() ? "<<MISSING>>" : "****" + apiKey.substring(Math.max(0, apiKey.length() - 4))));
    }
}
