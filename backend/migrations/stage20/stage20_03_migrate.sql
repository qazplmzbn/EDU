-- Existing student skill rows become compatible with the new aggregation metadata.
UPDATE student_skill_state
SET core_proficiency_value = proficiency_value,
    knowledge_coverage_rate = CASE WHEN evidence_count > 0 THEN 1.0000 ELSE 0.0000 END,
    calculation_version = COALESCE(calculation_version, 'career_skill_aggregation_v1'),
    calculated_at = COALESCE(calculated_at, updated_at)
WHERE calculation_version IS NULL OR calculated_at IS NULL;
