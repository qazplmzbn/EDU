package com.xyz.question_bank_management_system.modules.knowledge.model;

import com.xyz.question_bank_management_system.modules.knowledge.dto.CourseGraphDocument;

import java.util.List;
import java.util.Map;

public record NormalizedCourseGraph(
        CourseGraphDocument source,
        Map<String, CourseGraphDocument.Node> nodesById,
        List<NormalizedNode> nodes,
        List<NormalizedEdge> edges,
        String canonicalJson,
        String normalizedHash) {

    public record NormalizedNode(String id, String name, String type, Integer level, String parent) {}

    public record NormalizedEdge(String source, String target, String relation) {}
}
