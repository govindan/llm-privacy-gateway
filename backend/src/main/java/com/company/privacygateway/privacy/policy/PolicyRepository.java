package com.company.privacygateway.privacy.policy;

import java.util.Optional;

public interface PolicyRepository {

    Optional<PrivacyPolicy> findActivePolicy(String tenantId);
}
