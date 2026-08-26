package com.xyz.question_bank_management_system.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class LlmSecretCodec {
    private static final String PREFIX = "v2:";
    private static final String LEGACY_PREFIX = "v1:";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecureRandom secureRandom = new SecureRandom();
    private final String configuredKey;
    private final String legacyJwtSecret;

    /**
     * API keys must never be derived from the JWT signing secret.  The value is
     * intentionally read from an environment-backed property so a deployment
     * cannot silently use the development JWT default as an encryption key.
     */
    public LlmSecretCodec(@Value("${app.llm.encryption-key:${APP_LLM_ENCRYPTION_KEY:}}") String configuredKey,
                          @Value("${app.jwt.secret:}") String legacyJwtSecret) {
        this.configuredKey = configuredKey;
        this.legacyJwtSecret = legacyJwtSecret;
    }

    public String encode(String plainText) {
        if (!StringUtils.hasText(plainText)) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.trim().getBytes(StandardCharsets.UTF_8));
            return PREFIX + Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt LLM secret", ex);
        }
    }

    public String decode(String cipherText) {
        if (!StringUtils.hasText(cipherText)) {
            return "";
        }
        String value = cipherText.trim();
        if (!value.startsWith(PREFIX) && !value.startsWith(LEGACY_PREFIX)) {
            return value;
        }
        if (!isConfigured()) {
            throw new IllegalStateException("APP_LLM_ENCRYPTION_KEY must be configured before encrypted model keys can be used");
        }
        try {
            boolean legacy = value.startsWith(LEGACY_PREFIX);
            String[] parts = value.substring(legacy ? LEGACY_PREFIX.length() : PREFIX.length()).split(":", 2);
            if (parts.length != 2) {
                return "";
            }
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] encrypted = Base64.getDecoder().decode(parts[1]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, legacy ? legacyKeySpec() : keySpec(), new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return "";
        }
    }

    public String mask(String cipherText) {
        if (StringUtils.hasText(cipherText) && !isConfigured()) {
            return "configured";
        }
        String secret = decode(cipherText);
        if (!StringUtils.hasText(secret)) {
            return "";
        }
        String trimmed = secret.trim();
        if (trimmed.length() <= 8) {
            return "****" + trimmed.substring(Math.max(0, trimmed.length() - 2));
        }
        return trimmed.substring(0, 4) + "****" + trimmed.substring(trimmed.length() - 4);
    }

    public boolean isConfigured() {
        try {
            Base64.getDecoder().decode(String.valueOf(configuredKey).trim());
            return StringUtils.hasText(configuredKey)
                    && Base64.getDecoder().decode(configuredKey.trim()).length == 32;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private SecretKeySpec keySpec() {
        if (!isConfigured()) {
            throw new IllegalStateException("APP_LLM_ENCRYPTION_KEY must be a Base64 encoded 32-byte key");
        }
        return new SecretKeySpec(Base64.getDecoder().decode(configuredKey.trim()), "AES");
    }

    private SecretKeySpec legacyKeySpec() {
        if (!StringUtils.hasText(legacyJwtSecret)) {
            throw new IllegalStateException("Legacy LLM secret cannot be decrypted without the previous JWT secret");
        }
        try {
            byte[] key = MessageDigest.getInstance("SHA-256").digest(legacyJwtSecret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(key, "AES");
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load legacy LLM secret", ex);
        }
    }
}
