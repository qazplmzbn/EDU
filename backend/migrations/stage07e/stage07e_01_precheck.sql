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
