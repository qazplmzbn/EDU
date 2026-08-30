SELECT 'blueprint_without_unit' check_name,COUNT(*) problem_count FROM resource_blueprint b LEFT JOIN resource_unit u ON u.id=b.resource_unit_id WHERE u.id IS NULL;
SELECT 'job_without_blueprint' check_name,COUNT(*) problem_count FROM resource_generation_job j LEFT JOIN resource_blueprint b ON b.id=j.blueprint_id WHERE j.status NOT IN('QUEUED','FAILED') AND b.id IS NULL;
