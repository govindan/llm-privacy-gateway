package com.company.privacygateway.vault;

import java.security.SecureRandom;
import java.util.HexFormat;

final class OpaqueTokenIds {

    private static final int ID_BYTES = 8;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private OpaqueTokenIds() {
    }

    static String randomHexId() {
        byte[] bytes = new byte[ID_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().withUpperCase().formatHex(bytes);
    }

    static String formatToken(String entityType, String hexId) {
        return "<<" + entityType + ":" + hexId + ">>";
    }
}
