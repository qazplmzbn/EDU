# Stage 18 - Transactional course graph import and review

Target: MySQL 8.x after Stage 17. Execute `precheck -> schema -> migrate -> verify`. Cleanup is blocked.

This stage adds legacy mappings and review linkage. Course graph data is written only by the authenticated import API after validation-hash verification.
