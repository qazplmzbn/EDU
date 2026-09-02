package com.xyz.question_bank_management_system.modules.knowledge.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.question_bank_management_system.config.CorrelationContext;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.course.mapper.CourseMapper;
import com.xyz.question_bank_management_system.modules.knowledge.dto.GraphVersionRelationRequest;
import com.xyz.question_bank_management_system.modules.knowledge.entity.*;
import com.xyz.question_bank_management_system.modules.knowledge.mapper.*;
import com.xyz.question_bank_management_system.modules.knowledge.repository.Neo4jKnowledgeGraphRepository;
import com.xyz.question_bank_management_system.modules.knowledge.service.KnowledgeGraphVersionService;
import com.xyz.question_bank_management_system.util.HashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class KnowledgeGraphVersionServiceImpl implements KnowledgeGraphVersionService {
    private static final Set<String> TYPES = Set.of("PREREQUISITE","SIMILAR","PART_OF","RELATED_TO","SUPPORTS");
    private final CourseMapper courseMapper;
    private final KnowledgePointMapper pointMapper;
    private final KnowledgeGraphVersionMapper versionMapper;
    private final KnowledgeGraphVersionRelationMapper relationMapper;
    private final KnowledgeGraphSyncRecordMapper syncMapper;
    private final CourseGraphImportMapper importMapper;
    private final Neo4jKnowledgeGraphRepository graphRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Map<String, Object> createDraft(Long courseId, String description, Long operatorId) {
        if (courseId == null || courseMapper.selectById(courseId) == null) throw BizException.of(ErrorCode.NOT_FOUND,"课程不存在");
        KnowledgeGraphVersion value = new KnowledgeGraphVersion();
        value.setCourseId(courseId);
        value.setVersionCode("graph_course" + courseId + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS")));
        value.setDescription(description);
        value.setStatus("DRAFT"); value.setNodeCount(0); value.setEdgeCount(0); value.setReviewStatus("NOT_REQUIRED"); value.setCreatedBy(operatorId); value.setCorrelationId(CorrelationContext.get());
        versionMapper.insert(value);
        return view(value);
    }

    @Override
    @Transactional
    public Map<String, Object> replaceRelations(String versionCode, GraphVersionRelationRequest request, Long operatorId) {
        KnowledgeGraphVersion version = requireVersion(versionCode, true);
        if ("REJECTED".equals(version.getStatus())) { versionMapper.resetRejectedToDraft(version.getId()); version.setStatus("DRAFT"); }
        if (!"DRAFT".equals(version.getStatus())) throw BizException.of(ErrorCode.CONFLICT,"仅 DRAFT 或 REJECTED 图版本可覆盖关系");
        List<GraphVersionRelationRequest.RelationItem> items = request == null ? List.of() : request.getRelations();
        List<KnowledgeGraphVersionRelation> rows = new ArrayList<>();
        Set<String> edgeKeys = new HashSet<>();
        Set<String> relationCodes = new HashSet<>();
        int index = 0;
        for (GraphVersionRelationRequest.RelationItem item : items) {
            KnowledgePoint source = pointMapper.selectByIdAndCourse(item.getSourceKnowledgePointId(),version.getCourseId());
            KnowledgePoint target = pointMapper.selectByIdAndCourse(item.getTargetKnowledgePointId(),version.getCourseId());
            if (source == null || target == null) throw BizException.of(ErrorCode.PARAM_ERROR,"关系知识点不属于当前课程");
            if (Objects.equals(item.getSourceKnowledgePointId(),item.getTargetKnowledgePointId())) throw BizException.of(ErrorCode.PARAM_ERROR,"关系不能形成自环：知识点 " + item.getSourceKnowledgePointId());
            String type = Objects.toString(item.getRelationType(),"").toUpperCase(Locale.ROOT);
            if (!TYPES.contains(type)) throw BizException.of(ErrorCode.PARAM_ERROR,"不支持的关系类型：" + type);
            if (!edgeKeys.add(item.getSourceKnowledgePointId() + ":" + item.getTargetKnowledgePointId() + ":" + type))
                throw BizException.of(ErrorCode.PARAM_ERROR,"同一图版本内存在重复关系：" + item.getSourceKnowledgePointId() + "->" + item.getTargetKnowledgePointId() + "/" + type);
            KnowledgeGraphVersionRelation row = new KnowledgeGraphVersionRelation();
            row.setCourseId(version.getCourseId()); row.setGraphVersionId(version.getId());
            row.setRelationCode(StringUtils.hasText(item.getRelationCode()) ? item.getRelationCode() : "rel_" + version.getId() + "_" + (++index));
            if (!relationCodes.add(row.getRelationCode())) throw BizException.of(ErrorCode.PARAM_ERROR,"关系编码重复：" + row.getRelationCode());
            row.setSourceKnowledgePointId(item.getSourceKnowledgePointId()); row.setTargetKnowledgePointId(item.getTargetKnowledgePointId());
            row.setRelationType(type); row.setWeight(normalize(item.getWeight(),BigDecimal.ONE));
            row.setConfidence(normalize(item.getConfidence(),BigDecimal.ONE)); row.setSourceType(Objects.toString(item.getSourceType(),"TEACHER_CONFIRMED"));
            row.setCreatedBy(operatorId); row.setSourceChunkIds(item.getSourceChunkIds() == null ? List.of() : item.getSourceChunkIds()); rows.add(row);
        }
        relationMapper.deleteEvidence(version.getId()); relationMapper.deleteByVersion(version.getId());
        if (!rows.isEmpty()) relationMapper.batchInsert(rows);
        for (KnowledgeGraphVersionRelation row : rows) for (Long chunkId : row.getSourceChunkIds()) relationMapper.insertEvidence(version.getId(),row.getRelationCode(),chunkId);
        return Map.of("versionCode",versionCode,"status","DRAFT","relationCount",rows.size());
    }

    @Override
    @Transactional
    public Map<String, Object> validate(String versionCode) {
        KnowledgeGraphVersion version = requireVersion(versionCode, true);
        if (!Set.of("DRAFT","REJECTED").contains(version.getStatus())) throw BizException.of(ErrorCode.CONFLICT,"当前图版本不能校验");
        ensureApprovedImport(version);
        // 图谱需要包含分类/组织节点；学习路径才只读取 pathEligible 知识点。
        List<KnowledgePoint> points = pointMapper.selectActiveByCourse(version.getCourseId());
        List<KnowledgeGraphVersionRelation> relations = relationMapper.selectByVersion(version.getId());
        List<Map<String,Object>> issues = validateGraph(points,relations);
        String hash = graphHash(points,relations);
        version.setNodeCount(points.size()); version.setEdgeCount(relations.size()); version.setContentHash(hash);
        try { version.setValidationReportJson(objectMapper.writeValueAsString(Map.of("issues",issues))); }
        catch (Exception ex) { throw BizException.of(ErrorCode.SYSTEM_ERROR,"图校验报告序列化失败"); }
        version.setStatus(issues.isEmpty() ? "VALIDATING" : "REJECTED"); versionMapper.updateValidation(version);
        if (!issues.isEmpty()) return validationView(version,issues);
        KnowledgeGraphSyncRecord sync = syncRecord(version,"RUNNING",null);
        try {
            graphRepository.replaceVersion(version.getCourseId(),version.getVersionCode(),points,relations);
            Neo4jKnowledgeGraphRepository.GraphCounts counts=graphRepository.countVersion(version.getCourseId(),version.getVersionCode());
            if(counts.nodeCount()!=points.size()||counts.edgeCount()!=relations.size())throw BizException.of(ErrorCode.CONFLICT,"Neo4j 回读计数与 MySQL 不一致");
            sync.setStatus("SUCCESS"); sync.setFinishedAt(LocalDateTime.now()); syncMapper.insert(sync);
        } catch (RuntimeException ex) {
            sync.setStatus("FAILED"); sync.setErrorMessage(shortMessage(ex)); sync.setFinishedAt(LocalDateTime.now()); syncMapper.insert(sync);
            issues.add(issue("NEO4J_SYNC_FAILED",null));
            version.setStatus("DRAFT");
            try { version.setValidationReportJson(objectMapper.writeValueAsString(Map.of("issues",issues))); }
            catch (Exception ignored) { version.setValidationReportJson("{\"issues\":[{\"type\":\"NEO4J_SYNC_FAILED\"}]}"); }
            versionMapper.updateValidation(version);
        }
        return validationView(version,issues);
    }

    @Override
    @Transactional
    public Map<String, Object> publish(String versionCode) {
        KnowledgeGraphVersion version = requireVersion(versionCode, true);
        ensureApprovedImport(version);
        if (!"VALIDATING".equals(version.getStatus())) throw BizException.of(ErrorCode.CONFLICT,"只有校验通过的图版本可发布");
        KnowledgeGraphSyncRecord sync=syncMapper.selectLatestSuccess(version.getId());
        if(sync==null||!Objects.equals(sync.getContentHash(),version.getContentHash())||!Objects.equals(sync.getNodeCount(),version.getNodeCount())||!Objects.equals(sync.getEdgeCount(),version.getEdgeCount()))throw BizException.of(ErrorCode.CONFLICT,"图版本缺少一致的 SUCCESS 同步记录");
        KnowledgeGraphVersion active = versionMapper.selectActive(version.getCourseId());
        if (active != null && Objects.equals(active.getContentHash(),version.getContentHash())) throw BizException.of(ErrorCode.CONFLICT,"图内容未变化，不创建空版本");
        versionMapper.archiveActive(version.getCourseId());
        if (versionMapper.activate(version.getId()) != 1) throw BizException.of(ErrorCode.CONFLICT,"图版本状态已变化");
        relationMapper.publishVersionRelations(version.getId());
        if(version.getImportId()!=null)courseMapper.activateImported(version.getCourseId());
        version.setStatus("ACTIVE"); version.setActivatedAt(LocalDateTime.now());
        return view(version);
    }

    private List<Map<String,Object>> validateGraph(List<KnowledgePoint> points,List<KnowledgeGraphVersionRelation> relations) {
        Set<Long> ids = new HashSet<>(); for (KnowledgePoint point : points) if ("ACTIVE".equalsIgnoreCase(Objects.toString(point.getStatus(),"ACTIVE"))) ids.add(point.getId());
        List<Map<String,Object>> issues = new ArrayList<>(); Set<String> seen = new HashSet<>(); Map<Long,List<Long>> edges = new HashMap<>();
        for (KnowledgeGraphVersionRelation r : relations) {
            if (!ids.contains(r.getSourceKnowledgePointId()) || !ids.contains(r.getTargetKnowledgePointId())) issues.add(issue("CROSS_COURSE_OR_DISABLED",r.getRelationCode()));
            if (Objects.equals(r.getSourceKnowledgePointId(),r.getTargetKnowledgePointId())) issues.add(issue("SELF_LOOP",r.getRelationCode()));
            String key=r.getSourceKnowledgePointId()+":"+r.getTargetKnowledgePointId()+":"+r.getRelationType(); if(!seen.add(key)) issues.add(issue("DUPLICATE_EDGE",r.getRelationCode()));
            if (r.getWeight()==null || r.getWeight().compareTo(BigDecimal.ZERO)<0 || r.getWeight().compareTo(BigDecimal.ONE)>0) issues.add(issue("INVALID_WEIGHT",r.getRelationCode()));
            if (relationMapper.countEvidence(r.getId())==0) issues.add(issue("MISSING_SOURCE_EVIDENCE",r.getRelationCode()));
            if ("PREREQUISITE".equals(r.getRelationType())) edges.computeIfAbsent(r.getSourceKnowledgePointId(),x->new ArrayList<>()).add(r.getTargetKnowledgePointId());
        }
        if (hasCycle(edges)) issues.add(issue("PREREQUISITE_CYCLE",null));
        return issues;
    }

    private boolean hasCycle(Map<Long,List<Long>> edges){Set<Long> visiting=new HashSet<>(),done=new HashSet<>();for(Long n:edges.keySet())if(cycle(n,edges,visiting,done))return true;return false;}
    private boolean cycle(Long n,Map<Long,List<Long>> e,Set<Long> v,Set<Long>d){if(v.contains(n))return true;if(d.contains(n))return false;v.add(n);for(Long x:e.getOrDefault(n,List.of()))if(cycle(x,e,v,d))return true;v.remove(n);d.add(n);return false;}
    private Map<String,Object> issue(String type,String relationCode){Map<String,Object> m=new LinkedHashMap<>();m.put("type",type);if(relationCode!=null)m.put("relationCode",relationCode);return m;}
    private void ensureApprovedImport(KnowledgeGraphVersion version){if(version.getImportId()==null)return;CourseGraphImport record=importMapper.selectById(version.getImportId());if(record==null||!"APPROVED".equals(record.getStatus())||importMapper.countUnresolvedErrors(record.getId())>0)throw BizException.of(ErrorCode.CONFLICT,"Imported graph 必须关联无未解决 ERROR 的 APPROVED 导入");}
    private String graphHash(List<KnowledgePoint> points,List<KnowledgeGraphVersionRelation> relations){try{Map<Long,KnowledgePoint> byId=new HashMap<>();points.forEach(p->byId.put(p.getId(),p));List<Map<String,Object>> nodes=points.stream().sorted(Comparator.comparing(p->Objects.toString(p.getCode(),""))).map(p->{Map<String,Object> n=new LinkedHashMap<>();n.put("code",Objects.toString(p.getCode(),""));n.put("name",Objects.toString(p.getName(),""));n.put("knowledgeType",Objects.toString(p.getKnowledgeType(),""));n.put("status",Objects.toString(p.getStatus(),""));n.put("contentVersion",Objects.toString(p.getContentVersion(),""));n.put("metadata",Objects.toString(p.getMetadataJson(),""));return n;}).toList();List<Map<String,Object>> edges=relations.stream().sorted(Comparator.comparing(KnowledgeGraphVersionRelation::getRelationCode)).map(r->{Map<String,Object> e=new LinkedHashMap<>();e.put("relationCode",r.getRelationCode());e.put("sourceCode",byId.containsKey(r.getSourceKnowledgePointId())?byId.get(r.getSourceKnowledgePointId()).getCode():"");e.put("targetCode",byId.containsKey(r.getTargetKnowledgePointId())?byId.get(r.getTargetKnowledgePointId()).getCode():"");e.put("relationType",r.getRelationType());e.put("weight",r.getWeight());e.put("confidence",r.getConfidence());e.put("sourceType",r.getSourceType());return e;}).toList();return HashUtil.sha256(objectMapper.writeValueAsString(Map.of("nodes",nodes,"edges",edges)));}catch(Exception ex){throw BizException.of(ErrorCode.SYSTEM_ERROR,"图内容哈希计算失败");}}
    private String shortMessage(Throwable ex){String value=Objects.toString(ex.getMessage(),ex.getClass().getSimpleName());return value.length()>1000?value.substring(0,1000):value;}
    private BigDecimal normalize(BigDecimal value,BigDecimal fallback){BigDecimal v=value==null?fallback:value;if(v.compareTo(BigDecimal.ZERO)<0||v.compareTo(BigDecimal.ONE)>0)throw BizException.of(ErrorCode.PARAM_ERROR,"权重和置信度必须在 0 到 1 之间");return v;}
    private KnowledgeGraphVersion requireVersion(String code,boolean lock){KnowledgeGraphVersion v=lock?versionMapper.selectByCodeForUpdate(code):versionMapper.selectByCode(code);if(v==null)throw BizException.of(ErrorCode.NOT_FOUND,"图版本不存在");return v;}
    private Map<String,Object> validationView(KnowledgeGraphVersion v,List<Map<String,Object>> issues){Map<String,Object> m=new LinkedHashMap<>(view(v));m.put("issues",issues);return m;}
    private Map<String,Object> view(KnowledgeGraphVersion v){Map<String,Object> m=new LinkedHashMap<>();m.put("versionCode",v.getVersionCode());m.put("courseId",v.getCourseId());m.put("status",v.getStatus());m.put("nodeCount",Objects.requireNonNullElse(v.getNodeCount(),0));m.put("edgeCount",Objects.requireNonNullElse(v.getEdgeCount(),0));m.put("contentHash",Objects.toString(v.getContentHash(),""));return m;}
    private KnowledgeGraphSyncRecord syncRecord(KnowledgeGraphVersion v,String status,String error){KnowledgeGraphSyncRecord s=new KnowledgeGraphSyncRecord();s.setGraphVersionId(v.getId());s.setSyncCode("sync_"+UUID.randomUUID());s.setStatus(status);s.setNodeCount(v.getNodeCount());s.setEdgeCount(v.getEdgeCount());s.setContentHash(v.getContentHash());s.setErrorMessage(error);s.setCorrelationId(CorrelationContext.get());s.setStartedAt(LocalDateTime.now());return s;}
}
