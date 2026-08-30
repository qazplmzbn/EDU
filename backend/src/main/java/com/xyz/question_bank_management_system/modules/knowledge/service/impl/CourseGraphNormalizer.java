package com.xyz.question_bank_management_system.modules.knowledge.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.question_bank_management_system.modules.knowledge.dto.CourseGraphDocument;
import com.xyz.question_bank_management_system.modules.knowledge.model.NormalizedCourseGraph;
import com.xyz.question_bank_management_system.util.HashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CourseGraphNormalizer {
    private final ObjectMapper objectMapper;

    public NormalizedCourseGraph normalize(CourseGraphDocument document) {
        Map<String, CourseGraphDocument.Node> byId = new LinkedHashMap<>();
        List<NormalizedCourseGraph.NormalizedNode> nodes = new ArrayList<>();
        for (CourseGraphDocument.Node node : document.getNodes()) {
            String id = text(node.getId());
            if (!id.isEmpty()) byId.putIfAbsent(id, node);
            nodes.add(new NormalizedCourseGraph.NormalizedNode(
                    id, text(node.getName()), text(node.getType()), node.getLevel(), nullable(node.getParent())));
        }
        nodes.sort(Comparator.comparing(NormalizedCourseGraph.NormalizedNode::id));

        List<NormalizedCourseGraph.NormalizedEdge> edges = new ArrayList<>();
        for (CourseGraphDocument.Edge edge : document.getEdges()) {
            String relation = relation(edge.getRelation());
            String source = text(edge.getSource());
            String target = text(edge.getTarget());
            if ("SIMILAR".equals(relation) && source.compareTo(target) > 0) {
                String swap = source; source = target; target = swap;
            }
            edges.add(new NormalizedCourseGraph.NormalizedEdge(source, target, relation));
        }
        edges.sort(Comparator.comparing(NormalizedCourseGraph.NormalizedEdge::relation)
                .thenComparing(NormalizedCourseGraph.NormalizedEdge::source)
                .thenComparing(NormalizedCourseGraph.NormalizedEdge::target));

        try {
            Map<String, Object> canonical = new LinkedHashMap<>();
            CourseGraphDocument.Meta meta = document.getMeta();
            Map<String,Object> canonicalMeta = new LinkedHashMap<>();
            canonicalMeta.put("course",meta == null ? "" : text(meta.getCourse()));
            canonicalMeta.put("courseId",meta == null ? "" : text(meta.getCourseId()));
            canonicalMeta.put("schemaVersion",meta == null ? "" : text(meta.getSchemaVersion()));
            canonical.put("meta", canonicalMeta);
            canonical.put("nodes", nodes);
            canonical.put("edges", edges);
            String json = objectMapper.writeValueAsString(canonical);
            return new NormalizedCourseGraph(document, byId, List.copyOf(nodes), List.copyOf(edges), json, HashUtil.sha256(json));
        } catch (Exception ex) {
            throw new IllegalStateException("课程图谱规范化失败", ex);
        }
    }

    public String relation(String value) {
        String normalized = text(value).toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "包含", "CONTAINS" -> "CONTAINS";
            case "先修", "PREREQUISITE" -> "PREREQUISITE";
            case "相似", "SIMILAR" -> "SIMILAR";
            default -> normalized;
        };
    }

    private String text(String value) { return value == null ? "" : value.trim(); }
    private String nullable(String value) { String v = text(value); return v.isEmpty() ? null : v; }
}
