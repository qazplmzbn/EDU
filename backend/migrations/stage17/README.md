# Stage 17 - Course graph import intake and validation

Target: MySQL 8.x after Stage 16. Execute `precheck -> schema -> migrate -> verify` manually. Never execute cleanup in this delivery.

This stage adds immutable import intake and issue records. It does not import course-domain rows. The validate API is read-only; rejected commits may persist only import and issue audit rows.

Back up the rehearsal database before schema execution. Rollback is limited to restoring that backup because audit rows are intentionally retained.
