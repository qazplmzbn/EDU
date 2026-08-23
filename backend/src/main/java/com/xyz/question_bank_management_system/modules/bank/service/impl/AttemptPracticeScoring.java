package com.xyz.question_bank_management_system.modules.bank.service.impl;

import com.xyz.question_bank_management_system.common.enums.QuestionTypeEnum;
import com.xyz.question_bank_management_system.modules.bank.entity.QbQuestion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

final class AttemptPracticeScoring {

    static final int PRACTICE_DEFAULT_SCORE = 10;
    static final int PRACTICE_PROGRAMMING_SCORE = 20;

    private AttemptPracticeScoring() {
    }

    static int estimatePracticeQuestionLimit(int totalScore) {
        return Math.max(1, Math.min(50, (int) Math.ceil(totalScore / (double) PRACTICE_DEFAULT_SCORE)));
    }

    static int[] buildPracticeScores(List<QbQuestion> selected) {
        int[] scores = new int[selected.size()];
        for (int i = 0; i < selected.size(); i++) {
            scores[i] = practiceScoreForQuestion(selected.get(i));
        }
        return scores;
    }

    static List<QbQuestion> pickPracticeQuestionsByScore(List<QbQuestion> orderedCandidates, int targetTotalScore) {
        if (orderedCandidates == null || orderedCandidates.isEmpty()) {
            return List.of();
        }

        int size = orderedCandidates.size();
        int[] suffixDefaultCount = new int[size + 1];
        int[] suffixProgrammingCount = new int[size + 1];
        for (int i = size - 1; i >= 0; i--) {
            suffixDefaultCount[i] = suffixDefaultCount[i + 1];
            suffixProgrammingCount[i] = suffixProgrammingCount[i + 1];
            if (isProgrammingQuestionType(orderedCandidates.get(i).getQuestionType())) {
                suffixProgrammingCount[i]++;
            } else {
                suffixDefaultCount[i]++;
            }
        }

        List<QbQuestion> selected = new ArrayList<>();
        Set<Long> selectedIds = new HashSet<>();
        int remaining = targetTotalScore;

        for (int i = 0; i < size && remaining > 0; i++) {
            QbQuestion question = orderedCandidates.get(i);
            int score = practiceScoreForQuestion(question);
            if (score > remaining) {
                continue;
            }
            if (!canAssemblePracticeScore(remaining - score, suffixDefaultCount[i + 1], suffixProgrammingCount[i + 1])) {
                continue;
            }
            selected.add(question);
            if (question.getId() != null) {
                selectedIds.add(question.getId());
            }
            remaining -= score;
        }

        if (remaining > 0) {
            for (QbQuestion question : orderedCandidates) {
                if (remaining <= 0 || selectedIds.contains(question.getId())) {
                    continue;
                }
                int score = practiceScoreForQuestion(question);
                if (score > remaining) {
                    continue;
                }
                selected.add(question);
                if (question.getId() != null) {
                    selectedIds.add(question.getId());
                }
                remaining -= score;
            }
        }

        if (remaining > 0) {
            for (QbQuestion question : orderedCandidates) {
                if (remaining <= 0 || selectedIds.contains(question.getId())) {
                    continue;
                }
                selected.add(question);
                if (question.getId() != null) {
                    selectedIds.add(question.getId());
                }
                remaining -= practiceScoreForQuestion(question);
            }
        }
        return selected;
    }

    static boolean canAssemblePracticeScore(int remainingScore, int defaultQuestionCount, int programmingQuestionCount) {
        if (remainingScore < 0) {
            return false;
        }
        if (remainingScore == 0) {
            return true;
        }
        if (remainingScore % PRACTICE_DEFAULT_SCORE != 0) {
            return false;
        }
        int maxProgrammingUsed = Math.min(programmingQuestionCount, remainingScore / PRACTICE_PROGRAMMING_SCORE);
        for (int programmingUsed = maxProgrammingUsed; programmingUsed >= 0; programmingUsed--) {
            int leftover = remainingScore - programmingUsed * PRACTICE_PROGRAMMING_SCORE;
            if (leftover < 0) {
                continue;
            }
            int defaultNeeded = leftover / PRACTICE_DEFAULT_SCORE;
            if (defaultNeeded <= defaultQuestionCount) {
                return true;
            }
        }
        return false;
    }

    static int practiceScoreForQuestion(QbQuestion question) {
        return isProgrammingQuestionType(question == null ? null : question.getQuestionType())
                ? PRACTICE_PROGRAMMING_SCORE
                : PRACTICE_DEFAULT_SCORE;
    }

    static boolean isProgrammingQuestionType(Integer questionType) {
        return Objects.equals(questionType, QuestionTypeEnum.CODE.getCode());
    }

    static boolean isObjective(Integer questionType) {
        if (questionType == null) return false;
        return questionType == QuestionTypeEnum.SINGLE.getCode()
                || questionType == QuestionTypeEnum.MULTIPLE.getCode()
                || questionType == QuestionTypeEnum.TRUE_FALSE.getCode()
                || questionType == QuestionTypeEnum.BLANK.getCode();
    }
}
