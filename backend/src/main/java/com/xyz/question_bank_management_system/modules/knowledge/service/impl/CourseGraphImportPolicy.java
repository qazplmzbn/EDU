package com.xyz.question_bank_management_system.modules.knowledge.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** Import admission policy. Course-specific data belongs in configuration, not in the importer flow. */
@Component
class CourseGraphImportPolicy {
    private final String legacyBridgeCourseCode;

    CourseGraphImportPolicy(
            @Value("${app.course-graph-import.legacy-bridge-course-code:C}") String legacyBridgeCourseCode) {
        this.legacyBridgeCourseCode = legacyBridgeCourseCode.trim().toUpperCase(Locale.ROOT);
    }

    boolean usesLegacyBridge(String courseCode) {
        return legacyBridgeCourseCode.equalsIgnoreCase(courseCode);
    }
}
