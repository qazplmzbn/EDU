package com.xyz.question_bank_management_system.modules.knowledge.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.question_bank_management_system.modules.knowledge.model.NormalizedCourseGraph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CourseGraphParserValidatorTest {
    private static final Path GRAPH_DIR = Path.of("D:/qq/downloadfile/课程图谱");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CourseGraphJsonParser parser = new CourseGraphJsonParser(objectMapper);
    private final CourseGraphNormalizer normalizer = new CourseGraphNormalizer(objectMapper);
    private final CourseGraphValidator validator = new CourseGraphValidator(normalizer);

    @ParameterizedTest
    @ValueSource(strings={"C语言_知识图谱.json","计算机网络_知识图谱.json","面向对象编程_知识图谱.json","前端开发框架_知识图谱.json","数据库_知识图谱.json"})
    void parsesAndValidatesAllDeliveredGraphs(String fileName) throws Exception {
        Path path=GRAPH_DIR.resolve(fileName);
        Assumptions.assumeTrue(Files.exists(path),"当前环境未挂载交付课程图谱："+path);
        NormalizedCourseGraph graph=normalizer.normalize(parser.parse(Files.readAllBytes(path),fileName).document());
        assertTrue(validator.validate(graph).isEmpty(),"结构校验失败："+fileName+" -> "+validator.validate(graph));
        assertEquals(64,graph.normalizedHash().length());
    }

    @Test
    void cGraphRetainsImportableStructureAfterApprovedAugmentation() throws Exception {
        Path path=GRAPH_DIR.resolve("C语言_知识图谱.json");
        Assumptions.assumeTrue(Files.exists(path),"当前环境未挂载 C语言交付图谱："+path);
        NormalizedCourseGraph graph=normalizer.normalize(parser.parse(Files.readAllBytes(path),path.getFileName().toString()).document());
        long courses=graph.nodes().stream().filter(x->"Course".equals(x.type())).count();
        long modules=graph.nodes().stream().filter(x->"Module".equals(x.type())).count();
        long categories=graph.nodes().stream().filter(x->"Category".equals(x.type())).count();
        long points=graph.nodes().stream().filter(x->"KnowledgePoint".equals(x.type())).count();
        long contains=graph.edges().stream().filter(x->"CONTAINS".equals(x.relation())).count();
        long prerequisites=graph.edges().stream().filter(x->"PREREQUISITE".equals(x.relation())).count();
        long similar=graph.edges().stream().filter(x->"SIMILAR".equals(x.relation())).count();
        assertAll(()->assertEquals(1,courses),()->assertTrue(modules>=1),()->assertTrue(categories>=0),
                ()->assertTrue(points>=1),()->assertTrue(contains>=modules+points),
                ()->assertTrue(prerequisites>=0),()->assertTrue(similar>=0));
    }

    @Test
    void detectsBrokenParentAndPrerequisiteCycle() {
        String json="""
                {"meta":{"course":"测试","course_id":"T","schema_version":"3.0"},
                 "nodes":[{"id":"T","name":"测试","type":"Course","level":0,"parent":null},
                          {"id":"T.1","name":"模块","type":"Module","level":1,"parent":"T"},
                          {"id":"T.1.1","name":"A","type":"KnowledgePoint","level":2,"parent":"missing"}],
                 "edges":[{"source":"T","target":"T.1","relation":"包含"},
                          {"source":"T.1.1","target":"T.1.1","relation":"先修"}]}
                """;
        NormalizedCourseGraph graph=normalizer.normalize(parser.parse(json.getBytes(java.nio.charset.StandardCharsets.UTF_8),"broken.json").document());
        var codes=validator.validate(graph).stream().map(x->x.getIssueCode()).toList();
        assertTrue(codes.contains("PARENT_NOT_FOUND"));
        assertTrue(codes.contains("SELF_LOOP"));
        assertTrue(codes.contains("PREREQUISITE_CYCLE"));
    }

    @Test
    void structureHashIgnoresResourcesButChangesWithNodeContent() {
        String base="""
                {"meta":{"course":"测试","course_id":"T","schema_version":"3.0"},
                 "nodes":[{"id":"T","name":"测试","type":"Course","level":0,"parent":null,"resources":[{"modality":"文字解释","content":"%s"}]},
                          {"id":"T.1","name":"模块","type":"Module","level":1,"parent":"T"}],
                 "edges":[{"source":"T","target":"T.1","relation":"包含"}]}
                """;
        var first=normalizer.normalize(parser.parse(base.formatted("资源A").getBytes(java.nio.charset.StandardCharsets.UTF_8),"a.json").document());
        var resourceChanged=normalizer.normalize(parser.parse(base.formatted("资源B").getBytes(java.nio.charset.StandardCharsets.UTF_8),"b.json").document());
        String nodeChanged=base.formatted("资源A").replace("\"name\":\"模块\"","\"name\":\"模块二\"");
        var structureChanged=normalizer.normalize(parser.parse(nodeChanged.getBytes(java.nio.charset.StandardCharsets.UTF_8),"c.json").document());
        assertEquals(first.normalizedHash(),resourceChanged.normalizedHash());
        assertNotEquals(first.normalizedHash(),structureChanged.normalizedHash());
    }
}
