# Neo4j knowledge graph

Use Neo4j 5.x, execute `constraints.cypher`, then configure `NEO4J_ENABLED=true`, `NEO4J_URI`, `NEO4J_USERNAME`, `NEO4J_PASSWORD`, and `NEO4J_DATABASE`. MySQL owns version state; Neo4j stores only versioned published graph data and traversal indexes.

## Docker rehearsal

```powershell
docker run -d --name edu-neo4j-rehearsal `
  -p 7474:7474 -p 7687:7687 `
  -v edu-neo4j-rehearsal-data:/data `
  -e "NEO4J_AUTH=neo4j/$env:NEO4J_PASSWORD" `
  neo4j:5.26-community

docker cp constraints.cypher edu-neo4j-rehearsal:/tmp/constraints.cypher
docker exec edu-neo4j-rehearsal cypher-shell `
  -u neo4j -p $env:NEO4J_PASSWORD -d neo4j `
  -f /tmp/constraints.cypher
```

The Stage 19 acceptance count for the first C-language graph is 51 `KnowledgePoint` nodes, 26 `PREREQUISITE` relationships, and 17 `SIMILAR` relationships. Keep the rehearsal volume separate from production data.
