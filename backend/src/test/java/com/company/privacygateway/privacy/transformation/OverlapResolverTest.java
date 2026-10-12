package com.company.privacygateway.privacy.transformation;

import com.company.privacygateway.privacy.policy.SensitiveEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OverlapResolverTest {

    private final OverlapResolver resolver = new OverlapResolver();

    @Test
    void dropsFullyContainedShorterSpan() {
        SensitiveEntity outer = new SensitiveEntity("ADDRESS", 0, 20, 0.7);
        SensitiveEntity inner = new SensitiveEntity("PERSON", 5, 10, 0.9);

        List<SensitiveEntity> resolved = resolver.resolve(List.of(inner, outer));

        assertThat(resolved).containsExactly(outer);
    }

    @Test
    void partialOverlapKeepsLongerSpan() {
        SensitiveEntity left = new SensitiveEntity("PERSON", 0, 10, 0.5);
        SensitiveEntity right = new SensitiveEntity("EMAIL", 5, 20, 0.5);

        List<SensitiveEntity> resolved = resolver.resolve(List.of(left, right));

        assertThat(resolved).containsExactly(right);
    }

    @Test
    void equalLengthUsesHigherConfidence() {
        SensitiveEntity low = new SensitiveEntity("PERSON", 0, 10, 0.4);
        SensitiveEntity high = new SensitiveEntity("EMAIL", 5, 15, 0.9);

        List<SensitiveEntity> resolved = resolver.resolve(List.of(low, high));

        assertThat(resolved).containsExactly(high);
    }

    @Test
    void equalLengthEqualScoreKeepsLeftmost() {
        SensitiveEntity left = new SensitiveEntity("PERSON", 0, 10, 0.8);
        SensitiveEntity right = new SensitiveEntity("EMAIL", 5, 15, 0.8);

        List<SensitiveEntity> resolved = resolver.resolve(List.of(right, left));

        assertThat(resolved).containsExactly(left);
    }

    @Test
    void nonOverlappingEntitiesAreAllKeptSorted() {
        SensitiveEntity first = new SensitiveEntity("PERSON", 0, 4, 0.8);
        SensitiveEntity second = new SensitiveEntity("EMAIL", 10, 20, 0.9);

        List<SensitiveEntity> resolved = resolver.resolve(List.of(second, first));

        assertThat(resolved).containsExactly(first, second);
    }
}
