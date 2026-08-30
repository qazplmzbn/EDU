# Stage 19 - Graph publication gate

Target: MySQL 8.x and Neo4j 5.x after Stage 18 approval. Execute MySQL precheck/schema/migrate/verify, then apply `deploy/neo4j/constraints.cypher` before graph validation.

The stage adds release indexes and a checkpoint. It never removes old knowledge points or historical references. Cleanup is blocked.
