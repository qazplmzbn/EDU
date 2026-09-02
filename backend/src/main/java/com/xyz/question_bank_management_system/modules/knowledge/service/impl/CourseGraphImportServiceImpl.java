package com.xyz.question_bank_management_system.modules.knowledge.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.question_bank_management_system.config.CorrelationContext;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.course.entity.Course;
import com.xyz.question_bank_management_system.modules.course.entity.CourseChapter;
import com.xyz.question_bank_management_system.modules.course.entity.CourseKnowledge;
import com.xyz.question_bank_management_system.modules.course.mapper.CourseChapterMapper;
import com.xyz.question_bank_management_system.modules.course.mapper.CourseKnowledgeMapper;
import com.xyz.question_bank_management_system.modules.course.mapper.CourseMapper;
import com.xyz.question_bank_management_system.modules.knowledge.dto.CourseGraphDocument;
import com.xyz.question_bank_management_system.modules.knowledge.entity.*;
import com.xyz.question_bank_management_system.modules.knowledge.mapper.*;
import com.xyz.question_bank_management_system.modules.knowledge.model.NormalizedCourseGraph;
import com.xyz.question_bank_management_system.modules.knowledge.service.CourseGraphImportService;
import com.xyz.question_bank_management_system.modules.knowledge.vo.CourseGraphImportDetailVO;
import com.xyz.question_bank_management_system.modules.knowledge.vo.CourseGraphValidationVO;
import com.xyz.question_bank_management_system.modules.source.entity.FileAsset;
import com.xyz.question_bank_management_system.modules.source.entity.SourceChunk;
import com.xyz.question_bank_management_system.modules.source.entity.SourceDocument;
import com.xyz.question_bank_management_system.modules.source.mapper.FileAssetMapper;
import com.xyz.question_bank_management_system.modules.source.mapper.SourceChunkMapper;
import com.xyz.question_bank_management_system.modules.source.mapper.SourceDocumentMapper;
import com.xyz.question_bank_management_system.util.HashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CourseGraphImportServiceImpl implements CourseGraphImportService {
    private static final String MODE = "STRUCTURE_ONLY";

    private final CourseGraphJsonParser parser;
    private final CourseGraphNormalizer normalizer;
    private final CourseGraphValidator validator;
    private final CourseGraphImportMapper importMapper;
    private final CourseMapper courseMapper;
    private final CourseChapterMapper chapterMapper;
    private final CourseKnowledgeMapper courseKnowledgeMapper;
    private final KnowledgePointMapper pointMapper;
    private final KnowledgeGraphVersionMapper versionMapper;
    private final KnowledgeGraphVersionRelationMapper relationMapper;
    private final FileAssetMapper fileAssetMapper;
    private final SourceDocumentMapper documentMapper;
    private final SourceChunkMapper chunkMapper;
    private final ObjectMapper objectMapper;
    private final CourseGraphImportPolicy importPolicy;

    @Value("${app.file.storage-dir:uploads}") private String storageDir;

    @Override
    public CourseGraphValidationVO validate(MultipartFile file, String mode) {
        return analyze(parser.parse(file), mode).view();
    }

    @Override
    @Transactional
    public CourseGraphImportDetailVO commit(MultipartFile file, String mode, String validationHash,
                                             String idempotencyKey, Long operatorId) {
        if (!StringUtils.hasText(idempotencyKey)) throw BizException.of(ErrorCode.PARAM_ERROR,"Idempotency-Key 不能为空");
        Analysis analysis = analyze(parser.parse(file), mode);
        if (!Objects.equals(analysis.view().getValidationHash(), validationHash)) {
            throw BizException.of(ErrorCode.CONFLICT,"validationHash 与当前文件校验结果不一致");
        }

        CourseGraphImport old = importMapper.selectByIdempotency(operatorId,idempotencyKey);
        if (old != null) {
            if (!sameRequest(old,analysis)) throw BizException.of(ErrorCode.CONFLICT,"幂等键已用于其他课程图谱内容");
            return detail(old.getImportCode());
        }
        CourseGraphImport sameSource = importMapper.selectBySource(analysis.view().getCourseCode(),analysis.view().getSourceFileHash(),analysis.view().getSchemaVersion());
        if (sameSource != null) return detail(sameSource.getImportCode());

        CourseGraphImport record = record(analysis,idempotencyKey,operatorId);
        record.setStatus(analysis.view().isValid()?"VALIDATED":"REJECTED");
        importMapper.insert(record);
        insertIssues(record.getId(),analysis.view());
        if (!analysis.view().isValid()) return detail(record.getImportCode());

        requireImportAdmission(analysis.view());
        boolean legacyBridge = importPolicy.usesLegacyBridge(analysis.view().getCourseCode());
        LegacyReferenceBaseline legacyBaseline = legacyBridge ? captureLegacyReferences() : null;
        Course course = requireOrCreateDraftCourse(analysis.view());
        record.setCourseId(course.getId());
        // Retire legacy bridge rows before inserting imported points.  The old
        // implementation did this afterwards, so an empty/rehearsal database
        // could assign a legacy numeric id to a newly imported point and then
        // disable that new point by mistake.
        if (legacyBridge) {
            archiveLegacy(record.getId());
        }

        Map<String,CourseChapter> chapters = insertChapters(course.getId(),analysis.normalized());
        Map<String,KnowledgePoint> points = insertPoints(course.getId(),chapters,analysis.normalized(),analysis.view().getNormalizedHash());
        replaceCourseKnowledge(course.getId(),points, legacyBridge);

        KnowledgeGraphVersion version = insertGraphVersion(course.getId(),record,analysis.view());
        List<KnowledgeGraphVersionRelation> relations = insertRelations(course.getId(),version,points,analysis.normalized(),operatorId);
        SourceDocument source = persistSource(file,course.getId(),record,analysis.view());
        insertRelationEvidence(source,version,relations,analysis.normalized(),analysis.view().getSourceFileHash());

        if (legacyBridge) {
            verifyLegacyReferences(legacyBaseline);
        }
        record.setGraphVersionId(version.getId());
        record.setStatus("IMPORTED");
        importMapper.updateImported(record);
        return detail(record.getImportCode());
    }

    @Override
    public CourseGraphImportDetailVO detail(String importCode) {
        CourseGraphImport value = importMapper.selectByCode(importCode);
        if (value == null) throw BizException.of(ErrorCode.NOT_FOUND,"课程图谱导入记录不存在");
        CourseGraphImportDetailVO vo = new CourseGraphImportDetailVO();
        vo.setImportCode(value.getImportCode());vo.setStatus(value.getStatus());vo.setCourseCode(value.getCourseCode());vo.setCourseName(value.getCourseName());
        vo.setSchemaVersion(value.getSchemaVersion());vo.setMode(value.getMode());vo.setSourceFileName(value.getSourceFileName());
        vo.setSourceFileHash(value.getSourceFileHash());vo.setNormalizedHash(value.getNormalizedHash());vo.setValidationHash(value.getValidationHash());vo.setCourseId(value.getCourseId());
        KnowledgeGraphVersion graph = value.getGraphVersionId()==null?null:versionMapper.selectByImportId(value.getId());
        vo.setGraphVersionCode(graph==null?null:graph.getVersionCode());vo.setCounts(counts(value));
        for(CourseGraphImportIssue issue:importMapper.selectIssues(value.getId())){
            CourseGraphValidationVO.Issue row=new CourseGraphValidationVO.Issue(issue.getSeverity(),issue.getIssueCode(),issue.getLocationType(),issue.getLocationCode(),issue.getMessage());
            if("ERROR".equals(issue.getSeverity()))vo.getErrors().add(row);else vo.getWarnings().add(row);
        }
        vo.setLegacyMappings(importMapper.selectLegacyMappings(value.getId()));vo.setCreatedBy(value.getCreatedBy());vo.setReviewedBy(value.getReviewedBy());vo.setReviewedAt(value.getReviewedAt());vo.setCreatedAt(value.getCreatedAt());
        return vo;
    }

    @Override
    @Transactional
    public CourseGraphImportDetailVO approve(String importCode, Long reviewerId) {
        CourseGraphImport value = importMapper.selectByCodeForUpdate(importCode);
        if (value == null) throw BizException.of(ErrorCode.NOT_FOUND,"课程图谱导入记录不存在");
        if ("APPROVED".equals(value.getStatus())) return detail(importCode);
        if (!"IMPORTED".equals(value.getStatus())) throw BizException.of(ErrorCode.CONFLICT,"只有 IMPORTED 导入可审核通过");
        if (importMapper.countUnresolvedErrors(value.getId()) > 0) throw BizException.of(ErrorCode.CONFLICT,"导入仍有未解决 ERROR");
        KnowledgeGraphVersion graph=versionMapper.selectByImportId(value.getId());
        if(graph==null||!"DRAFT".equals(graph.getStatus()))throw BizException.of(ErrorCode.CONFLICT,"导入缺少 DRAFT 图版本");
        int expectedRelations = Objects.requireNonNullElse(value.getPrerequisiteCount(),0) + Objects.requireNonNullElse(value.getSimilarCount(),0);
        int expectedPersistedPoints = Objects.requireNonNullElse(value.getKnowledgePointCount(),0) + Objects.requireNonNullElse(value.getCategoryCount(),0);
        if(importMapper.countActiveChapters(value.getCourseId())!=Objects.requireNonNullElse(value.getModuleCount(),0)
                ||importMapper.countActivePoints(value.getCourseId())!=expectedPersistedPoints
                ||importMapper.countGraphRelations(graph.getId())!=expectedRelations
                ||importMapper.countGraphEvidence(graph.getId())!=expectedRelations)throw BizException.of(ErrorCode.CONFLICT,"导入的章节、知识点、关系或证据实际计数不完整");
        if(importMapper.approve(value.getId(),reviewerId)!=1)throw BizException.of(ErrorCode.CONFLICT,"导入状态已变化");
        versionMapper.approveReview(graph.getId(),reviewerId);documentMapper.approveImport(importCode);importMapper.approveLegacyMappings(value.getId());
        return detail(importCode);
    }

    private Analysis analyze(CourseGraphJsonParser.ParsedCourseGraph parsed,String requestedMode){
        String mode=Objects.toString(requestedMode,MODE).trim().toUpperCase(Locale.ROOT);
        NormalizedCourseGraph normalized=normalizer.normalize(parsed.document());
        List<CourseGraphValidationVO.Issue> issues=new ArrayList<>(validator.validate(normalized));
        CourseGraphValidationVO vo=new CourseGraphValidationVO();CourseGraphDocument.Meta meta=parsed.document().getMeta();
        vo.setCourseCode(meta==null?null:meta.getCourseId());vo.setCourseName(meta==null?null:meta.getCourse());vo.setSchemaVersion(meta==null?null:meta.getSchemaVersion());vo.setMode(mode);vo.setCounts(count(normalized));
        if(!MODE.equals(mode))issues.add(new CourseGraphValidationVO.Issue("ERROR","MODE_UNSUPPORTED","META","mode","首轮只支持 STRUCTURE_ONLY"));
        if(importPolicy.usesLegacyBridge(vo.getCourseCode()) && normalized.nodes().stream().noneMatch(n -> n.type().equals("KnowledgePoint") && (n.id().toUpperCase(Locale.ROOT).contains("STRING") || n.name().contains("字符串"))))issues.add(new CourseGraphValidationVO.Issue("WARNING","STRING_NODE_MISSING","LEGACY","4","旧“数组与字符串”只能映射到 C.11，图谱缺少独立字符串节点"));
        issues.sort(Comparator.comparing(CourseGraphValidationVO.Issue::getSeverity).thenComparing(CourseGraphValidationVO.Issue::getIssueCode).thenComparing(x->Objects.toString(x.getLocationCode(),"")));
        issues.forEach(x->{if("ERROR".equals(x.getSeverity()))vo.getErrors().add(x);else vo.getWarnings().add(x);});
        vo.setSourceFileHash(HashUtil.sha256(parsed.rawText()));vo.setNormalizedHash(normalized.normalizedHash());vo.setValid(vo.getErrors().isEmpty());
        try{Map<String,Object> validation=new LinkedHashMap<>();validation.put("mode",mode);validation.put("sourceFileHash",vo.getSourceFileHash());validation.put("normalizedHash",vo.getNormalizedHash());validation.put("counts",vo.getCounts());validation.put("issues",issues);vo.setValidationHash(HashUtil.sha256(objectMapper.writeValueAsString(validation)));}catch(Exception ex){throw new IllegalStateException(ex);}
        return new Analysis(parsed,normalized,vo);
    }

    private CourseGraphValidationVO.Counts count(NormalizedCourseGraph graph){CourseGraphValidationVO.Counts c=new CourseGraphValidationVO.Counts();for(var n:graph.nodes())switch(n.type()){case"Course"->c.setCourse(c.getCourse()+1);case"Module"->c.setModule(c.getModule()+1);case"Category"->c.setCategory(c.getCategory()+1);case"KnowledgePoint"->c.setKnowledgePoint(c.getKnowledgePoint()+1);}for(var e:graph.edges())switch(e.relation()){case"CONTAINS"->c.setContains(c.getContains()+1);case"PREREQUISITE"->c.setPrerequisite(c.getPrerequisite()+1);case"SIMILAR"->c.setSimilar(c.getSimilar()+1);}return c;}
    private void requireImportAdmission(CourseGraphValidationVO v){
        if(!MODE.equals(v.getMode())||!StringUtils.hasText(v.getCourseCode())||!StringUtils.hasText(v.getCourseName()))throw BizException.of(ErrorCode.PARAM_ERROR,"导入必须提供 STRUCTURE_ONLY 方式和课程信息");
        CourseGraphValidationVO.Counts c=v.getCounts();
        if(c.getCourse()!=1||c.getModule()<1||c.getKnowledgePoint()<1||c.getContains()<c.getModule()+c.getKnowledgePoint())throw BizException.of(ErrorCode.PARAM_ERROR,"课程图谱结构计数不完整");
    }
    private boolean sameRequest(CourseGraphImport old,Analysis a){return Objects.equals(old.getSourceFileHash(),a.view().getSourceFileHash())&&Objects.equals(old.getValidationHash(),a.view().getValidationHash())&&Objects.equals(old.getMode(),a.view().getMode());}
    private CourseGraphImport record(Analysis a,String key,Long user){CourseGraphValidationVO v=a.view();CourseGraphImport r=new CourseGraphImport();r.setImportCode("cgi_"+UUID.randomUUID().toString().replace("-",""));r.setIdempotencyKey(key);r.setCourseCode(Objects.toString(v.getCourseCode(),"UNKNOWN"));r.setCourseName(Objects.toString(v.getCourseName(),"UNKNOWN"));r.setSchemaVersion(Objects.toString(v.getSchemaVersion(),"UNKNOWN"));r.setMode(v.getMode());r.setSourceFileName(a.parsed().fileName());r.setSourceFileHash(v.getSourceFileHash());r.setNormalizedHash(v.getNormalizedHash());r.setValidationHash(v.getValidationHash());r.setNodeCount(v.getCounts().nodeCount());r.setModuleCount(v.getCounts().getModule());r.setCategoryCount(v.getCounts().getCategory());r.setKnowledgePointCount(v.getCounts().getKnowledgePoint());r.setContainsCount(v.getCounts().getContains());r.setPrerequisiteCount(v.getCounts().getPrerequisite());r.setSimilarCount(v.getCounts().getSimilar());r.setErrorCount(v.getErrors().size());r.setWarningCount(v.getWarnings().size());r.setCreatedBy(user);r.setCorrelationId(Objects.toString(CorrelationContext.get(),"course-graph-import"));return r;}
    private void insertIssues(Long importId,CourseGraphValidationVO v){List<CourseGraphImportIssue> rows=new ArrayList<>();for(var i:concat(v.getErrors(),v.getWarnings())){CourseGraphImportIssue x=new CourseGraphImportIssue();x.setImportId(importId);x.setSeverity(i.getSeverity());x.setIssueCode(i.getIssueCode());x.setLocationType(i.getLocationType());x.setLocationCode(i.getLocationCode());x.setMessage(i.getMessage());rows.add(x);}if(!rows.isEmpty())importMapper.batchInsertIssues(rows);}
    private List<CourseGraphValidationVO.Issue> concat(List<CourseGraphValidationVO.Issue>a,List<CourseGraphValidationVO.Issue>b){List<CourseGraphValidationVO.Issue>x=new ArrayList<>(a);x.addAll(b);return x;}
    private Course requireOrCreateDraftCourse(CourseGraphValidationVO v){
        Course c=courseMapper.selectByCode(v.getCourseCode());
        if(c!=null)return c;
        Course created=new Course();created.setCourseCode(v.getCourseCode());created.setCourseName(v.getCourseName());created.setDescription("Imported course graph draft");created.setStatus("draft");courseMapper.insert(created);return created;
    }
    private Map<String,CourseChapter> insertChapters(Long courseId,NormalizedCourseGraph g){Map<String,CourseChapter> out=new LinkedHashMap<>();int order=0;for(var n:g.nodes())if("Module".equals(n.type())){CourseChapter existing=chapterMapper.selectByCode(courseId,n.id());if(existing!=null){out.put(n.id(),existing);continue;}CourseChapter c=new CourseChapter();c.setCourseId(courseId);c.setChapterCode(n.id());c.setChapterName(n.name());c.setOrderNo(++order);c.setStatus("ACTIVE");chapterMapper.insert(c);out.put(n.id(),c);}return out;}
    private Map<String,KnowledgePoint> insertPoints(Long courseId,Map<String,CourseChapter> chapters,NormalizedCourseGraph g,String version){Map<String,KnowledgePoint> out=new LinkedHashMap<>();Map<String,String> parent=new HashMap<>();for(var n:g.nodes())parent.put(n.id(),n.parent());for(var n:g.nodes())if(Set.of("KnowledgePoint","Category").contains(n.type())){if(pointMapper.selectByCourseAndCode(courseId,n.id())!=null)throw BizException.of(ErrorCode.CONFLICT,"知识点编码已存在："+n.id());String chapterCode=n.parent();while(chapterCode!=null&&!chapters.containsKey(chapterCode))chapterCode=parent.get(chapterCode);CourseChapter chapter=chapters.get(chapterCode);if(chapter==null)throw BizException.of(ErrorCode.PARAM_ERROR,"知识点缺少所属章节："+n.id());KnowledgePoint p=new KnowledgePoint();p.setCourseId(courseId);p.setChapterId(chapter.getId());p.setName(n.name());p.setCode(n.id());p.setLevel(Math.max(1,Objects.requireNonNullElse(n.level(),2)));p.setKnowledgeType("CONCEPT");p.setStatus("ACTIVE");p.setContentVersion(version);try{Map<String,Object> metadata=new LinkedHashMap<>();metadata.put("originalNodeType",n.type());metadata.put("pathEligible",!"Category".equals(n.type()));metadata.put("sourceFile",g.source().getMeta().getCourseId());metadata.put("schemaVersion",g.source().getMeta().getSchemaVersion());metadata.put("originalParent",Objects.toString(n.parent(),""));p.setMetadataJson(objectMapper.writeValueAsString(metadata));}catch(Exception ex){throw new IllegalStateException(ex);}pointMapper.insertCourseGraph(p);out.put(n.id(),p);}return out;}
    private void replaceCourseKnowledge(Long courseId,Map<String,KnowledgePoint> points,boolean legacyBridge){if(legacyBridge)importMapper.deleteLegacyBridge();List<CourseKnowledge> rows=new ArrayList<>();int seq=0;for(KnowledgePoint p:points.values())if(!p.getMetadataJson().contains("\"pathEligible\":false")){CourseKnowledge ck=new CourseKnowledge();ck.setCourseId(courseId);ck.setKnowledgePointId(p.getId());ck.setSequenceNo(++seq);ck.setIsCore(1);ck.setCoverageWeight(BigDecimal.ONE);rows.add(ck);}courseKnowledgeMapper.batchInsert(rows);}
    private KnowledgeGraphVersion insertGraphVersion(Long courseId,CourseGraphImport record,CourseGraphValidationVO v){KnowledgeGraphVersion g=new KnowledgeGraphVersion();g.setVersionCode("graph_"+v.getCourseCode().replaceAll("[^A-Za-z0-9_-]","_")+"_"+v.getNormalizedHash().substring(0,12));g.setCourseId(courseId);g.setDescription("Imported course graph "+record.getImportCode());g.setStatus("DRAFT");g.setNodeCount(v.getCounts().getKnowledgePoint());g.setEdgeCount(v.getCounts().getPrerequisite()+v.getCounts().getSimilar());g.setCorrelationId(record.getCorrelationId());g.setCreatedBy(record.getCreatedBy());g.setImportId(record.getId());g.setReviewStatus("PENDING");versionMapper.insert(g);return g;}
    private List<KnowledgeGraphVersionRelation> insertRelations(Long courseId,KnowledgeGraphVersion version,Map<String,KnowledgePoint> points,NormalizedCourseGraph graph,Long operator){List<KnowledgeGraphVersionRelation> rows=new ArrayList<>();int order=0;for(var e:graph.edges())if(Set.of("PREREQUISITE","SIMILAR").contains(e.relation())){KnowledgePoint s=points.get(e.source()),t=points.get(e.target());if(s==null||t==null)throw BizException.of(ErrorCode.PARAM_ERROR,"最终关系端点必须是知识点");KnowledgeGraphVersionRelation r=new KnowledgeGraphVersionRelation();r.setCourseId(courseId);r.setGraphVersionId(version.getId());r.setRelationCode(String.format("rel_%s_%03d",version.getVersionCode().substring(8),++order));r.setSourceKnowledgePointId(s.getId());r.setTargetKnowledgePointId(t.getId());r.setRelationType(e.relation());r.setWeight(BigDecimal.ONE);r.setConfidence(BigDecimal.ONE);r.setSourceType("IMPORTED_CURRICULUM");r.setCreatedBy(operator);rows.add(r);}relationMapper.batchInsert(rows);return relationMapper.selectByVersion(version.getId());}
    private SourceDocument persistSource(MultipartFile file,Long courseId,CourseGraphImport record,CourseGraphValidationVO v){try{Path dir=Paths.get(storageDir).toAbsolutePath().normalize().resolve("course-graph-imports");Files.createDirectories(dir);Path target=dir.resolve(v.getSourceFileHash()+".json");if(!Files.exists(target))Files.write(target,file.getBytes());FileAsset asset=new FileAsset();asset.setBizType("course_graph_import");asset.setFileName(record.getSourceFileName());asset.setFileExt("json");asset.setMimeType(file.getContentType());asset.setStorageType("local");asset.setStoragePath(target.toString());asset.setFileSize(file.getSize());asset.setFileHash(v.getSourceFileHash());asset.setUploadedBy(record.getCreatedBy());fileAssetMapper.insert(asset);SourceDocument d=new SourceDocument();d.setCourseId(courseId);d.setTitle(record.getCourseName()+"知识图谱 "+record.getImportCode());d.setDocumentType("course");d.setSourceKind("COURSE_GRAPH_JSON");d.setReviewStatus("PENDING");d.setImportCode(record.getImportCode());d.setFileAssetId(asset.getId());d.setVersion(record.getSchemaVersion());d.setAuthorityLevel(3);d.setContentHash(v.getSourceFileHash());d.setParseStatus("parsed");documentMapper.insertCourseGraph(d);asset.setBizId(d.getId());fileAssetMapper.updateBizId(asset);return d;}catch(Exception ex){throw BizException.of(ErrorCode.BIZ_ERROR,"保存课程图谱来源文件失败："+ex.getMessage());}}
    private void insertRelationEvidence(SourceDocument source,KnowledgeGraphVersion version,List<KnowledgeGraphVersionRelation> relations,NormalizedCourseGraph graph,String fileHash){Map<String,NormalizedCourseGraph.NormalizedEdge> edgeByKey=new LinkedHashMap<>();for(var e:graph.edges())if(Set.of("PREREQUISITE","SIMILAR").contains(e.relation()))edgeByKey.put(e.relation()+":"+e.source()+":"+e.target(),e);if(relations.size()!=edgeByKey.size())throw BizException.of(ErrorCode.CONFLICT,"关系证据数量与最终关系不一致");int index=0;Iterator<NormalizedCourseGraph.NormalizedEdge> iterator=edgeByKey.values().iterator();for(KnowledgeGraphVersionRelation relation:relations){var edge=iterator.next();String content="source="+edge.source()+"\ntarget="+edge.target()+"\nrelation="+edge.relation()+"\nfileHash="+fileHash;SourceChunk chunk=new SourceChunk();chunk.setDocumentId(source.getId());chunk.setChunkIndex(index++);chunk.setSectionTitle(edge.relation()+" "+edge.source()+" -> "+edge.target());chunk.setContent(content);chunk.setContentHash(HashUtil.sha256(content));chunk.setTokenCount(content.length());chunkMapper.insert(chunk);relationMapper.insertEvidenceTyped(version.getId(),relation.getRelationCode(),chunk.getId(),"IMPORTED_CURRICULUM",content,BigDecimal.ONE);}}
    private void archiveLegacy(Long importId){importMapper.disableLegacyPoints();for(LegacySpec spec:legacy()){KnowledgePointLegacyMapping m=new KnowledgePointLegacyMapping();m.setImportId(importId);m.setLegacyKnowledgePointId(spec.id());m.setTargetType(spec.type());m.setTargetExternalCode(spec.target());m.setMappingType(spec.mapping());m.setConfidence(spec.confidence());m.setReviewStatus("PENDING");m.setNotes(spec.notes());importMapper.insertLegacyMapping(m);}}
    private List<LegacySpec> legacy(){return List.of(new LegacySpec(1L,"COURSE","C","COURSE_SCOPE",BigDecimal.ONE,"C语言课程范围"),new LegacySpec(2L,"CHAPTER","C.1","CHAPTER_SCOPE",BigDecimal.ONE,"变量与数据类型"),new LegacySpec(3L,"KNOWLEDGE_POINT","C.4.1","DIRECT",new BigDecimal("0.9000"),"分支"),new LegacySpec(3L,"KNOWLEDGE_POINT","C.4.2","DIRECT",new BigDecimal("0.9000"),"循环"),new LegacySpec(4L,"CHAPTER","C.11","CHAPTER_SCOPE",new BigDecimal("0.8000"),"字符串节点缺失"),new LegacySpec(5L,"CHAPTER","C.9","CHAPTER_SCOPE",BigDecimal.ONE,"函数"),new LegacySpec(6L,"CHAPTER","C.12","CHAPTER_SCOPE",BigDecimal.ONE,"指针"),new LegacySpec(7L,"NONE",null,"ARCHIVED_TEST_DATA",BigDecimal.ONE,"测试节点归档"));}
    private LegacyReferenceBaseline captureLegacyReferences(){return new LegacyReferenceBaseline(importMapper.countLegacyQuestionReferences(),importMapper.countLegacyResourceReferences(),importMapper.countLegacyStateReferences());}
    private void verifyLegacyReferences(LegacyReferenceBaseline baseline){if(baseline.questionReferences()!=importMapper.countLegacyQuestionReferences()||baseline.resourceReferences()!=importMapper.countLegacyResourceReferences()||baseline.stateReferences()!=importMapper.countLegacyStateReferences())throw BizException.of(ErrorCode.CONFLICT,"旧知识点历史引用在导入过程中发生变化");}
    private CourseGraphValidationVO.Counts counts(CourseGraphImport x){CourseGraphValidationVO.Counts c=new CourseGraphValidationVO.Counts();int modules=Objects.requireNonNullElse(x.getModuleCount(),0),categories=Objects.requireNonNullElse(x.getCategoryCount(),0),points=Objects.requireNonNullElse(x.getKnowledgePointCount(),0);c.setCourse(Math.max(0,Objects.requireNonNullElse(x.getNodeCount(),0)-modules-categories-points));c.setModule(modules);c.setCategory(categories);c.setKnowledgePoint(points);c.setContains(Objects.requireNonNullElse(x.getContainsCount(),0));c.setPrerequisite(Objects.requireNonNullElse(x.getPrerequisiteCount(),0));c.setSimilar(Objects.requireNonNullElse(x.getSimilarCount(),0));return c;}
    private record Analysis(CourseGraphJsonParser.ParsedCourseGraph parsed,NormalizedCourseGraph normalized,CourseGraphValidationVO view){}
    private record LegacyReferenceBaseline(int questionReferences,int resourceReferences,int stateReferences){}
    private record LegacySpec(Long id,String type,String target,String mapping,BigDecimal confidence,String notes){}
}
