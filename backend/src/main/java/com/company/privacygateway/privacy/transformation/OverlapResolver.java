package com.company.privacygateway.privacy.transformation;

import com.company.privacygateway.common.PrivacyProcessingException;
import com.company.privacygateway.privacy.policy.SensitiveEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Deterministic overlap resolution (D-010).
 */
@Component
public class OverlapResolver {

    public List<SensitiveEntity> resolve(List<SensitiveEntity> entities) {
        if (entities == null) {
            throw new PrivacyProcessingException("OVERLAP_INVALID_INPUT", "entities must not be null");
        }
        try {
            List<SensitiveEntity> sorted = entities.stream()
                    .sorted(Comparator
                            .comparingInt(SensitiveEntity::start)
                            .thenComparing(Comparator.comparingInt(SensitiveEntity::end).reversed())
                            .thenComparing(SensitiveEntity::type))
                    .toList();

            List<SensitiveEntity> accepted = new ArrayList<>();
            for (SensitiveEntity candidate : sorted) {
                validateOffsets(candidate);
                boolean dropCandidate = false;
                List<SensitiveEntity> toRemove = new ArrayList<>();
                for (SensitiveEntity existing : accepted) {
                    if (!overlaps(candidate, existing)) {
                        continue;
                    }
                    int candidateLen = length(candidate);
                    int existingLen = length(existing);
                    if (fullyContains(existing, candidate)) {
                        dropCandidate = true;
                        break;
                    }
                    if (fullyContains(candidate, existing)) {
                        toRemove.add(existing);
                        continue;
                    }
                    // Partial overlap
                    if (candidateLen > existingLen) {
                        toRemove.add(existing);
                    } else if (candidateLen < existingLen) {
                        dropCandidate = true;
                        break;
                    } else {
                        int cmp = Double.compare(scoreOf(candidate), scoreOf(existing));
                        if (cmp > 0) {
                            toRemove.add(existing);
                        } else if (cmp < 0) {
                            dropCandidate = true;
                            break;
                        } else if (candidate.start() < existing.start()) {
                            toRemove.add(existing);
                        } else {
                            dropCandidate = true;
                            break;
                        }
                    }
                }
                if (!dropCandidate) {
                    accepted.removeAll(toRemove);
                    accepted.add(candidate);
                }
            }

            return accepted.stream()
                    .sorted(Comparator
                            .comparingInt(SensitiveEntity::start)
                            .thenComparing(Comparator.comparingInt(SensitiveEntity::end).reversed()))
                    .toList();
        } catch (PrivacyProcessingException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new PrivacyProcessingException(
                    "OVERLAP_RESOLUTION_FAILED", "unable to resolve overlapping entities", ex);
        }
    }

    private static void validateOffsets(SensitiveEntity entity) {
        if (entity.start() < 0 || entity.end() < entity.start()) {
            throw new PrivacyProcessingException(
                    "OVERLAP_INVALID_OFFSETS",
                    "invalid entity offsets for type " + entity.type());
        }
    }

    private static boolean overlaps(SensitiveEntity a, SensitiveEntity b) {
        return a.start() < b.end() && b.start() < a.end();
    }

    private static boolean fullyContains(SensitiveEntity outer, SensitiveEntity inner) {
        return outer.start() <= inner.start() && outer.end() >= inner.end()
                && length(outer) > length(inner);
    }

    private static int length(SensitiveEntity entity) {
        return entity.end() - entity.start();
    }

    private static double scoreOf(SensitiveEntity entity) {
        return entity.score() == null ? Double.NEGATIVE_INFINITY : entity.score();
    }
}
