package com.company.privacygateway.configuration;

import com.company.privacygateway.vault.AesGcmEncryptionService;
import com.company.privacygateway.vault.EncryptionService;
import com.company.privacygateway.vault.HmacValueHasher;
import com.company.privacygateway.vault.ValueHasher;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({AppProperties.class, LlmProperties.class})
public class VaultCryptoConfiguration {

    @Bean
    EncryptionService encryptionService(AppProperties properties) {
        return new AesGcmEncryptionService(properties.getVaultEncryptionKey());
    }

    @Bean
    ValueHasher valueHasher(AppProperties properties) {
        String hashKey = properties.getVaultHashKey();
        if (hashKey == null || hashKey.isBlank()) {
            hashKey = properties.getVaultEncryptionKey();
        }
        return new HmacValueHasher(hashKey);
    }
}
