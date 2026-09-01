SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('file_asset','student_evidence','student_resume_document','student_resume_evidence');
