USE question_bank;
SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('dialogue_session','dialogue_message') ORDER BY table_name;
