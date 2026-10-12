package com.company.privacygateway.privacy.response;

import com.company.privacygateway.common.PrivacyProcessingException;
import com.company.privacygateway.privacy.detection.SensitiveDataDetector;
import com.company.privacygateway.privacy.policy.SensitiveEntity;
import com.company.privacygateway.privacy.transformation.OverlapResolver;
import com.company.privacygateway.privacy.transformation.PrivacyProcessingService;
import com.company.privacygateway.vault.TokenMapping;
import com.company.privacygateway.vault.TokenVault;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Response inspection and controlled re-identification (D-013, D-014).
 * The LLM never calls the vault; only this service may resolve tokens after authorization.
 */
@Service
public class ResponsePrivacyService {

    static final String REDACTED = "[REDACTED]";
    private static final Pattern TOKEN_PATTERN = Pattern.compile("<<([A-Z][A-Z0-9_]*):([0-9A-Fa-f]{16})>>");

    private final SensitiveDataDetector detector;
    private final OverlapResolver overlapResolver;
    private final TokenVault tokenVault;

    public ResponsePrivacyService(
            SensitiveDataDetector detector,
            OverlapResolver overlapResolver,
            TokenVault tokenVault) {
        this.detector = detector;
        this.overlapResolver = overlapResolver;
        this.tokenVault = tokenVault;
    }

    public ResponsePrivacyResult process(
            String tenantId,
            String conversationId,
            String llmResponseText,
            boolean reidentifyAuthorized) {
        if (llmResponseText == null) {
            throw new PrivacyProcessingException("RESPONSE_INVALID_INPUT", "llm response must not be null");
        }

        List<TokenSpan> tokenSpans = findTokenSpans(llmResponseText);

        List<SensitiveEntity> detected;
        try {
            detected = detector.detect(llmResponseText);
        } catch (PrivacyProcessingException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new PrivacyProcessingException(
                    "RESPONSE_DETECTION_FAILED",
                    "response sensitive-data detection failed",
                    ex);
        }

        List<SensitiveEntity> novel = detected.stream()
                .filter(entity -> !overlapsAnyToken(entity, tokenSpans))
                .toList();
        List<SensitiveEntity> toRedact = overlapResolver.resolve(novel);

        List<SpanReplacement> replacements = new ArrayList<>();
        for (SensitiveEntity entity : toRedact) {
            replacements.add(new SpanReplacement(entity.start(), entity.end(), REDACTED));
        }

        int resolvedCount = 0;
        for (TokenSpan tokenSpan : tokenSpans) {
            if (reidentifyAuthorized) {
                Optional<TokenMapping> mapping =
                        tokenVault.resolve(tenantId, conversationId, tokenSpan.tokenId());
                if (mapping.isPresent()) {
                    String plaintext = tokenVault.decryptValue(mapping.get());
                    replacements.add(new SpanReplacement(tokenSpan.startCp(), tokenSpan.endCp(), plaintext));
                    resolvedCount++;
                    continue;
                }
            }
            replacements.add(new SpanReplacement(
                    tokenSpan.startCp(), tokenSpan.endCp(), tokenSpan.tokenId()));
        }

        replacements.sort(Comparator
                .comparingInt(SpanReplacement::startCp)
                .thenComparing(Comparator.comparingInt(SpanReplacement::endCp).reversed()));

        String userFacing = applyReplacements(llmResponseText, replacements);
        validateRedactions(userFacing, llmResponseText, toRedact);

        return new ResponsePrivacyResult(
                llmResponseText,
                userFacing,
                reidentifyAuthorized,
                tokenSpans.size(),
                resolvedCount,
                toRedact.size());
    }

    private static void validateRedactions(
            String userFacing, String original, List<SensitiveEntity> redacted) {
        for (SensitiveEntity entity : redacted) {
            String value = PrivacyProcessingService.codePointSlice(original, entity.start(), entity.end());
            if (userFacing.contains(value)) {
                throw new PrivacyProcessingException(
                        "RESPONSE_REDACTION_FAILED",
                        "new sensitive value remained in response for type " + entity.type());
            }
        }
    }

    private static String applyReplacements(String text, List<SpanReplacement> replacements) {
        if (replacements.isEmpty()) {
            return text;
        }
        StringBuilder out = new StringBuilder();
        int cursorCp = 0;
        int totalCp = text.codePointCount(0, text.length());
        for (SpanReplacement replacement : replacements) {
            if (replacement.startCp() < cursorCp) {
                throw new PrivacyProcessingException(
                        "RESPONSE_OFFSET_ERROR", "overlapping response replacements");
            }
            if (replacement.startCp() > cursorCp) {
                out.append(PrivacyProcessingService.codePointSlice(text, cursorCp, replacement.startCp()));
            }
            out.append(replacement.replacement());
            cursorCp = replacement.endCp();
        }
        if (cursorCp < totalCp) {
            out.append(PrivacyProcessingService.codePointSlice(text, cursorCp, totalCp));
        }
        return out.toString();
    }

    private static List<TokenSpan> findTokenSpans(String text) {
        List<TokenSpan> spans = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(text);
        while (matcher.find()) {
            int startIdx = matcher.start();
            int endIdx = matcher.end();
            int startCp = text.codePointCount(0, startIdx);
            int endCp = startCp + text.codePointCount(startIdx, endIdx);
            spans.add(new TokenSpan(matcher.group(), startCp, endCp));
        }
        return spans;
    }

    private static boolean overlapsAnyToken(SensitiveEntity entity, List<TokenSpan> tokenSpans) {
        for (TokenSpan token : tokenSpans) {
            if (entity.start() < token.endCp() && token.startCp() < entity.end()) {
                return true;
            }
        }
        return false;
    }

    private record TokenSpan(String tokenId, int startCp, int endCp) {
    }

    private record SpanReplacement(int startCp, int endCp, String replacement) {
    }
}
