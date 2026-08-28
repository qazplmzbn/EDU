package com.xyz.question_bank_management_system.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan({
        "com.xyz.question_bank_management_system.modules.user.mapper",
        "com.xyz.question_bank_management_system.modules.org.mapper",
        "com.xyz.question_bank_management_system.modules.bank.mapper",
        "com.xyz.question_bank_management_system.modules.course.mapper",
        "com.xyz.question_bank_management_system.modules.dialogue.mapper",
        "com.xyz.question_bank_management_system.modules.competency.mapper",
        "com.xyz.question_bank_management_system.modules.knowledge.mapper",
        "com.xyz.question_bank_management_system.modules.profile.mapper",
        "com.xyz.question_bank_management_system.modules.recommendation.mapper",
        "com.xyz.question_bank_management_system.modules.source.mapper",
        "com.xyz.question_bank_management_system.modules.learning.mapper",
        "com.xyz.question_bank_management_system.modules.agent.mapper",
        "com.xyz.question_bank_management_system.modules.llm.mapper"
})
public class MybatisConfig {
}
