package com.xyz.question_bank_management_system.modules.agent.service;
import java.util.Map;
public interface ResourceGenerationWorkflow {Map<String,Object> start(Long userId,String pathCode,String pathStepCode,String providerKey,String idempotencyKey);Map<String,Object> status(Long userId,String jobCode,boolean admin);Map<String,Object> bundle(Long userId,String bundleCode,boolean admin);Map<String,Object> regenerate(Long userId,String bundleCode,String providerKey,String idempotencyKey);Map<String,Object> decide(String eventType,boolean hasPublishedBundle);}
