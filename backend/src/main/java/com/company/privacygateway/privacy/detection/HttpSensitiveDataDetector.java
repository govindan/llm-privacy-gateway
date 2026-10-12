package com.company.privacygateway.privacy.detection;

import com.company.privacygateway.common.PrivacyProcessingException;
import com.company.privacygateway.configuration.AppProperties;
import com.company.privacygateway.privacy.policy.SensitiveEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class HttpSensitiveDataDetector implements SensitiveDataDetector {

    private final RestClient restClient;
    private final AppProperties appProperties;

    public HttpSensitiveDataDetector(RestClient.Builder restClientBuilder, AppProperties appProperties) {
        this.appProperties = appProperties;
        this.restClient = restClientBuilder.baseUrl(appProperties.getPrivacyDetectorUrl()).build();
    }

    @Override
    public List<SensitiveEntity> detect(String text) {
        if (text == null) {
            throw new PrivacyProcessingException("DETECTION_INVALID_INPUT", "text must not be null");
        }
        String token = appProperties.getPrivacyDetectorToken();
        if (token == null || token.isBlank()) {
            throw new PrivacyProcessingException(
                    "DETECTION_UNAVAILABLE", "PRIVACY_DETECTOR_TOKEN is not configured");
        }
        try {
            DetectResponse response = restClient
                    .post()
                    .uri("/detect")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(token))
                    .body(new DetectRequest(text))
                    .retrieve()
                    .body(DetectResponse.class);
            if (response == null || response.entities() == null) {
                throw new PrivacyProcessingException(
                        "DETECTION_INVALID_RESPONSE", "detector returned an empty response");
            }
            return response.entities().stream()
                    .map(e -> new SensitiveEntity(e.type(), e.start(), e.end(), e.score()))
                    .toList();
        } catch (PrivacyProcessingException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new PrivacyProcessingException(
                    "DETECTION_UNAVAILABLE", "sensitive-data detection failed", ex);
        }
    }

    private record DetectRequest(String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DetectResponse(List<DetectEntity> entities) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DetectEntity(String type, int start, int end, Double score) {
    }
}
