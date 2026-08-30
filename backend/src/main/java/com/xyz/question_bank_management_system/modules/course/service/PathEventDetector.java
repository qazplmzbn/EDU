package com.xyz.question_bank_management_system.modules.course.service;
import com.xyz.question_bank_management_system.modules.learning.entity.LearningPathProgress;
import java.util.*;
public interface PathEventDetector {Set<String> detect(LearningPathProgress progress,Map<String,Object> event);}
