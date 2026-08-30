package com.xyz.question_bank_management_system.modules.profile.service.impl;
import com.xyz.question_bank_management_system.modules.profile.entity.StudentProfileSnapshot;
import com.xyz.question_bank_management_system.modules.profile.mapper.*;
import com.xyz.question_bank_management_system.modules.profile.service.ProfileQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;
@Service @RequiredArgsConstructor public class ProfileQueryServiceImpl implements ProfileQueryService {
 private final StudentKnowledgeStateMapper stateMapper;private final StudentProfileSnapshotMapper snapshotMapper;private final ProfileV1Mapper profileMapper;
 @Override public Map<String,Object> summary(Long u,Long c){Map<String,Object> m=new LinkedHashMap<>(meta(u,c));m.put("knowledgeStates",stateMapper.selectByUserAndCourse(u,c));m.put("resourcePreferences",profileMapper.preferences(u,c));m.put("cognitiveProfile",profileMapper.cognitive(u,c));m.put("behaviorMetrics",profileMapper.behavior(u,c));return m;}
 @Override public Map<String,Object> knowledgeStates(Long u,Long c,Collection<Long> ids){Map<String,Object> m=new LinkedHashMap<>(meta(u,c));m.put("states",stateMapper.selectByUserAndCourse(u,c).stream().filter(x->ids==null||ids.isEmpty()||ids.contains(x.getKnowledgePointId())).toList());return m;}
 @Override public Map<String,Object> resourcePreferences(Long u,Long c){Map<String,Object> m=new LinkedHashMap<>(meta(u,c));m.put("preferences",profileMapper.preferences(u,c));return m;}
 @Override public Map<String,Object> cognitiveProfile(Long u,Long c){Map<String,Object> m=new LinkedHashMap<>(meta(u,c));m.put("states",profileMapper.cognitive(u,c));return m;}
 private Map<String,Object> meta(Long u,Long c){StudentProfileSnapshot s=snapshotMapper.selectLatest(u,c);Map<String,Object> m=new LinkedHashMap<>();m.put("userId",u);m.put("courseId",c);m.put("profileVersion",s==null?0:s.getProfileVersion());m.put("algorithmVersion",s==null?"profile_v1":Objects.toString(s.getAlgorithmVersion(),"profile_v1"));m.put("asOf",s==null?null:s.getCalculatedAt());return m;}
}
