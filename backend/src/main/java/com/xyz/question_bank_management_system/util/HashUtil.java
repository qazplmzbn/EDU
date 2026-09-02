package com.xyz.question_bank_management_system.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class HashUtil {

    private HashUtil() {}

    public static String sha256(String text) {
        return sha256(text.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : dig) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
