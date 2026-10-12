package com.company.privacygateway.security;

import com.company.privacygateway.auth.GatewayPrincipal;
import com.company.privacygateway.auth.JwtTokenService;
import com.company.privacygateway.configuration.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AppProperties appProperties) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/actuator/health", "/actuator/health/**").permitAll();
                    if (appProperties.isDevTokenEnabled()) {
                        auth.requestMatchers(HttpMethod.POST, "/api/v1/dev/token").permitAll();
                    }
                    auth.requestMatchers("/api/v1/**").authenticated().anyRequest().denyAll();
                })
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(this::toAuthentication)));
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder(JwtTokenService jwtTokenService) {
        return NimbusJwtDecoder.withSecretKey(jwtTokenService.secretKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    private AbstractAuthenticationToken toAuthentication(Jwt jwt) {
        String userId = jwt.getSubject();
        String tenantId = jwt.getClaimAsString("tenant_id");
        Boolean reidentify = jwt.getClaim("reidentify");
        if (userId == null || userId.isBlank() || tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("JWT must include sub and tenant_id");
        }
        GatewayPrincipal principal = new GatewayPrincipal(userId, tenantId, Boolean.TRUE.equals(reidentify));
        return new AbstractAuthenticationToken(List.of()) {
            @Override
            public Object getCredentials() {
                return jwt.getTokenValue();
            }

            @Override
            public Object getPrincipal() {
                return principal;
            }

            @Override
            public boolean isAuthenticated() {
                return true;
            }
        };
    }
}
