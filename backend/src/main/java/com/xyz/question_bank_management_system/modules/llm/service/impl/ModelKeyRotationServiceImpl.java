package com.xyz.question_bank_management_system.modules.llm.service.impl;

import com.xyz.question_bank_management_system.modules.llm.mapper.ModelConfigMapper;
import com.xyz.question_bank_management_system.modules.llm.service.ModelKeyRotationService;
import com.xyz.question_bank_management_system.util.LlmSecretCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ModelKeyRotationServiceImpl implements ModelKeyRotationService {
    private final ModelConfigMapper modelConfigMapper;
    private final LlmSecretCodec secretCodec;

    @Override
    @Transactional
    public int reencryptLegacyKeys() {
        if (!secretCodec.isConfigured()) {
            throw new IllegalStateException("APP_LLM_ENCRYPTION_KEY must be configured before key rotation");
        }
        int count = 0;
        for (var row : modelConfigMapper.selectLegacyEncryptedKeys()) {
            String plain = secretCodec.decode(row.getApiKeyCipher());
            if (plain == null || plain.isBlank()) {
                throw new IllegalStateException("A legacy encrypted model key cannot be decrypted with the configured legacy JWT secret");
            }
            count += modelConfigMapper.updateCipher(row.getId(), secretCodec.encode(plain));
        }
        return count;
    }
}
