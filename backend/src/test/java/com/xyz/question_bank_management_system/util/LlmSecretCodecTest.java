package com.xyz.question_bank_management_system.util;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class LlmSecretCodecTest {
    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    void encryptsWithEnvironmentKeyAndNeverReturnsPlaintext() {
        LlmSecretCodec codec = new LlmSecretCodec(KEY, "legacy-jwt-secret");
        String cipher = codec.encode("sk-stage06-secret");
        assertTrue(cipher.startsWith("v2:"));
        assertNotEquals("sk-stage06-secret", cipher);
        assertEquals("sk-stage06-secret", codec.decode(cipher));
        assertFalse(codec.mask(cipher).contains("stage06-secret"));
    }

    @Test
    void rejectsSavingWhenEnvironmentKeyIsMissing() {
        LlmSecretCodec codec = new LlmSecretCodec("", "legacy-jwt-secret");
        assertFalse(codec.isConfigured());
        assertThrows(IllegalStateException.class, () -> codec.encode("secret"));
    }
}
