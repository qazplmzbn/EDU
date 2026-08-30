# Stage 08 — Course knowledge graph foundation

Target: MySQL 8 and Neo4j 5.x. Run `precheck → schema → migrate → verify`; configure Neo4j and run `backend/deploy/neo4j/constraints.cypher` before publishing a graph. Back up `knowledge_point`, `knowledge_relation`, and `course_knowledge`. Rollback is additive until cleanup; cleanup remains blocked until Stage 16.
