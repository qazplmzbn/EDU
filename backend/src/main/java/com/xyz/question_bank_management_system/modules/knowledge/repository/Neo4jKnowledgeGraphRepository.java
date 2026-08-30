package com.xyz.question_bank_management_system.modules.knowledge.repository;

import com.xyz.question_bank_management_system.config.KnowledgeGraphProperties;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.knowledge.entity.KnowledgeGraphVersionRelation;
import com.xyz.question_bank_management_system.modules.knowledge.entity.KnowledgePoint;
import lombok.RequiredArgsConstructor;
import org.neo4j.driver.Driver;
import org.neo4j.driver.SessionConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class Neo4jKnowledgeGraphRepository {
    private final ObjectProvider<Driver> driverProvider;
    private final KnowledgeGraphProperties properties;

    public boolean available() {
        Driver driver = driverProvider.getIfAvailable();
        if (!properties.isEnabled() || driver == null) return false;
        try { driver.verifyConnectivity(); return true; } catch (Exception ex) { return false; }
    }

    public void replaceVersion(Long courseId, String graphVersion, List<KnowledgePoint> points,
                               List<KnowledgeGraphVersionRelation> relations) {
        Driver driver = requiredDriver();
        try (var session = driver.session(SessionConfig.forDatabase(properties.getDatabase()))) {
            session.executeWrite(tx -> {
                tx.run("MATCH (n:KnowledgePoint {courseId:$courseId,graphVersion:$version}) DETACH DELETE n",
                        Map.of("courseId", courseId, "version", graphVersion)).consume();
                List<Map<String,Object>> nodes = points.stream().map(p -> {
                    Map<String,Object> value = new HashMap<>();
                    value.put("id", p.getId()); value.put("code", Objects.toString(p.getCode(), ""));
                    value.put("name", Objects.toString(p.getName(), ""));
                    value.put("type", Objects.toString(p.getKnowledgeType(), "CONCEPT"));
                    value.put("status", Objects.toString(p.getStatus(), "ACTIVE"));
                    return value;
                }).toList();
                tx.run("UNWIND $nodes AS row CREATE (:KnowledgePoint {knowledgePointId:row.id,courseId:$courseId,graphVersion:$version,code:row.code,name:row.name,knowledgeType:row.type,status:row.status})",
                        Map.of("nodes", nodes, "courseId", courseId, "version", graphVersion)).consume();
                for (KnowledgeGraphVersionRelation relation : relations) {
                    if (!Set.of("PREREQUISITE","SIMILAR","PART_OF","RELATED_TO","SUPPORTS").contains(relation.getRelationType())) continue;
                    String cypher = "MATCH (a:KnowledgePoint {courseId:$courseId,graphVersion:$version,knowledgePointId:$source})," +
                            "(b:KnowledgePoint {courseId:$courseId,graphVersion:$version,knowledgePointId:$target}) " +
                            "CREATE (a)-[r:" + relation.getRelationType() +
                            " {courseId:$courseId,graphVersion:$version,relationCode:$code,weight:$weight,confidence:$confidence,sourceType:$sourceType}]->(b)";
                    tx.run(cypher, Map.of("courseId", courseId, "version", graphVersion,
                            "source", relation.getSourceKnowledgePointId(), "target", relation.getTargetKnowledgePointId(),
                            "code", relation.getRelationCode(), "weight", relation.getWeight().doubleValue(),
                            "confidence", relation.getConfidence().doubleValue(), "sourceType", relation.getSourceType())).consume();
                }
                return null;
            });
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.KNOWLEDGE_GRAPH_UNAVAILABLE, "Neo4j 图版本写入失败：" + ex.getMessage());
        }
    }

    public List<Long> prerequisiteClosure(Long courseId, String version, Long targetId) {
        Driver driver = requiredDriver();
        try (var session = driver.session(SessionConfig.forDatabase(properties.getDatabase()))) {
            return session.executeRead(tx -> tx.run("MATCH (p:KnowledgePoint {courseId:$courseId,graphVersion:$version})-[:PREREQUISITE*0..16]->(t:KnowledgePoint {courseId:$courseId,graphVersion:$version,knowledgePointId:$target}) RETURN DISTINCT p.knowledgePointId AS id",
                    Map.of("courseId",courseId,"version",version,"target",targetId)).list(r -> r.get("id").asLong()));
        }
    }

    public boolean hasAllowedRelation(Long courseId, String version, Long source, Long target) {
        Driver driver = requiredDriver();
        try (var session = driver.session(SessionConfig.forDatabase(properties.getDatabase()))) {
            return session.executeRead(tx -> tx.run("MATCH (a:KnowledgePoint {courseId:$courseId,graphVersion:$version,knowledgePointId:$source})-[r:PREREQUISITE|PART_OF|RELATED_TO|SUPPORTS]-(b:KnowledgePoint {courseId:$courseId,graphVersion:$version,knowledgePointId:$target}) RETURN count(r)>0 AS found",
                    Map.of("courseId",courseId,"version",version,"source",source,"target",target)).single().get("found").asBoolean());
        }
    }

    public GraphCounts countVersion(Long courseId, String version) {
        Driver driver = requiredDriver();
        try (var session = driver.session(SessionConfig.forDatabase(properties.getDatabase()))) {
            return session.executeRead(tx -> {
                long nodes = tx.run("MATCH (n:KnowledgePoint {courseId:$courseId,graphVersion:$version}) RETURN count(n) AS count",
                        Map.of("courseId",courseId,"version",version)).single().get("count").asLong();
                long edges = tx.run("MATCH (:KnowledgePoint {courseId:$courseId,graphVersion:$version})-[r]->(:KnowledgePoint {courseId:$courseId,graphVersion:$version}) RETURN count(r) AS count",
                        Map.of("courseId",courseId,"version",version)).single().get("count").asLong();
                return new GraphCounts(Math.toIntExact(nodes),Math.toIntExact(edges));
            });
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.KNOWLEDGE_GRAPH_UNAVAILABLE,"Neo4j 图版本回读失败："+ex.getMessage());
        }
    }

    public record GraphCounts(int nodeCount,int edgeCount) {}

    private Driver requiredDriver() {
        Driver driver = driverProvider.getIfAvailable();
        if (!properties.isEnabled() || driver == null) {
            throw BizException.of(ErrorCode.KNOWLEDGE_GRAPH_UNAVAILABLE, "知识图谱服务不可用");
        }
        return driver;
    }
}
