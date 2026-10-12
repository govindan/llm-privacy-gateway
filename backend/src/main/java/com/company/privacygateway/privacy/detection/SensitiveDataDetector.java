package com.company.privacygateway.privacy.detection;

import com.company.privacygateway.privacy.policy.SensitiveEntity;

import java.util.List;

public interface SensitiveDataDetector {

    List<SensitiveEntity> detect(String text);
}
