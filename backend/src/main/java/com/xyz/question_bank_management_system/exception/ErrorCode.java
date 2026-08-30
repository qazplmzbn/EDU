package com.xyz.question_bank_management_system.exception;

/**
 * 业务错误码（可按需扩展）。
 */
public interface ErrorCode {
    String PARAM_ERROR = "PARAM_ERROR";
    String UNAUTHORIZED = "UNAUTHORIZED";
    String FORBIDDEN = "FORBIDDEN";
    String NOT_FOUND = "NOT_FOUND";
    String CONFLICT = "CONFLICT";
    String BIZ_ERROR = "BIZ_ERROR";
    String SYSTEM_ERROR = "SYSTEM_ERROR";
    String LLM_ERROR = "LLM_ERROR";
    String KNOWLEDGE_GRAPH_UNAVAILABLE = "KNOWLEDGE_GRAPH_UNAVAILABLE";
    String PROFILE_EVIDENCE_MISSING = "PROFILE_EVIDENCE_MISSING";
    String EVIDENCE_INSUFFICIENT = "EVIDENCE_INSUFFICIENT";
    String EVIDENCE_CONFLICT = "EVIDENCE_CONFLICT";
    String FAILED_INPUT = "FAILED_INPUT";
}
