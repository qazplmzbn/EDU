package com.xyz.question_bank_management_system.modules.course.service.impl;
import com.xyz.question_bank_management_system.modules.course.service.PathEventDetector;
import com.xyz.question_bank_management_system.modules.learning.entity.LearningPathProgress;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
@Service public class PathEventDetectorImpl implements PathEventDetector {
 @Override public Set<String> detect(LearningPathProgress p,Map<String,Object> event){Set<String> out=new LinkedHashSet<>();String type=Objects.toString(event==null?null:event.get("type"),"");if(Set.of("RESOURCE_COMPLETED","TARGET_CHANGED","MANUAL_REPLAN","CONTENT_INVALIDATED").contains(type))out.add(type);if(p!=null&&Objects.requireNonNullElse(p.getConsecutiveWrongCount(),0)>=3)out.add("CONSECUTIVE_WRONG");Object gain=event==null?null:event.get("recentMasteryGain");if(p!=null&&Objects.requireNonNullElse(p.getWindowAttemptCount(),0)>=3&&gain!=null&&new BigDecimal(String.valueOf(gain)).compareTo(new BigDecimal("0.01"))<0)out.add("LEARNING_STAGNATION");return out;}
}
