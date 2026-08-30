package com.xyz.question_bank_management_system.modules.knowledge.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.knowledge.dto.CourseGraphDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class CourseGraphJsonParser {
    private final ObjectMapper objectMapper;

    public ParsedCourseGraph parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "请选择课程图谱 JSON 文件");
        }
        try {
            return parse(file.getBytes(), file.getOriginalFilename());
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "课程图谱文件读取失败");
        }
    }

    public ParsedCourseGraph parse(byte[] bytes, String fileName) {
        if (bytes == null || bytes.length == 0) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "课程图谱 JSON 不能为空");
        }
        try {
            String raw = new String(bytes, StandardCharsets.UTF_8);
            ObjectMapper mapper = objectMapper.copy();
            mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            CourseGraphDocument document = mapper.readValue(raw, CourseGraphDocument.class);
            if (document.getNodes() == null) document.setNodes(new java.util.ArrayList<>());
            if (document.getEdges() == null) document.setEdges(new java.util.ArrayList<>());
            return new ParsedCourseGraph(fileName == null ? "course-graph.json" : fileName, bytes, raw, document);
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "课程图谱 JSON 格式不正确：" + ex.getMessage());
        }
    }

    public record ParsedCourseGraph(String fileName, byte[] bytes, String rawText, CourseGraphDocument document) {}
}
