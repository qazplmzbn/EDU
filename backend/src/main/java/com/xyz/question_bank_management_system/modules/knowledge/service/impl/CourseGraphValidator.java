package com.xyz.question_bank_management_system.modules.knowledge.service.impl;

import com.xyz.question_bank_management_system.modules.knowledge.dto.CourseGraphDocument;
import com.xyz.question_bank_management_system.modules.knowledge.model.NormalizedCourseGraph;
import com.xyz.question_bank_management_system.modules.knowledge.vo.CourseGraphValidationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CourseGraphValidator {
    private static final Set<String> NODE_TYPES = Set.of("Course", "Module", "Category", "KnowledgePoint");
    private static final Set<String> RELATIONS = Set.of("CONTAINS", "PREREQUISITE", "SIMILAR");
    private final CourseGraphNormalizer normalizer;

    public List<CourseGraphValidationVO.Issue> validate(NormalizedCourseGraph graph) {
        List<CourseGraphValidationVO.Issue> issues = new ArrayList<>();
        CourseGraphDocument document = graph.source();
        CourseGraphDocument.Meta meta = document.getMeta();
        if (meta == null) {
            error(issues, "META_MISSING", "META", null, "meta 不能为空");
        } else {
            if (!"3.0".equals(trim(meta.getSchemaVersion()))) error(issues, "SCHEMA_VERSION_UNSUPPORTED", "META", "schema_version", "schema_version 必须为 3.0");
        }

        Map<String, CourseGraphDocument.Node> unique = new HashMap<>();
        List<CourseGraphDocument.Node> courseRoots = new ArrayList<>();
        for (CourseGraphDocument.Node node : document.getNodes()) {
            String id = trim(node.getId());
            if (id.isEmpty()) {
                error(issues, "NODE_ID_MISSING", "NODE", null, "节点 id 不能为空");
                continue;
            }
            if (unique.putIfAbsent(id, node) != null) error(issues, "DUPLICATE_NODE_ID", "NODE", id, "节点 id 重复");
            if (!NODE_TYPES.contains(trim(node.getType()))) error(issues, "NODE_TYPE_UNSUPPORTED", "NODE", id, "不支持的节点类型：" + node.getType());
            if ("Course".equals(node.getType())) courseRoots.add(node);
        }
        if (courseRoots.size() != 1) {
            error(issues, "COURSE_ROOT_COUNT_INVALID", "META", null, "必须且只能有一个 Course 根节点");
        } else if (meta != null) {
            CourseGraphDocument.Node root = courseRoots.get(0);
            if (!Objects.equals(trim(meta.getCourseId()), trim(root.getId())) || !Objects.equals(trim(meta.getCourse()), trim(root.getName()))) {
                error(issues, "META_COURSE_MISMATCH", "META", trim(root.getId()), "meta.course/course_id 与 Course 根节点不一致");
            }
        }

        Map<String, Integer> containsIncoming = new HashMap<>();
        Set<String> edgeKeys = new HashSet<>();
        Map<String, List<String>> prerequisites = new HashMap<>();
        for (NormalizedCourseGraph.NormalizedEdge edge : graph.edges()) {
            if (!RELATIONS.contains(edge.relation())) error(issues, "RELATION_TYPE_UNSUPPORTED", "EDGE", edge.source()+">"+edge.target(), "不支持的关系类型：" + edge.relation());
            if (!unique.containsKey(edge.source()) || !unique.containsKey(edge.target())) error(issues, "EDGE_ENDPOINT_NOT_FOUND", "EDGE", edge.source()+">"+edge.target(), "关系端点不存在");
            if (edge.source().equals(edge.target())) error(issues, "SELF_LOOP", "EDGE", edge.source(), "不允许自环");
            String key = edge.relation()+":"+edge.source()+":"+edge.target();
            if (!edgeKeys.add(key)) error(issues, "DUPLICATE_EDGE", "EDGE", key, "关系重复");
            if ("CONTAINS".equals(edge.relation())) containsIncoming.merge(edge.target(), 1, Integer::sum);
            if ("PREREQUISITE".equals(edge.relation())) prerequisites.computeIfAbsent(edge.source(), ignored -> new ArrayList<>()).add(edge.target());
        }

        for (CourseGraphDocument.Node node : document.getNodes()) {
            String id = trim(node.getId());
            if ("Course".equals(node.getType())) continue;
            String parent = trim(node.getParent());
            if (parent.isEmpty() || !unique.containsKey(parent)) {
                error(issues, "PARENT_NOT_FOUND", "NODE", id, "非根节点 parent 必须存在");
                continue;
            }
            String containsKey = "CONTAINS:" + parent + ":" + id;
            if (containsIncoming.getOrDefault(id, 0) != 1 || !edgeKeys.contains(containsKey)) {
                error(issues, "CONTAINS_PARENT_MISMATCH", "NODE", id, "节点必须恰好有一条与 parent 一致的包含边");
            }
        }
        if (hasCycle(prerequisites)) error(issues, "PREREQUISITE_CYCLE", "EDGE", null, "先修关系必须为 DAG");

        return issues;
    }

    private boolean hasCycle(Map<String, List<String>> edges) {
        Set<String> visiting = new HashSet<>();
        Set<String> done = new HashSet<>();
        for (String node : edges.keySet()) if (cycle(node, edges, visiting, done)) return true;
        return false;
    }

    private boolean cycle(String node, Map<String, List<String>> edges, Set<String> visiting, Set<String> done) {
        if (visiting.contains(node)) return true;
        if (done.contains(node)) return false;
        visiting.add(node);
        for (String next : edges.getOrDefault(node, List.of())) if (cycle(next, edges, visiting, done)) return true;
        visiting.remove(node);
        done.add(node);
        return false;
    }

    private void error(List<CourseGraphValidationVO.Issue> out, String code, String type, String location, String message) {
        out.add(new CourseGraphValidationVO.Issue("ERROR", code, type, location, message));
    }

    private String trim(String value) { return value == null ? "" : value.trim(); }
}
