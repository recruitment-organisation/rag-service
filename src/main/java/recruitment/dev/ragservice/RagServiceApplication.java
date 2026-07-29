package recruitment.dev.ragservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableFeignClients
public class RagServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagServiceApplication.class, args);
    }

    @Bean
    ApplicationRunner validateGeminiConfiguration(
            @Value("${spring.ai.google.genai.api-key:}") String apiKey
    ) {
        return arguments -> {
            if (apiKey.isBlank()) {
                throw new IllegalStateException("GEMINI_API_KEY must be configured for RAG analysis");
            }
        };
    }
}
