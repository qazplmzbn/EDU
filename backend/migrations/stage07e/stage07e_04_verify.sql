USE question_bank;

SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'qb_learning_resource_target';

SELECT column_name, column_type, is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'qb_learning_resource_target'
ORDER BY ordinal_position;

SELECT constraint_name, constraint_type
FROM information_schema.table_constraints
WHERE table_schema = DATABASE()
  AND table_name = 'qb_learning_resource_target'
ORDER BY constraint_type, constraint_name;

SELECT COUNT(*) AS invalid_target_count
FROM qb_learning_resource_target
WHERE (target_type = 'student' AND (student_id IS NULL OR class_id IS NOT NULL))
   OR (target_type = 'class' AND (class_id IS NULL OR student_id IS NOT NULL))
   OR target_type NOT IN ('student', 'class');
