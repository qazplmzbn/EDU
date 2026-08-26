package com.xyz.question_bank_management_system.modules.llm.service;

public interface ModelKeyRotationService {
    /** Explicit administrator action; never invoked automatically at startup. */
    int reencryptLegacyKeys();
}
