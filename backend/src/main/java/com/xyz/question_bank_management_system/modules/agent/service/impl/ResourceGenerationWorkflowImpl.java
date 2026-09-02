package com.xyz.question_bank_management_system.modules.agent.service.impl;

import com.fasterxml.jackson.databind.*;
import com.xyz.question_bank_management_system.config.CorrelationContext;
import com.xyz.question_bank_management_system.exception.*;
import com.xyz.question_bank_management_system.modules.agent.entity.*;
import com.xyz.question_bank_management_system.modules.agent.entity.ResourceBundle;
import com.xyz.question_bank_management_system.modules.agent.mapper.PersonalizedResourceMapper;
import com.xyz.question_bank_management_system.modules.agent.mapper.AgentReviewMapper;
import com.xyz.question_bank_management_system.modules.agent.service.*;
import com.xyz.question_bank_management_system.modules.agent.tool.*;
import com.xyz.question_bank_management_system.modules.learning.entity.*;
import com.xyz.question_bank_management_system.modules.learning.mapper.LearningPathV1Mapper;
import com.xyz.question_bank_management_system.modules.llm.entity.QbLlmCall;
import com.xyz.question_bank_management_system.modules.llm.service.LlmService;
import com.xyz.question_bank_management_system.modules.source.entity.ResourceSource;
import com.xyz.question_bank_management_system.modules.source.mapper.EvidenceLinkMapper;
import com.xyz.question_bank_management_system.util.HashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ResourceGenerationWorkflowImpl implements ResourceGenerationWorkflow {
    private static final List<String> EXPERTS = List.of("PROFILE_ADAPTATION","KNOWLEDGE_FIDELITY","PEDAGOGICAL_PATH_ALIGNMENT","QUESTION_COGNITIVE_ALIGNMENT","HIDDEN_ASSESSMENT_INDEPENDENCE");
    private final PersonalizedResourceMapper mapper;
    private final AgentReviewMapper agentReviewMapper;
    private final EvidenceLinkMapper evidenceLinkMapper;
    private final LearningPathV1Mapper pathMapper;
    private final ResourceUnitService unitService;
    private final ResourceBlueprintService blueprintService;
    private final ResourceReusePolicy reusePolicy;
    private final CourseRagSearchTool ragTool;
    private final QuestionSimilaritySearchTool similarityTool;
    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Map<String, Object> start(Long userId, String pathCode, String pathStepCode, String providerKey, String key) {
        if (!StringUtils.hasText(key)) throw BizException.of(ErrorCode.PARAM_ERROR,"Idempotency-Key 不能为空");
        ResourceGenerationJob old = mapper.selectJobByIdempotency(userId,key);
        if (old != null) return jobView(old);
        LearningPath path = pathMapper.selectByCode(pathCode);
        if (path == null || !Objects.equals(path.getUserId(),userId)) throw BizException.of(ErrorCode.NOT_FOUND,"学习路径不存在");
        LearningPathVersion pathVersion = pathMapper.selectActiveVersion(path.getId());
        ResourceUnit unit = unitService.aggregate(pathCode,pathStepCode);
        ResourceBlueprint blueprint = blueprintService.design(userId,unit);
        ResourceGenerationJob job = new ResourceGenerationJob();
        job.setJobCode("job_"+UUID.randomUUID().toString().replace("-",""));job.setUserId(userId);job.setCourseId(path.getCourseId());job.setPathVersionId(pathVersion.getId());job.setResourceUnitId(unit.getId());job.setBlueprintId(blueprint.getId());job.setGenerationPolicyVersion("resource_policy_v1");job.setIdempotencyKey(key);job.setStatus("BLUEPRINT_READY");job.setMaxRevisionRounds(5);job.setInputSnapshotJson(snapshot(unit,blueprint));job.setCorrelationId(CorrelationContext.get());mapper.insertJob(job);
        try { run(job,unit,blueprint,pathVersion,providerKey); }
        catch (RuntimeException ex) { job.setStatus("FAILED");job.setErrorCode(ex instanceof BizException b?b.getCode():ErrorCode.SYSTEM_ERROR);job.setErrorMessage(shortError(ex));mapper.updateJob(job);return jobView(job); }
        return jobView(job);
    }

    private void run(ResourceGenerationJob job,ResourceUnit unit,ResourceBlueprint blueprint,LearningPathVersion pathVersion,String providerKey) {
        List<Long> knowledgeIds = unitKnowledge(unit);
        List<Map<String,Object>> evidence = ragTool.query(knowledgeIds);
        String repairs = "";
        for (int round=1; round<=job.getMaxRevisionRounds(); round++) {
            job.setStatus(round==1?"GENERATING":"REPAIRING");mapper.updateJob(job);
            JsonNode generated = generate(job.getUserId(),blueprint,evidence,repairs,providerKey);
            List<GeneratedItem> items = parseItems(generated,knowledgeIds);
            List<ResourceReview> reviews = supervise(job,blueprint,items,knowledgeIds,round,providerKey,generated);
            if (missingRequiredQuestions(items)) reviews.add(requiredQuestionReview(job, blueprint, round));
            boolean pass = reviews.stream().allMatch(x->"PASS".equals(x.getResult()));
            if (pass) {
                ResourceBundle bundle = persistBundle(job,unit,blueprint,pathVersion,items,reviews,generated);
                job.setStatus("PUBLISHED");mapper.updateJob(job);return;
            }
            for (ResourceReview review : reviews) persistAgentReview(review, null, blueprint, round);
            repairs = reviews.stream().filter(x->"REVISE".equals(x.getResult())).map(ResourceReview::getRepairInstruction).filter(Objects::nonNull).reduce("",(a,b)->a+"\n- "+b);
        }
        throw BizException.of(ErrorCode.LLM_ERROR,"资源经过 5 轮修复仍未通过完整监督");
    }

    private JsonNode generate(Long userId,ResourceBlueprint blueprint,List<Map<String,Object>> evidence,String repairs,String providerKey) {
        String prompt = "你是资源生成智能体。只能执行给定蓝图，不得重新查询画像或改变资源类型、知识范围、难度与认知层级。每个事实必须含 evidenceIds。只输出 {\"items\":[...]} 严格 JSON。\n蓝图="+snapshot(null,blueprint)+"\n证据="+json(evidence)+"\n修复要求="+repairs;
        prompt += "\nMandatory output: include at least one TEACHING_RESOURCE, one VISIBLE LEARNING_PRACTICE question, and one HIDDEN_UNTIL_ASSESSMENT KNOWLEDGE_ASSESSMENT question. Every item needs knowledgePointIds and evidenceIds; both questions need questionDifficulty, cognitiveLevel, and gradingKey.";
        QbLlmCall call=llmService.chatCompletion(4,userId,prompt,providerKey,userId);String content=normalizeJsonPayload(llmService.extractContent(call.getResponseText()));
        if(!StringUtils.hasText(content))throw BizException.of(ErrorCode.LLM_ERROR,"Generator 未返回结构化内容");
        try{JsonNode root=objectMapper.readTree(content);if(!root.path("items").isArray()||root.path("items").isEmpty())throw new IllegalArgumentException("items 为空");return root;}catch(Exception ex){throw BizException.of(ErrorCode.LLM_ERROR,"GeneratedResourceBundle 不符合 JSON 契约");}
    }

    private String normalizeJsonPayload(String raw){if(raw==null)return null;String value=raw.trim();if(value.startsWith("```")){int first=value.indexOf('\n');int last=value.lastIndexOf("```");value=(first<0?value.substring(3):value.substring(first+1,last<0?value.length():last)).trim();}int start=value.indexOf('{'),end=value.lastIndexOf('}');if(start>=0&&end>start)value=value.substring(start,end+1);StringBuilder out=new StringBuilder();boolean quoted=false,escaped=false;for(int i=0;i<value.length();i++){char c=value.charAt(i);if(escaped){out.append(c);escaped=false;continue;}if(c=='\\'){out.append(c);escaped=true;continue;}if(c=='\"'){quoted=!quoted;out.append(c);continue;}if(quoted&&c=='\n'){out.append("\\n");continue;}if(quoted&&c=='\r'){out.append("\\r");continue;}if(quoted&&c=='\t'){out.append("\\t");continue;}out.append(c);}return out.toString();}
    private List<GeneratedItem> parseItems(JsonNode root,List<Long> allowed) {
        List<GeneratedItem> out=new ArrayList<>();int order=0;
        for(JsonNode n:root.path("items")){String itemCode=n.path("itemCode").asText("item_"+(++order));String purpose=n.path("purpose").asText("TEACHING_RESOURCE");String visibility=n.path("visibility").asText("VISIBLE");List<Long> ids=new ArrayList<>();n.path("knowledgePointIds").forEach(x->ids.add(x.asLong()));if(ids.isEmpty()||!allowed.containsAll(ids))throw BizException.of(ErrorCode.LLM_ERROR,"Generator 输出了 Blueprint 范围外知识点");ResourceItem item=new ResourceItem();item.setItemCode(itemCode);item.setItemType(n.path("itemType").asText("concept_explanation"));item.setPurpose(purpose);item.setVisibility(visibility);item.setTitle(n.path("title").asText(itemCode));item.setContentJson(n.path("content").isMissingNode()?n.toString():n.path("content").toString());if(purpose.contains("PRACTICE")||purpose.contains("ASSESSMENT")){item.setGeneratedQuestionCode("pq_"+UUID.randomUUID().toString().replace("-",""));item.setQuestionDifficulty(decimal(n.path("questionDifficulty"),new BigDecimal("0.50")));item.setCognitiveLevel(n.path("cognitiveLevel").asText());item.setGradingKeyJson(n.path("gradingKey").isMissingNode()?null:n.path("gradingKey").toString());}item.setOrderNo(++order);item.setStatus("ACTIVE");item.setNormalizedTextHash(HashUtil.sha256(item.getContentJson()));item.setSimhash64((long)item.getContentJson().hashCode());List<Long> evidenceIds=new ArrayList<>();n.path("evidenceIds").forEach(x->evidenceIds.add(x.asLong()));out.add(new GeneratedItem(item,ids,evidenceIds,n));}
        return out;
    }

    private List<ResourceReview> supervise(ResourceGenerationJob job,ResourceBlueprint blueprint,List<GeneratedItem> items,List<Long> allowed,int round,String providerKey,JsonNode generated) {
        List<ResourceReview> out=new ArrayList<>();
        for(String role:EXPERTS){ResourceReview deterministic=deterministicReview(role,job,blueprint,items,allowed,round);String prompt="你是个性化学习资源监督专家，expertRole="+role+"。只评价该维度。只输出 {\"expertRole\":\""+role+"\",\"result\":\"PASS|REVISE\",\"issues\":[]} JSON。输入="+generated;QbLlmCall call=llmService.chatCompletion(4,job.getId(),prompt,providerKey,job.getUserId());String content=llmService.extractContent(call.getResponseText());if(!StringUtils.hasText(content)){markRevise(deterministic,"EXPERT_OUTPUT_INVALID","监督专家未返回有效 JSON");}else{try{JsonNode report=objectMapper.readTree(content);if(!role.equals(report.path("expertRole").asText())||!Set.of("PASS","REVISE").contains(report.path("result").asText()))markRevise(deterministic,"EXPERT_OUTPUT_INVALID","监督专家输出契约不合法");else if("REVISE".equals(report.path("result").asText())){markRevise(deterministic,"EXPERT_REVISE",report.path("issues").toString());deterministic.setReportJson(report.toString());}}catch(Exception ex){markRevise(deterministic,"EXPERT_OUTPUT_INVALID","监督专家输出无法解析");}}
            out.add(deterministic);}
        return out;
    }

    private ResourceReview deterministicReview(String role,ResourceGenerationJob job,ResourceBlueprint bp,List<GeneratedItem> items,List<Long> allowed,int round){ResourceReview r=new ResourceReview();r.setJobId(job.getId());r.setBlueprintId(bp.getId());r.setExpertRole(role);r.setResult("PASS");r.setCritical(0);r.setRoundNo(round);switch(role){case "PROFILE_ADAPTATION"->{if(!StringUtils.hasText(bp.getProfileEvidenceJson()))markRevise(r,"PROFILE_EVIDENCE_MISSING","蓝图缺少画像证据快照");}case "KNOWLEDGE_FIDELITY"->{if(items.stream().anyMatch(x->x.evidenceIds().isEmpty()))markRevise(r,"FACT_UNSUPPORTED","存在未绑定 evidenceIds 的资源项");}case "PEDAGOGICAL_PATH_ALIGNMENT"->{if(items.stream().anyMatch(x->!allowed.containsAll(x.knowledgeIds())))markRevise(r,"OUT_OF_SCOPE_KNOWLEDGE","资源超出 ResourceUnit");}case "QUESTION_COGNITIVE_ALIGNMENT"->{if(items.stream().map(GeneratedItem::item).filter(x->x.getGeneratedQuestionCode()!=null).anyMatch(x->x.getQuestionDifficulty()==null||!StringUtils.hasText(x.getCognitiveLevel())||!StringUtils.hasText(x.getGradingKeyJson())))markRevise(r,"QUESTION_CONTRACT_INVALID","题目缺少冻结难度、认知层级或 grading key");}case "HIDDEN_ASSESSMENT_INDEPENDENCE"->{for(GeneratedItem x:items)if("HIDDEN_UNTIL_ASSESSMENT".equals(x.item().getVisibility())&&similarityTool.query(x.item().getContentJson(),0L).exceedsThreshold()){markRevise(r,"HIDDEN_DUPLICATE","隐藏题与已有内容实质重复");break;}}}return r;}
    private boolean missingRequiredQuestions(List<GeneratedItem> items){boolean visible=items.stream().anyMatch(x->"LEARNING_PRACTICE".equals(x.item().getPurpose())&&"VISIBLE".equals(x.item().getVisibility())&&x.item().getGeneratedQuestionCode()!=null);boolean hidden=items.stream().anyMatch(x->"KNOWLEDGE_ASSESSMENT".equals(x.item().getPurpose())&&"HIDDEN_UNTIL_ASSESSMENT".equals(x.item().getVisibility())&&x.item().getGeneratedQuestionCode()!=null);return !visible||!hidden;}
    private ResourceReview requiredQuestionReview(ResourceGenerationJob job,ResourceBlueprint blueprint,int round){ResourceReview review=new ResourceReview();review.setJobId(job.getId());review.setBlueprintId(blueprint.getId());review.setExpertRole("QUESTION_COGNITIVE_ALIGNMENT");review.setRoundNo(round);markRevise(review,"QUESTION_CONTRACT_INVALID","资源包必须同时包含可见 LEARNING_PRACTICE 与隐藏 KNOWLEDGE_ASSESSMENT 题目");return review;}
    private void markRevise(ResourceReview r,String type,String description){r.setResult("REVISE");r.setIssueType(type);r.setLocation("bundle");r.setDescription(description);r.setRepairTarget(type.startsWith("PROFILE")?"RESOURCE_BLUEPRINT_AGENT":"RESOURCE_GENERATION_AGENT");r.setRepairScope("CURRENT_BUNDLE");r.setRepairAction("REGENERATE_AFFECTED_CONTENT");r.setRepairInstruction(description);r.setCritical(1);}

    @Transactional protected ResourceBundle persistBundle(ResourceGenerationJob job,ResourceUnit unit,ResourceBlueprint bp,LearningPathVersion pv,List<GeneratedItem> generated,List<ResourceReview> reviews,JsonNode root){ResourceBundle previous=mapper.selectLatestBundle(unit.getId());ResourceBundle b=new ResourceBundle();b.setBundleCode("bundle_"+UUID.randomUUID().toString().replace("-",""));b.setUserId(job.getUserId());b.setCourseId(job.getCourseId());b.setResourceUnitId(unit.getId());b.setBlueprintId(bp.getId());b.setVersion(previous==null?1:previous.getVersion()+1);b.setStatus("IN_REVIEW");b.setProfileVersionUsed(bp.getProfileVersionUsed());b.setGraphVersion(pv.getGraphVersion());b.setPolicyVersion(bp.getPolicyVersion());b.setCorrelationId(CorrelationContext.get());mapper.insertBundle(b);List<ResourceItem> items=generated.stream().map(GeneratedItem::item).toList();items.forEach(x->x.setBundleId(b.getId()));mapper.batchInsertItems(items);List<ResourceItem> stored=mapper.selectAllItems(b.getId());for(int i=0;i<stored.size();i++){GeneratedItem meta=generated.get(i);ResourceItem item=stored.get(i);BigDecimal weight=BigDecimal.ONE.divide(BigDecimal.valueOf(meta.knowledgeIds().size()),4,RoundingMode.HALF_UP);for(int k=0;k<meta.knowledgeIds().size();k++)mapper.insertItemKnowledge(item.getId(),meta.knowledgeIds().get(k),item.getGeneratedQuestionCode()==null?"COVERAGE":"DIRECT_ASSESSMENT",weight,k==0?1:0);int order=0;for(Long chunkId:meta.evidenceIds()){ResourceSource source=new ResourceSource();source.setResourceId(item.getId());source.setSourceChunkId(chunkId);source.setSupportType("grounding");source.setRelevanceScore(BigDecimal.ONE);source.setCitationOrder(++order);evidenceLinkMapper.upsertResource(source);}}String hash=HashUtil.sha256(root.toString());if(mapper.publishBundle(b.getId(),hash)!=1)throw BizException.of(ErrorCode.CONFLICT,"资源发布状态已变化");b.setContentHash(hash);b.setStatus("PUBLISHED");if(previous!=null&&"PUBLISHED".equals(previous.getStatus()))mapper.updateBundleStatus(previous.getId(),"SUPERSEDED","NEW_VERSION_PUBLISHED");for(ResourceReview r:reviews)persistAgentReview(r,b,bp,r.getRoundNo());return b;}

    private void persistAgentReview(ResourceReview review,ResourceBundle bundle,ResourceBlueprint blueprint,int round){AgentReview audit=new AgentReview();audit.setTargetType("PERSONALIZED_RESOURCE");audit.setTargetId(bundle==null?null:bundle.getId());audit.setReviewStatus(review.getResult());audit.setReviewReport(review.getReportJson());audit.setBundleId(bundle==null?null:bundle.getId());audit.setBlueprintId(blueprint.getId());audit.setReviewDimension(review.getExpertRole());audit.setIssueCode(review.getIssueType());audit.setRepairTarget(review.getRepairTarget());audit.setRepairScope(review.getRepairScope());audit.setRepairAction(review.getRepairAction());audit.setRepairInstruction(review.getRepairInstruction());audit.setRoundNo(round);audit.setEvidenceRefsJson("[]");audit.setToolResultJson(review.getReportJson());audit.setReviewerRole(review.getExpertRole());agentReviewMapper.insertResourceAudit(audit);}

    @Override public Map<String,Object> status(Long userId,String code,boolean admin){ResourceGenerationJob j=mapper.selectJobByCode(code);if(j==null||(!admin&&!Objects.equals(j.getUserId(),userId)))throw BizException.of(ErrorCode.NOT_FOUND,"任务不存在");return jobView(j);}
    @Override public Map<String,Object> bundle(Long userId,String code,boolean admin){ResourceBundle b=mapper.selectBundleByCode(code);if(b==null||(!admin&&!Objects.equals(b.getUserId(),userId)))throw BizException.of(ErrorCode.NOT_FOUND,"资源不存在");if(!admin&&!"PUBLISHED".equals(b.getStatus()))throw BizException.of(ErrorCode.FORBIDDEN,"资源尚未发布");Map<String,Object> m=new LinkedHashMap<>();m.put("bundleCode",b.getBundleCode());m.put("version",b.getVersion());m.put("status",b.getStatus());m.put("items",admin?mapper.selectAllItems(b.getId()):com.xyz.question_bank_management_system.modules.agent.dto.StudentResourceItemView.of(mapper.selectVisibleItems(b.getId())));return m;}
    @Override @Transactional public Map<String,Object> regenerate(Long userId,String code,String provider,String key){if(!StringUtils.hasText(key))throw BizException.of(ErrorCode.PARAM_ERROR,"Idempotency-Key 不能为空");ResourceGenerationJob old=mapper.selectJobByIdempotency(userId,key);if(old!=null)return jobView(old);ResourceBundle b=mapper.selectBundleByCode(code);if(b==null||!Objects.equals(b.getUserId(),userId))throw BizException.of(ErrorCode.NOT_FOUND,"资源不存在");ResourceUnit unit=mapper.selectUnitById(b.getResourceUnitId());if(unit==null)throw BizException.of(ErrorCode.NOT_FOUND,"ResourceUnit 不存在");LearningPath path=pathMapper.selectPathById(unit.getPathId());LearningPathVersion pathVersion=pathMapper.selectActiveVersion(path.getId());ResourceBlueprint blueprint=blueprintService.design(userId,unit);ResourceGenerationJob job=new ResourceGenerationJob();job.setJobCode("job_"+UUID.randomUUID().toString().replace("-",""));job.setUserId(userId);job.setCourseId(path.getCourseId());job.setPathVersionId(pathVersion.getId());job.setResourceUnitId(unit.getId());job.setBlueprintId(blueprint.getId());job.setGenerationPolicyVersion("resource_policy_v1");job.setIdempotencyKey(key);job.setStatus("BLUEPRINT_READY");job.setMaxRevisionRounds(5);job.setInputSnapshotJson(snapshot(unit,blueprint));job.setCorrelationId(CorrelationContext.get());mapper.insertJob(job);try{run(job,unit,blueprint,pathVersion,provider);}catch(RuntimeException ex){job.setStatus("FAILED");job.setErrorCode(ex instanceof BizException x?x.getCode():ErrorCode.SYSTEM_ERROR);job.setErrorMessage(ex.getMessage());mapper.updateJob(job);return jobView(job);}return jobView(job);}
    @Override public Map<String,Object> decide(String event,boolean published){return Map.of("resourceAction",reusePolicy.decide(event,published),"reason",event==null?"PROFILE_VALUE_ONLY":event);}
    private String shortError(Throwable ex){String value=Objects.toString(ex.getMessage(),ex.getClass().getSimpleName());return value.length()>900?value.substring(0,900):value;}
    private List<Long> unitKnowledge(ResourceUnit unit){Set<Long> stepIds=mapper.selectUnitSteps(unit.getId()).stream().map(ResourceUnitStep::getPathStepId).collect(java.util.stream.Collectors.toSet());return pathMapper.selectSteps(unit.getPathVersionId()).stream().filter(x->stepIds.contains(x.getId())).sorted(Comparator.comparing(LearningPathItem::getOrderNo)).map(LearningPathItem::getKnowledgePointId).toList();}
    private Map<String,Object> jobView(ResourceGenerationJob j){Map<String,Object> m=new LinkedHashMap<>();m.put("jobCode",j.getJobCode());m.put("status",j.getStatus());m.put("resourceUnitId",j.getResourceUnitId());m.put("blueprintId",j.getBlueprintId());m.put("errorCode",j.getErrorCode());m.put("errorMessage",j.getErrorMessage());if(j.getResourceUnitId()!=null){ResourceBundle b=mapper.selectLatestBundle(j.getResourceUnitId());if(b!=null)m.put("bundleCode",b.getBundleCode());}return m;}
    private String snapshot(ResourceUnit unit,ResourceBlueprint bp){Map<String,Object> m=new LinkedHashMap<>();if(unit!=null)m.put("resourceUnit",unit);m.put("blueprint",bp);return json(m);}private String json(Object v){try{return objectMapper.writeValueAsString(v);}catch(Exception e){throw new IllegalStateException(e);}}private BigDecimal decimal(JsonNode n,BigDecimal d){try{return n.isNumber()?n.decimalValue():new BigDecimal(n.asText());}catch(Exception e){return d;}}
    private record GeneratedItem(ResourceItem item,List<Long> knowledgeIds,List<Long> evidenceIds,JsonNode raw){}
}
