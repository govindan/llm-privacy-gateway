package com.company.privacygateway.vault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JdbcTokenVaultTest {

    private static final AtomicReference<Instant> NOW =
            new AtomicReference<>(Instant.parse("2026-09-14T12:00:00Z"));

    @TestConfiguration
    static class MutableClockConfig {
        @Bean
        @Primary
        Clock testClock() {
            return new Clock() {
                @Override
                public ZoneOffset getZone() {
                    return ZoneOffset.UTC;
                }

                @Override
                public Clock withZone(java.time.ZoneId zone) {
                    return Clock.fixed(instant(), zone);
                }

                @Override
                public Instant instant() {
                    return NOW.get();
                }
            };
        }
    }

    @Autowired
    private TokenVault tokenVault;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetClock() {
        NOW.set(Instant.parse("2026-09-14T12:00:00Z"));
    }

    @Test
    void createOrGetStoresCiphertextNotPlaintextAndReusesWithinScope() {
        String plaintext = "john.smith@example.com";

        TokenMapping first = tokenVault.createOrGet(
                "tenant-a", "conv-1", "EMAIL", plaintext, "user-1");
        TokenMapping second = tokenVault.createOrGet(
                "tenant-a", "conv-1", "EMAIL", plaintext, "user-1");

        assertThat(first.tokenId()).isEqualTo(second.tokenId());
        assertThat(first.tokenId()).startsWith("<<EMAIL:").endsWith(">>");
        assertThat(first.tokenId()).doesNotContain(plaintext);
        assertThat(first.encryptedValue()).doesNotContain(plaintext);
        assertThat(tokenVault.decryptValue(first)).isEqualTo(plaintext);

        List<String> stored = jdbcTemplate.query(
                "SELECT encrypted_value FROM token_mapping WHERE id = ?",
                (rs, rowNum) -> rs.getString(1),
                first.id().toString());
        assertThat(stored).hasSize(1);
        assertThat(stored.getFirst()).doesNotContain(plaintext);
        assertThat(stored.getFirst()).isEqualTo(first.encryptedValue());

        Integer plaintextHits = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM token_mapping
                WHERE encrypted_value LIKE ? OR value_hash LIKE ? OR token_id LIKE ?
                """,
                Integer.class,
                "%" + plaintext + "%",
                "%" + plaintext + "%",
                "%" + plaintext + "%");
        assertThat(plaintextHits).isZero();
    }

    @Test
    void differentTenantCannotResolveToken() {
        TokenMapping mapping = tokenVault.createOrGet(
                "tenant-a", "conv-1", "PERSON", "John Smith", "user-1");

        Optional<TokenMapping> foreign = tokenVault.resolve("tenant-b", "conv-1", mapping.tokenId());
        Optional<TokenMapping> own = tokenVault.resolve("tenant-a", "conv-1", mapping.tokenId());

        assertThat(foreign).isEmpty();
        assertThat(own).isPresent();
        assertThat(tokenVault.decryptValue(own.get())).isEqualTo("John Smith");
    }

    @Test
    void differentConversationDoesNotShareToken() {
        TokenMapping first = tokenVault.createOrGet(
                "tenant-a", "conv-1", "ACCOUNT_NUMBER", "123456789", "user-1");
        TokenMapping second = tokenVault.createOrGet(
                "tenant-a", "conv-2", "ACCOUNT_NUMBER", "123456789", "user-1");

        assertThat(first.tokenId()).isNotEqualTo(second.tokenId());
        assertThat(tokenVault.resolve("tenant-a", "conv-1", second.tokenId())).isEmpty();
        assertThat(tokenVault.resolve("tenant-a", "conv-2", first.tokenId())).isEmpty();
    }

    @Test
    void expiredMappingIsNotResolvableOrReusable() {
        TokenMapping mapping = tokenVault.createOrGet(
                "tenant-a", "conv-exp", "TRANSACTION_ID", "TX998877", "user-1");

        NOW.set(Instant.parse("2026-09-16T12:00:00Z"));

        assertThat(tokenVault.resolve("tenant-a", "conv-exp", mapping.tokenId())).isEmpty();

        TokenMapping replacement = tokenVault.createOrGet(
                "tenant-a", "conv-exp", "TRANSACTION_ID", "TX998877", "user-1");
        assertThat(replacement.tokenId()).isNotEqualTo(mapping.tokenId());
        assertThat(tokenVault.resolve("tenant-a", "conv-exp", replacement.tokenId())).isPresent();
    }
}
