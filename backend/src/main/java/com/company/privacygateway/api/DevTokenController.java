package com.company.privacygateway.api;

import com.company.privacygateway.auth.JwtTokenService;
import com.company.privacygateway.configuration.AppProperties;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Local/dev JWT mint only. Disabled unless app.dev-token-enabled=true.
 */
@RestController
@RequestMapping("/api/v1/dev")
@ConditionalOnProperty(name = "app.dev-token-enabled", havingValue = "true")
public class DevTokenController {

    private final JwtTokenService jwtTokenService;

    public DevTokenController(JwtTokenService jwtTokenService, AppProperties ignored) {
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/token")
    public DevTokenResponse mint(@RequestBody DevTokenRequest request) {
        String token = jwtTokenService.mint(
                request.userId(),
                request.tenantId(),
                request.reidentify(),
                Instant.now().plus(8, ChronoUnit.HOURS));
        return new DevTokenResponse(token);
    }

    public record DevTokenRequest(
            @NotBlank String userId, @NotBlank String tenantId, boolean reidentify) {
    }

    public record DevTokenResponse(String accessToken) {
    }
}
