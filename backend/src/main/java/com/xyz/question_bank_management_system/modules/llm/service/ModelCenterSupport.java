package com.xyz.question_bank_management_system.modules.llm.service;

import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Locale;

final class ModelCenterSupport {

    private ModelCenterSupport() {
    }

    static long defaultUserId(Long userId) {
        return userId == null ? 0L : userId;
    }

    static Long number(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    static int numberInt(Object value, int fallback) {
        if (value == null) return fallback;
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return fallback;
        }
    }

    static double numberDouble(Object value, double fallback) {
        if (value == null) return fallback;
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ex) {
            return fallback;
        }
    }

    static boolean truthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean bool) return bool;
        if (value instanceof Number number) return number.intValue() != 0;
        String text = String.valueOf(value).trim();
        return "true".equalsIgnoreCase(text) || "1".equals(text) || "active".equalsIgnoreCase(text);
    }

    static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    static String textOrNull(Object value) {
        String text = text(value);
        return StringUtils.hasText(text) ? text : null;
    }

    static String textOrDefault(Object value, String fallback) {
        String text = textOrNull(value);
        return text == null ? fallback : text;
    }

    static String lower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    static String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) return value;
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    static String normalizeAuthorization(String apiKey) {
        String normalized = apiKey.trim();
        if (normalized.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return normalized;
        }
        return "Bearer " + normalized;
    }

    static String iso(Object value) {
        if (value == null) return null;
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toString();
        }
        if (value instanceof LocalDateTime time) {
            return time.toString();
        }
        return String.valueOf(value);
    }

    static Timestamp timestamp(Object value) {
        if (value == null) return null;
        if (value instanceof Timestamp timestamp) return timestamp;
        if (value instanceof LocalDateTime time) return Timestamp.valueOf(time);
        return Timestamp.valueOf(String.valueOf(value).replace("T", " "));
    }

    static Timestamp timestampOrNow(Object value) {
        Timestamp timestamp = timestamp(value);
        return timestamp == null ? Timestamp.valueOf(LocalDateTime.now()) : timestamp;
    }
}
