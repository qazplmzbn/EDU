package com.xyz.question_bank_management_system.config;

public final class CorrelationContext {
    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private CorrelationContext() {}

    public static String get() { return HOLDER.get(); }
    static void set(String value) { HOLDER.set(value); }
    static void clear() { HOLDER.remove(); }
}
