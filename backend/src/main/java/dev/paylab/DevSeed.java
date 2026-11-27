package dev.paylab;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
class DevSeed {
    @Bean
    ApplicationRunner seedMerchants(Accounts accounts) {
        return args -> {
            accounts.create("Avery Morgan", "avery@example.test", "PayLabDemo21");
            accounts.create("Jordan Lee", "jordan@example.test", "PayLabDemo21");
        };
    }
}
