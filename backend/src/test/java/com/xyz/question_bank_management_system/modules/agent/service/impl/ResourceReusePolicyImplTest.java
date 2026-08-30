package com.xyz.question_bank_management_system.modules.agent.service.impl;
import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class ResourceReusePolicyImplTest {@Test void masteryOnlyReusesAndInvalidationStales(){var p=new ResourceReusePolicyImpl();assertEquals("REUSE",p.decide("PROFILE_VALUE_ONLY",true));assertEquals("GENERATE",p.decide(null,false));assertEquals("REGENERATE",p.decide("CONSECUTIVE_WRONG",true));assertEquals("MARK_STALE",p.decide("CONTENT_INVALIDATED",true));}}
