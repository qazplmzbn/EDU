package com.xyz.question_bank_management_system.modules.profile.service.impl;

import com.fasterxml.jackson.databind.*;
import com.xyz.question_bank_management_system.modules.profile.entity.StudentKnowledgeModelState;
import com.xyz.question_bank_management_system.modules.profile.mapper.DimktStateMapper;
import com.xyz.question_bank_management_system.modules.profile.model.ValidatedInteraction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

@Slf4j @Component @RequiredArgsConstructor
public class DimktClient {
    private final DimktStateMapper stateMapper;
    private final ObjectMapper objectMapper;
    @Value("${app.dimkt.enabled:false}") private boolean enabled;
    @Value("${app.dimkt.base-url:http://localhost:8000}") private String baseUrl;
    @Value("${app.dimkt.model-version:dimkt_v1}") private String modelVersion;
    @Value("${app.dimkt.knowledge-index-version:knowledge_index_v1}") private String indexVersion;
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    public Optional<Map<Long,BigDecimal>> infer(ValidatedInteraction i) {
        if(!enabled) return Optional.empty();
        try {
            StudentKnowledgeModelState state=stateMapper.select(i.getUserId(),i.getCourseId());
            Map<String,BigDecimal> weights=new LinkedHashMap<>(); i.getKnowledgeWeights().forEach((k,v)->weights.put(String.valueOf(k),v));
            Map<String,Object> event=new LinkedHashMap<>(); event.put("interactionSeq",i.getInteractionSeq()); event.put("knowledgeWeights",weights); event.put("scoreNormalized",i.getScoreNormalized()); event.put("questionDifficulty",i.getQuestionDifficulty()); event.put("questionPurpose",i.getQuestionPurpose()); event.put("cognitiveLevel",null);
            Map<String,Object> payload=new LinkedHashMap<>(); payload.put("userId",i.getUserId()); payload.put("courseId",i.getCourseId()); payload.put("modelVersion",modelVersion); payload.put("knowledgeIndexVersion",indexVersion); payload.put("previousStateRef",state==null?null:state.getStateRef()); payload.put("interactions",List.of(event));
            HttpRequest req=HttpRequest.newBuilder(URI.create(baseUrl+"/internal/v1/dimkt/infer")).timeout(Duration.ofSeconds(8)).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload))).build();
            HttpResponse<String> res=client.send(req,HttpResponse.BodyHandlers.ofString()); if(res.statusCode()/100!=2)return Optional.empty(); JsonNode data=objectMapper.readTree(res.body()).path("data");
            Map<Long,BigDecimal> mastery=new LinkedHashMap<>(); data.path("masteryHead").fields().forEachRemaining(e->mastery.put(Long.valueOf(e.getKey()),e.getValue().decimalValue()));
            StudentKnowledgeModelState next=new StudentKnowledgeModelState(); next.setUserId(i.getUserId()); next.setCourseId(i.getCourseId()); next.setModelVersion(data.path("modelVersion").asText(modelVersion)); next.setKnowledgeIndexVersion(data.path("knowledgeIndexVersion").asText(indexVersion)); next.setStateRef(data.path("stateRef").asText()); next.setProcessedThroughSeq(data.path("processedThroughSeq").asLong()); next.setStatus("ACTIVE"); stateMapper.upsert(next); return Optional.of(mastery);
        } catch(Exception ex) { log.warn("DIMKT inference failed; using weighted rule baseline: {}",ex.getMessage()); return Optional.empty(); }
    }
}
