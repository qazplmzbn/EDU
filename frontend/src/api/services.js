import { clean, del, get, patch, post, put, query } from './client'
import { localCheckQuestion, localQuestionTutor } from './attemptFallback'

export const authApi = {
  register(payload) {
    return post('/api/register', payload)
  },
  login(payload) {
    return post('/api/login', payload)
  },
  logout() {
    return post('/api/logout')
  },
  me() {
    return get('/api/auth/me')
  },
}

export const tagApi = {
  list(keyword) {
    return query('/api/tags', { keyword })
  },
  tree() {
    return get('/api/tags/tree')
  },
  create(payload) {
    return post('/api/tags', payload)
  },
  update(tagId, payload) {
    return put(`/api/tags/${tagId}`, payload)
  },
  remove(tagId) {
    return del(`/api/tags/${tagId}`)
  },
}

export const questionApi = {
  search(params) {
    return query('/api/questions', params)
  },
  detail(questionId) {
    return get(`/api/questions/${questionId}`)
  },
  create(payload) {
    return post('/api/questions', payload)
  },
  update(questionId, payload) {
    return put(`/api/questions/${questionId}`, payload)
  },
  remove(questionId) {
    return del(`/api/questions/${questionId}`)
  },
  publish(questionId) {
    return post(`/api/questions/${questionId}/publish`)
  },
  submitBankReview(questionId) {
    return post(`/api/questions/${questionId}/bank-review/submit`)
  },
  cancelBankReview(questionId) {
    return post(`/api/questions/${questionId}/bank-review/cancel`)
  },
  reviewBankQuestion(questionId, payload) {
    return post(`/api/questions/${questionId}/bank-review`, payload)
  },
  generateLlmAnalysis(questionId, payload = {}) {
    return post(`/api/questions/${questionId}/analysis/llm`, payload, { timeout: 8000 })
  },
}

export const paperApi = {
  page(page = 1, size = 20) {
    return get('/api/papers', { page, size })
  },
  detail(paperId) {
    return get(`/api/papers/${paperId}`)
  },
  create(payload) {
    return post('/api/papers', payload)
  },
  update(paperId, payload) {
    return put(`/api/papers/${paperId}`, payload)
  },
  remove(paperId) {
    return del(`/api/papers/${paperId}`)
  },
  addQuestion(paperId, payload) {
    return post(`/api/papers/${paperId}/questions`, payload)
  },
  batchUpdateQuestions(paperId, payload) {
    return put(`/api/papers/${paperId}/questions/batch`, payload)
  },
  updatePaperQuestion(paperQuestionId, payload) {
    return put(`/api/papers/questions/${paperQuestionId}`, payload)
  },
  removePaperQuestion(paperQuestionId) {
    return del(`/api/papers/questions/${paperQuestionId}`)
  },
}

export const assignmentApi = {
  page(page = 1, size = 20, keyword) {
    return query('/api/assignments', { page, size, keyword })
  },
  my(status, page = 1, size = 10) {
    return query('/api/assignments/my', { status, page, size })
  },
  detail(assignmentId) {
    return get(`/api/assignments/${assignmentId}`)
  },
  create(payload) {
    return post('/api/assignments', payload)
  },
  update(assignmentId, payload) {
    return put(`/api/assignments/${assignmentId}`, payload)
  },
  remove(assignmentId) {
    return del(`/api/assignments/${assignmentId}`)
  },
  publish(assignmentId) {
    return post(`/api/assignments/${assignmentId}/publish`)
  },
  close(assignmentId) {
    return post(`/api/assignments/${assignmentId}/close`)
  },
  setTargets(assignmentId, payload) {
    return put(`/api/assignments/${assignmentId}/targets`, payload)
  },
}

export const classApi = {
  create(payload) {
    return post('/api/classes', payload)
  },
  mine() {
    return get('/api/classes/mine')
  },
  update(classId, payload) {
    return put(`/api/classes/${classId}`, payload)
  },
  remove(classId) {
    return del(`/api/classes/${classId}`)
  },
  teacherOptions() {
    return get('/api/classes/teachers')
  },
  students(classId) {
    return get(`/api/classes/${classId}/students`)
  },
  kickStudent(classId, studentId) {
    return del(`/api/classes/${classId}/students/${studentId}`)
  },
  join(classCode) {
    return post('/api/classes/join', { classCode })
  },
  my() {
    return get('/api/classes/my')
  },
}

export const attemptApi = {
  startAssignment(assignmentId) {
    return post(`/api/attempts/assignment/${assignmentId}/start`)
  },
  startPractice(payload) {
    return post('/api/attempts/practice/start', payload)
  },
  questions(attemptId) {
    return get(`/api/attempts/${attemptId}/questions`)
  },
  hint(attemptId, attemptQuestionId, payload) {
    return post(`/api/attempts/${attemptId}/questions/${attemptQuestionId}/hint`, payload, { timeout: 120000 })
  },
  async checkQuestion(attemptId, attemptQuestionId, payload) {
    try {
      return await post(`/api/attempts/${attemptId}/questions/${attemptQuestionId}/check`, payload, { timeout: 120000 })
    } catch (error) {
      console.warn('attemptApi.checkQuestion fallback to local mock:', error)
      return localCheckQuestion(payload)
    }
  },
  async questionTutor(attemptId, attemptQuestionId, payload) {
    try {
      const messages = Array.isArray(payload?.messages) ? payload.messages : []
      const latestMessage = messages[messages.length - 1]?.content || ''
      const result = await post(
        `/api/attempts/${attemptId}/questions/${attemptQuestionId}/hint`,
        {
          question: latestMessage,
          currentAnswer: payload?.studentAnswer || '',
          providerKey: payload?.providerKey || '',
        },
        { timeout: 120000 },
      )
      return {
        reply: result?.hint || '暂时没有生成新的讲解。',
        hints: [],
        sources: Array.isArray(result?.contextSources) ? result.contextSources : [],
        llmCallId: result?.llmCallId,
        source: 'llm',
      }
    } catch (error) {
      console.warn('attemptApi.questionTutor fallback to local mock:', error)
      return localQuestionTutor(payload)
    }
  },
  submit(attemptId) {
    return post(`/api/attempts/${attemptId}/submit`, undefined, { timeout: 8000 })
  },
  result(attemptId) {
    return get(`/api/attempts/${attemptId}/result`)
  },
  my(attemptType, page = 1, size = 20) {
    return query('/api/attempts/my', { attemptType, page, size })
  },
}

export const answerApi = {
  saveDraft(answerId, answerContent) {
    return put(`/api/answers/${answerId}/draft`, { answerContent })
  },
  submit(answerId, answerContent) {
    return put(`/api/answers/${answerId}/submit`, { answerContent })
  },
}

export const statsApi = {
  wrongQuestions(params) {
    return query('/api/stats/wrong-questions', params)
  },
  resolveWrongQuestion(questionId) {
    return post(`/api/stats/wrong-questions/${questionId}/resolve`)
  },
  mastery(tagType = 1) {
    return get('/api/stats/mastery', { tagType })
  },
  ability() {
    return get('/api/stats/ability')
  },
  questionStats(params) {
    return query('/api/stats/question-stats', params)
  },
}

export const appealApi = {
  create(payload) {
    return post('/api/appeals', payload)
  },
  my(params) {
    return query('/api/appeals/my', params)
  },
}

export const teacherApi = {
  reviewAnswers(params) {
    return query('/api/teacher/review/answers', params)
  },
  answerEvidence(answerId) {
    return get(`/api/teacher/answers/${answerId}/evidence`)
  },
  manualGrade(answerId, payload) {
    return post(`/api/teacher/answers/${answerId}/grade`, payload)
  },
  llmRetry(answerId, payload) {
    return post(`/api/teacher/answers/${answerId}/llm-retry`, payload)
  },
  llmBatch(payload) {
    return post('/api/teacher/answers/llm-batch', payload, { timeout: 180000 })
  },
  teacherLlmProviders(params) {
    return query('/api/teacher/llm/providers', params)
  },
  generateAgentResources(payload) {
    return post('/api/teacher/agent-resources/generate', payload, { timeout: 180000 })
  },
  discussAgentResource(payload) {
    return post('/api/teacher/agent-resources/discuss', payload, { timeout: 90000 })
  },
  discussAgentMeeting(payload) {
    return post('/api/teacher/agent-resources/discuss-meeting', payload, { timeout: 180000 })
  },
  startGenerateAgentResourcesTask(payload) {
    return post('/api/teacher/agent-resources/generate-tasks', payload, { timeout: 30000 })
  },
  generateAgentResourcesTaskStatus(taskId) {
    return get(`/api/teacher/agent-resources/generate-tasks/${taskId}`)
  },
  cancelGenerateAgentResourcesTask(taskId) {
    return post(`/api/teacher/agent-resources/generate-tasks/${taskId}/cancel`)
  },
  videoRecommendations(params) {
    return query('/api/resource/video-recommendations', params)
  },
  assignmentScores(assignmentId, page = 1, size = 10) {
    return get(`/api/teacher/assignments/${assignmentId}/scores`, { page, size })
  },
  assignmentTargets(assignmentId, page = 1, size = 10) {
    return get(`/api/teacher/assignments/${assignmentId}/targets`, { page, size })
  },
  assignmentStudentDetail(assignmentId, studentId) {
    return get(`/api/teacher/assignments/${assignmentId}/targets/${studentId}`)
  },
  appeals(params) {
    return query('/api/teacher/appeals', params)
  },
  handleAppeal(appealId, payload) {
    return post(`/api/teacher/appeals/${appealId}/handle`, payload)
  },
}

export const llmApi = {
  studentAssistantChat(payload) {
    return post('/api/student/assistant/chat', payload, { timeout: 120000 })
  },
  modelCenterModels(params) {
    return query('/api/models', params)
  },
  modelCenterModelDetail(id) {
    return get(`/api/models/${id}`)
  },
  createModelCenterModel(payload) {
    return post('/api/models', payload)
  },
  updateModelCenterModel(id, payload) {
    return put(`/api/models/${id}`, payload)
  },
  deleteModelCenterModel(id) {
    return del(`/api/models/${id}`)
  },
  updateModelCenterApiKey(id, apiKey) {
    return put(`/api/models/${id}/api-key`, { apiKey })
  },
  testModelCenterModel(id, payload) {
    return post(`/api/models/${id}/test`, payload)
  },
  callModelCenterModel(id, payload) {
    return post(`/api/models/${id}/call`, payload, { timeout: 120000 })
  },
  modelCenterTemplates(params) {
    return query('/api/models/templates', params)
  },
  modelCenterTemplateDetail(id) {
    return get(`/api/models/templates/${id}`)
  },
  createModelCenterTemplate(payload) {
    return post('/api/models/templates', payload)
  },
  updateModelCenterTemplate(id, payload) {
    return put(`/api/models/templates/${id}`, payload)
  },
  deleteModelCenterTemplate(id) {
    return del(`/api/models/templates/${id}`)
  },
  page(params) {
    return query('/api/llm/calls', params)
  },
  detail(llmCallId) {
    return get(`/api/llm/calls/${llmCallId}`)
  },
  studentProviders(params) {
    return query('/api/student/llm/providers', params)
  },
  createStudentProvider(payload) {
    return post('/api/student/llm/providers', payload)
  },
  updateStudentProvider(id, payload) {
    return put(`/api/student/llm/providers/${id}`, payload)
  },
  setStudentProviderEnabled(id, enabled) {
    return patch(`/api/student/llm/providers/${id}/enabled`, { enabled })
  },
  setStudentProviderDefault(id) {
    return patch(`/api/student/llm/providers/${id}/default`)
  },
  deleteStudentProvider(id) {
    return del(`/api/student/llm/providers/${id}`)
  },
  studentTemplates(params) {
    return query('/api/student/llm/templates', params)
  },
  createStudentTemplate(payload) {
    return post('/api/student/llm/templates', payload)
  },
  updateStudentTemplate(id, payload) {
    return put(`/api/student/llm/templates/${id}`, payload)
  },
  deleteStudentTemplate(id) {
    return del(`/api/student/llm/templates/${id}`)
  },
  async adminProviders(params = {}) {
    const normalized = {
      keyword: params.keyword,
      enabled: params.enabled,
      ...(params.providerType === 'API' ? { category: 'api' } : {}),
    }
    const rows = await query('/api/models', normalized)
    const filtered = params.providerType === 'LOCAL'
      ? rows.filter((item) => item.modelCategory !== 'api')
      : rows
    return filtered.map((item) => ({
      id: item.id,
      providerKey: item.modelCode,
      label: item.modelName,
      providerType: item.modelCategory === 'api' ? 'API' : 'LOCAL',
      baseUrl: item.apiBaseUrl || item.ollamaBaseUrl || item.modelPath || item.modelFilePath || '',
      apiKey: '',
      model: item.apiBaseUrl ? item.modelCode : item.ollamaModelName || item.modelFilePath || item.modelPath || item.modelCode,
      temperature: item.temperature ?? 0.7,
      supportsTemperature: item.modelCategory !== 'detection',
      description: item.description || '',
      tags: Array.isArray(item.supportedDatasets) ? item.supportedDatasets : [],
      enabled: Boolean(item.isAvailable),
      isDefault: Boolean(item.isDefault),
      hasApiKey: Boolean(item.hasApiKey),
      apiKeyMask: item.apiKeyMask || '',
      source: item.source || 'system',
      editable: item.source === 'system',
      deletable: item.source === 'system' && !item.isBuiltin,
      modelCategory: item.modelCategory,
      modelType: item.modelType,
      detectionType: item.detectionType,
      configTemplate: item.configTemplate,
    }))
  },
  createAdminProvider(payload) {
    const providerType = payload.providerType === 'API' ? 'api' : 'local_llm'
    const modelType = providerType === 'api' ? 'api' : 'ollama'
    return post('/api/models', {
      createAsSystem: true,
      modelName: payload.label,
      modelCode: payload.providerKey,
      modelCategory: providerType,
      modelType,
      provider: providerType === 'api' ? inferProviderName(payload.baseUrl, payload.model || payload.providerKey) : 'ollama',
      apiBaseUrl: providerType === 'api' ? payload.baseUrl : '',
      configTemplate: providerType === 'api' ? inferProviderName(payload.baseUrl, payload.model || payload.providerKey) : '',
      apiKey: payload.apiKey || '',
      ollamaModelName: providerType === 'api' ? '' : payload.model,
      ollamaBaseUrl: providerType === 'api' ? '' : payload.baseUrl,
      description: payload.description || '',
      supportedDatasets: splitTags(payload.tags),
      isAvailable: payload.enabled ?? true,
      isDefault: false,
      temperature: payload.supportsTemperature === false ? 0 : payload.temperature ?? 0.7,
      status: payload.enabled === false ? 'inactive' : 'active',
    })
  },
  updateAdminProvider(id, payload) {
    const providerType = payload.providerType === 'API' ? 'api' : 'local_llm'
    const modelType = providerType === 'api' ? 'api' : 'ollama'
    return put(`/api/models/${id}`, {
      modelName: payload.label,
      modelCode: payload.providerKey,
      modelCategory: providerType,
      modelType,
      provider: providerType === 'api' ? inferProviderName(payload.baseUrl, payload.model || payload.providerKey) : 'ollama',
      apiBaseUrl: providerType === 'api' ? payload.baseUrl : '',
      configTemplate: providerType === 'api' ? inferProviderName(payload.baseUrl, payload.model || payload.providerKey) : '',
      apiKey: payload.apiKey || undefined,
      ollamaModelName: providerType === 'api' ? '' : payload.model,
      ollamaBaseUrl: providerType === 'api' ? '' : payload.baseUrl,
      description: payload.description || '',
      supportedDatasets: splitTags(payload.tags),
      temperature: payload.supportsTemperature === false ? 0 : payload.temperature ?? 0.7,
    })
  },
  setAdminProviderEnabled(id, enabled) {
    return put(`/api/models/${id}`, {
      isAvailable: enabled,
      status: enabled ? 'active' : 'inactive',
    })
  },
  setAdminProviderDefault(id) {
    return put(`/api/models/${id}`, { isDefault: true })
  },
  deleteAdminProvider(id) {
    return del(`/api/models/${id}`)
  },
  async adminTemplates(params = {}) {
    const rows = await query('/api/models/templates', {
      keyword: params.keyword,
      task_type: params.taskType,
    })
    return rows.map((item) => ({
      id: item.id,
      templateName: item.name,
      taskType: item.taskType,
      description: item.description || '',
      promptText: item.promptContent,
      createdAt: item.createdAt,
      updatedAt: item.updatedAt,
      editable: item.source === 'system' || item.source === 'personal',
      source: item.source,
    }))
  },
  createAdminTemplate(payload) {
    return post('/api/models/templates', {
      createAsSystem: true,
      name: payload.templateName,
      taskType: payload.taskType,
      description: payload.description || '',
      promptContent: payload.promptText,
    })
  },
  updateAdminTemplate(id, payload) {
    return put(`/api/models/templates/${id}`, {
      name: payload.templateName,
      taskType: payload.taskType,
      description: payload.description || '',
      promptContent: payload.promptText,
    })
  },
  deleteAdminTemplate(id) {
    return del(`/api/models/templates/${id}`)
  },
}

export const landingApi = {
  competencyLayer() {
    return get('/api/landing/competency-layer')
  },
  syncCompetencyJobs() {
    return post('/api/admin/landing/jobs/sync')
  },
  competencySyncRecords(params) {
    return query('/api/admin/landing/jobs/sync-records', params)
  },
}

function splitTags(tags) {
  if (Array.isArray(tags)) return tags.map((item) => String(item).trim()).filter(Boolean)
  return String(tags || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

function inferProviderName(baseUrl, modelCode) {
  const text = `${baseUrl || ''} ${modelCode || ''}`.toLowerCase()
  if (text.includes('dashscope') || text.includes('qwen')) return 'dashscope'
  if (text.includes('deepseek')) return 'deepseek'
  if (text.includes('openai') || text.includes('gpt')) return 'openai'
  if (text.includes('zhipu') || text.includes('glm')) return 'zhipu'
  if (text.includes('moonshot') || text.includes('kimi')) return 'moonshot'
  if (text.includes('ollama')) return 'ollama'
  return 'custom'
}

export const learningApi = {
  knowledgePoints() {
    return get('/api/learning/knowledge-points')
  },
  createKnowledgePoint(payload) {
    return post('/api/learning/knowledge-points', payload)
  },
  updateKnowledgePoint(id, payload) {
    return put(`/api/learning/knowledge-points/${id}`, payload)
  },
  removeKnowledgePoint(id) {
    return del(`/api/learning/knowledge-points/${id}`)
  },
  resources(params) {
    return query('/api/learning/resources', params)
  },
  createResource(payload) {
    return post('/api/learning/resources', payload)
  },
  recommendResourceTargets(id, payload) {
    return post(`/api/learning/resources/${id}/recommend-targets`, payload)
  },
  updateResource(id, payload) {
    return put(`/api/learning/resources/${id}`, payload)
  },
  removeResource(id) {
    return del(`/api/learning/resources/${id}`)
  },
  recordBehavior(payload) {
    return post('/api/learning/behaviors', payload)
  },
  profile() {
    return get('/api/learning/profile')
  },
  profileReport() {
    return get('/api/learning/profile/report')
  },
  extractProfileFeatures(payload) {
    return post('/api/learning/profile/extract-features', payload, { timeout: 20000 })
  },
  recommendations() {
    return get('/api/learning/recommendations')
  },
  pathRecommendation(params) {
    return query('/api/learning/path-recommendation', params)
  },
  savePathRecommendationSnapshot(payload) {
    return post('/api/learning/path-recommendation/snapshots', payload)
  },
  pathRecommendationSnapshot(id) {
    return get(`/api/learning/path-recommendation/snapshots/${id}`)
  },
  pathRecommendationSnapshots(params) {
    return query('/api/learning/path-recommendation/snapshots', params)
  },
  knowledgeRelations() {
    return get('/api/learning/knowledge-relations')
  },
  createKnowledgeRelation(payload) {
    return post('/api/learning/knowledge-relations', payload)
  },
  updateKnowledgeRelation(id, payload) {
    return put(`/api/learning/knowledge-relations/${id}`, payload)
  },
  extractKnowledgeGraph(payload) {
    return post('/api/learning/knowledge-graph/extract', payload, { timeout: 120000 })
  },
  extractKnowledgeGraphFile(file, payload = {}) {
    const formData = new FormData()
    formData.append('file', file)
    Object.entries(clean(payload)).forEach(([key, value]) => {
      formData.append(key, value)
    })
    return post('/api/learning/knowledge-graph/extract-file', formData, { timeout: 120000 })
  },
  removeKnowledgeRelation(id) {
    return del(`/api/learning/knowledge-relations/${id}`)
  },
  personalizedPracticePlan(params) {
    return query('/api/learning/personalized-practice/plan', params)
  },
  startPersonalizedPractice(payload) {
    return post('/api/learning/personalized-practice/start', payload)
  },
}

export const stageEvaluationApi = {
  my(params) {
    return query('/api/stage-evaluations/my', params)
  },
  teacherStudents(params) {
    return query('/api/stage-evaluations/teacher/students', params)
  },
}

// ===== 职业画像与岗位匹配（学生端六维度画像"岗位匹配度"） =====
export const careerApi = {
  occupations(params) {
    return query('/api/competency/occupations', params)
  },
  gaps(occupationId) {
    return query('/api/competency/career/gaps', occupationId == null ? {} : { occupationId })
  },
  report(occupationId) {
    return query('/api/competency/career/report', occupationId == null ? {} : { occupationId })
  },
  diagnosticTargets(occupationId) {
    return query('/api/competency/career/diagnostic-targets', occupationId == null ? {} : { occupationId })
  },
}

// ===== Agent 任务（教师端个性化路径生成过程 trace） =====
export const agentTaskApi = {
  detail(taskCode) {
    return get(`/api/agent-tasks/${taskCode}`)
  },
  trace(taskCode) {
    return get(`/api/agent-tasks/${taskCode}/trace`)
  },
}

export const adminApi = {
  pageUsers(page = 1, size = 20) {
    return get('/api/admin/users', { page, size })
  },
  createUser(payload) {
    return post('/api/admin/users', payload)
  },
  updateUser(userId, payload) {
    return put(`/api/admin/users/${userId}`, payload)
  },
  updateUserRole(userId, role) {
    return put(`/api/admin/users/${userId}/role`, { role })
  },
  loginLogs(params) {
    return query('/api/admin/login-logs', params)
  },
  auditLogs(params) {
    return query('/api/admin/audit-logs', params)
  },
}


