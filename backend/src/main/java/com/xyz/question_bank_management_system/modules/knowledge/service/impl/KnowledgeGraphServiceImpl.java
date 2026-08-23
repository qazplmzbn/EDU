package com.xyz.question_bank_management_system.modules.knowledge.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.question_bank_management_system.modules.knowledge.dto.KnowledgeGraphExtractionRequest;
import com.xyz.question_bank_management_system.modules.knowledge.entity.QbKnowledgePoint;
import com.xyz.question_bank_management_system.modules.knowledge.entity.QbKnowledgeRelation;
import com.xyz.question_bank_management_system.modules.llm.entity.QbLlmCall;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.knowledge.mapper.QbKnowledgePointMapper;
import com.xyz.question_bank_management_system.modules.knowledge.mapper.QbKnowledgeRelationMapper;
import com.xyz.question_bank_management_system.modules.knowledge.service.KnowledgeGraphService;
import com.xyz.question_bank_management_system.modules.llm.service.LlmService;
import com.xyz.question_bank_management_system.modules.knowledge.vo.KnowledgeGraphExtractionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
public class KnowledgeGraphServiceImpl implements KnowledgeGraphService {

    private static final int BIZ_TYPE_KNOWLEDGE_GRAPH = 3;
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024L;
    private static final int MAX_SOURCE_TEXT_LENGTH = 60000;

    private final QbKnowledgePointMapper knowledgePointMapper;
    private final QbKnowledgeRelationMapper relationMapper;
    private final LlmService llmService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<QbKnowledgeRelation> relations() {
        return relationMapper.selectAll();
    }

    @Override
    public KnowledgeGraphExtractionVO extract(KnowledgeGraphExtractionRequest request) {
        List<QbKnowledgePoint> points = knowledgePointMapper.selectAll();
        String prompt = buildPrompt(points, request == null ? null : request.getSourceText());
        QbLlmCall call = llmService.chatCompletion(BIZ_TYPE_KNOWLEDGE_GRAPH, 0L, prompt,
                request == null ? null : request.getProviderKey());
        String raw = llmService.extractContent(call.getResponseText());
        if (!StringUtils.hasText(raw)) {
            raw = call.getResponseText();
        }

        List<QbKnowledgeRelation> extracted = parseRelations(raw, points);
        int saved = 0;
        if (request != null && Boolean.TRUE.equals(request.getAutoSave())) {
            for (QbKnowledgeRelation relation : extracted) {
                if (relation.getSourceId() == null || relation.getTargetId() == null || relation.getSourceId().equals(relation.getTargetId())) {
                    continue;
                }
                relationMapper.upsert(relation);
                saved++;
            }
        }

        KnowledgeGraphExtractionVO vo = new KnowledgeGraphExtractionVO();
        vo.setLlmCallId(call.getId());
        vo.setRawText(raw);
        vo.setSavedCount(saved);
        vo.setRelations(extracted);
        return vo;
    }

    @Override
    public KnowledgeGraphExtractionVO extractFromFile(MultipartFile file, Boolean autoSave, String providerKey) {
        String sourceText = readKnowledgeBaseFile(file);
        KnowledgeGraphExtractionRequest request = new KnowledgeGraphExtractionRequest();
        request.setSourceText(sourceText);
        request.setAutoSave(autoSave);
        request.setProviderKey(providerKey);
        return extract(request);
    }

    @Override
    public Long createRelation(QbKnowledgeRelation relation) {
        QbKnowledgeRelation normalized = normalizeManualRelation(relation);
        relationMapper.upsert(normalized);
        return normalized.getId();
    }

    @Override
    public void updateRelation(Long relationId, QbKnowledgeRelation relation) {
        QbKnowledgeRelation existing = relationMapper.selectById(relationId);
        if (existing == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "知识关系不存在");
        }
        QbKnowledgeRelation normalized = normalizeManualRelation(relation);
        normalized.setId(relationId);
        int updated = relationMapper.update(normalized);
        if (updated <= 0) {
            throw BizException.of(ErrorCode.BIZ_ERROR, "更新知识关系失败");
        }
    }

    @Override
    public void deleteRelation(Long relationId) {
        relationMapper.softDelete(relationId);
    }

    private QbKnowledgeRelation normalizeManualRelation(QbKnowledgeRelation relation) {
        if (relation == null || relation.getSourceId() == null || relation.getTargetId() == null) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "请选择前置知识点和后续知识点");
        }
        if (relation.getSourceId().equals(relation.getTargetId())) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "前置知识点和后续知识点不能相同");
        }
        QbKnowledgePoint source = knowledgePointMapper.selectById(relation.getSourceId());
        QbKnowledgePoint target = knowledgePointMapper.selectById(relation.getTargetId());
        if (source == null || target == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "知识点不存在");
        }

        QbKnowledgeRelation normalized = new QbKnowledgeRelation();
        normalized.setSourceId(source.getId());
        normalized.setTargetId(target.getId());
        normalized.setRelationType(text(relation.getRelationType(), "prerequisite"));
        normalized.setWeight(clamp(relation.getWeight(), 0.0, 10.0, 1.0));
        normalized.setConfidence(clamp(relation.getConfidence(), 0.0, 1.0, 1.0));
        normalized.setSourceType(text(relation.getSourceType(), "manual"));
        normalized.setDescription(relation.getDescription());
        return normalized;
    }

    private double clamp(Double value, double min, double max, double fallback) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return fallback;
        }
        return Math.max(min, Math.min(max, value));
    }

    private String buildPrompt(List<QbKnowledgePoint> points, String sourceText) {
        String pointList = points.stream()
                .map(p -> "- id=" + p.getId() + ", name=" + p.getName() + ", code=" + p.getCode())
                .collect(Collectors.joining("\n"));
        return """
                你是课程知识图谱抽取助手。请根据已有知识点和课程文本，抽取知识点之间的先修关系。
                只能从“已有知识点列表”中选择 sourceName 和 targetName，不要创造新知识点。
                sourceName 表示前置知识点，targetName 表示依赖它的后续知识点。
                只输出 JSON 数组，不要输出解释文字。
                JSON 格式：
                [
                  {"sourceName":"变量与数据类型","targetName":"数组与字符串","relationType":"prerequisite","weight":1.0,"confidence":0.8,"description":"..."}
                ]

                已有知识点列表：
                %s

                课程文本：
                %s
                """.formatted(pointList, sourceText == null ? "" : sourceText);
    }

    private String readKnowledgeBaseFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "请先上传知识库文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "知识库文件不能超过 5MB");
        }

        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = extensionOf(filename);
        try {
            String text;
            if ("docx".equals(extension)) {
                text = readDocx(file.getBytes());
            } else if (List.of("txt", "md", "csv", "json", "sql", "html", "htm").contains(extension)) {
                text = new String(file.getBytes(), StandardCharsets.UTF_8);
                if ("html".equals(extension) || "htm".equals(extension)) {
                    text = text.replaceAll("(?is)<script.*?</script>", " ")
                            .replaceAll("(?is)<style.*?</style>", " ")
                            .replaceAll("(?s)<[^>]+>", " ");
                }
            } else {
                throw BizException.of(ErrorCode.PARAM_ERROR, "暂只支持 txt、md、csv、json、sql、html、docx 文件");
            }
            text = normalizeSourceText(text);
            if (!StringUtils.hasText(text)) {
                throw BizException.of(ErrorCode.PARAM_ERROR, "知识库文件中没有可抽取的文本内容");
            }
            return text;
        } catch (BizException e) {
            throw e;
        } catch (IOException e) {
            throw BizException.of(ErrorCode.BIZ_ERROR, "读取知识库文件失败");
        }
    }

    private String readDocx(byte[] bytes) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    String xml = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                    return xml.replaceAll("(?s)<w:tab\\s*/>", "\t")
                            .replaceAll("(?s)</w:p>", "\n")
                            .replaceAll("(?s)<[^>]+>", " ");
                }
            }
        }
        return "";
    }

    private String normalizeSourceText(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace('\u00A0', ' ')
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        if (normalized.length() > MAX_SOURCE_TEXT_LENGTH) {
            return normalized.substring(0, MAX_SOURCE_TEXT_LENGTH);
        }
        return normalized;
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase();
    }

    private List<QbKnowledgeRelation> parseRelations(String raw, List<QbKnowledgePoint> points) {
        Map<String, QbKnowledgePoint> byName = points.stream()
                .filter(p -> StringUtils.hasText(p.getName()))
                .collect(Collectors.toMap(QbKnowledgePoint::getName, Function.identity(), (a, b) -> a));
        List<QbKnowledgeRelation> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(extractJsonArray(raw));
            if (!root.isArray()) {
                return result;
            }
            for (JsonNode item : root) {
                QbKnowledgePoint source = byName.get(item.path("sourceName").asText(""));
                QbKnowledgePoint target = byName.get(item.path("targetName").asText(""));
                if (source == null || target == null) {
                    continue;
                }
                QbKnowledgeRelation relation = new QbKnowledgeRelation();
                relation.setSourceId(source.getId());
                relation.setTargetId(target.getId());
                relation.setSourceName(source.getName());
                relation.setTargetName(target.getName());
                relation.setRelationType(text(item.path("relationType").asText(""), "prerequisite"));
                relation.setWeight(item.path("weight").isNumber() ? item.path("weight").asDouble() : 1.0);
                relation.setConfidence(item.path("confidence").isNumber() ? item.path("confidence").asDouble() : 0.7);
                relation.setSourceType("llm");
                relation.setDescription(item.path("description").asText(""));
                result.add(relation);
            }
        } catch (Exception ignore) {
        }
        return result;
    }

    private String extractJsonArray(String raw) {
        if (raw == null) {
            return "[]";
        }
        int start = raw.indexOf('[');
        int end = raw.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw;
    }

    private String text(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
