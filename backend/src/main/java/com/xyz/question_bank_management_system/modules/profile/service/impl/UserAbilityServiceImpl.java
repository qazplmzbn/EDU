package com.xyz.question_bank_management_system.modules.profile.service.impl;

import com.xyz.question_bank_management_system.modules.profile.entity.QbAbilitySample;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAnswerMapper;
import com.xyz.question_bank_management_system.modules.profile.mapper.QbUserAbilityMapper;
import com.xyz.question_bank_management_system.modules.profile.service.UserAbilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserAbilityServiceImpl implements UserAbilityService {

    private static final int MIN_ABILITY = 0;
    private static final int MAX_ABILITY = 100;
    private static final int MIN_DIFFICULTY = 1;
    private static final int MAX_DIFFICULTY = 5;

    // Rasch online update (Elo-IRT style) hyper-parameters.
    private static final double BASE_THETA_K = 0.55;
    private static final double BASE_ITEM_K = 0.12;
    private static final double DIFFICULTY_STEP = 0.70;
    private static final double THETA_TO_SCORE_SCALE = 1.00;

    private final QbAnswerMapper answerMapper;
    private final QbUserAbilityMapper userAbilityMapper;

    @Override
    public void recomputeAndPersist(Long userId) {
        if (userId == null) {
            return;
        }

        List<QbAbilitySample> samples = answerMapper.selectAbilitySamplesByUserId(userId);
        if (samples == null || samples.isEmpty()) {
            userAbilityMapper.upsert(userId, 0);
            return;
        }

        double theta = 0.0;
        int seenCount = 0;
        Map<Long, Double> itemBetaMap = new HashMap<>();
        Map<Long, Integer> itemSeenMap = new HashMap<>();

        for (QbAbilitySample sample : samples) {
            if (sample == null || sample.getQuestionId() == null) {
                continue;
            }

            int maxScore = safeNonNegative(sample.getMaxScore());
            if (maxScore <= 0) {
                continue;
            }

            int finalScore = clamp(safeNonNegative(sample.getFinalScore()), 0, maxScore);
            double y = (double) finalScore / maxScore;

            Long questionId = sample.getQuestionId();
            double beta = itemBetaMap.getOrDefault(questionId, initialBetaByDifficulty(sample.getDifficulty()));
            int itemSeen = itemSeenMap.getOrDefault(questionId, 0);

            double p = sigmoid(theta - beta);
            double residual = y - p;

            double thetaK = BASE_THETA_K / Math.sqrt(seenCount + 1.0);
            double itemK = BASE_ITEM_K / Math.sqrt(itemSeen + 1.0);

            theta += thetaK * residual;
            beta -= itemK * residual;

            itemBetaMap.put(questionId, beta);
            itemSeenMap.put(questionId, itemSeen + 1);
            seenCount++;
        }

        int abilityScore = seenCount == 0 ? 0 : mapThetaToAbility(theta);
        userAbilityMapper.upsert(userId, abilityScore);
    }

    private double initialBetaByDifficulty(Integer difficulty) {
        int d = clamp(safeDifficulty(difficulty), MIN_DIFFICULTY, MAX_DIFFICULTY);
        return (d - 3) * DIFFICULTY_STEP;
    }

    private int safeDifficulty(Integer difficulty) {
        return difficulty == null ? 3 : difficulty;
    }

    private int safeNonNegative(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private int mapThetaToAbility(double theta) {
        double normalized = sigmoid(theta / THETA_TO_SCORE_SCALE);
        int score = (int) Math.round(normalized * MAX_ABILITY);
        return clamp(score, MIN_ABILITY, MAX_ABILITY);
    }

    private double sigmoid(double x) {
        if (x >= 0) {
            double z = Math.exp(-x);
            return 1.0 / (1.0 + z);
        }
        double z = Math.exp(x);
        return z / (1.0 + z);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
