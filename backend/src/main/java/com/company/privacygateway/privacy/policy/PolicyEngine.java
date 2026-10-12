package com.company.privacygateway.privacy.policy;

public interface PolicyEngine {

    PolicyDecision evaluate(SensitiveEntity entity, PrivacyPolicy policy);
}
