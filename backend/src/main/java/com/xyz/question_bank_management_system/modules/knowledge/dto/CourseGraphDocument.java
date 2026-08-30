package com.xyz.question_bank_management_system.modules.knowledge.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CourseGraphDocument {
    private Meta meta;
    private List<Node> nodes = new ArrayList<>();
    private List<Edge> edges = new ArrayList<>();

    @Data
    public static class Meta {
        private String course;
        private String courseId;
        private String schemaVersion;
    }

    @Data
    public static class Node {
        private String id;
        private String name;
        private String type;
        private Integer level;
        private String parent;
        private List<Resource> resources = new ArrayList<>();
    }

    @Data
    public static class Edge {
        private String source;
        private String target;
        private String relation;
    }

    @Data
    public static class Resource {
        private String modality;
        private String title;
        private String url;
        private String source;
        private String content;
        private String sourceUrl;
    }
}
