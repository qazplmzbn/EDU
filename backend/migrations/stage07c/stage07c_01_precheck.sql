USE question_bank;
SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('student_resource_recommendation','resource_feedback') ORDER BY table_name;
SELECT COUNT(*) active_resources FROM qb_learning_resource WHERE is_deleted=0;
