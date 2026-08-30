<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Delete,
  DocumentChecked,
  Finished,
  SwitchButton,
  Refresh,
  Search,
  Setting,
  UserFilled,
} from '@element-plus/icons-vue'
import { gsap } from 'gsap'
import { classApi, learningApi, stageEvaluationApi, teacherApi } from '@/api/services'
import {
  agentInfoCards,
  agentProviderFieldMap,
  agentRunSequences,
  agents,
  backendAgentLabels,
  defaultAgentPositions,
  defaultResourceTypes,
  demoResources,
  demoStudent,
  editableAgentIds,
  edgesBase,
  stageOptions,
  videoResourceTypes,
} from '@/data/teacherAgentResource'
import {
  persistActivityLogs,
  prependActivityLog,
  readActivityLogs,
  removeActivityLog,
} from '@/features/teacher-agent-resource/activityLogs'
import {
  buildAgentProviderPayload,
  currentAgentProviderLabel as buildCurrentAgentProviderLabel,
  providerBindingMode as buildProviderBindingMode,
  providerSummaryText as buildProviderSummaryText,
  traceSummary as buildTraceSummary,
} from '@/features/teacher-agent-resource/providerConfig'
import {
  buildRemotionProps,
  buildFallbackGeneratedResources,
  buildRecommendTargetPayload,
  buildResourceSaveConfirm,
  buildResourceSavePayload,
  buildRevisionFeedback,
  buildVideoCover,
  canSaveGeneratedResource,
  ensureVideoResource,
  externalVideoLinks,
  hasConcreteVideo,
  hasExternalVideoResource,
  isConcreteVideoUrl,
  isQuizAnswerCorrect,
  isVideoSearchUrl,
  formatDimensionNames as formatResourceDimensionNames,
  formatWeakPointNames,
  normalizeGeneratedCards,
  normalizeAnswer,
  primaryVideo,
  quizDisplayQuestions,
  quizFeedbackText,
  remotionPropsFilename,
  safeFileName,
  validateResourceBeforeSave,
  videoSourceStatus,
} from '@/features/teacher-agent-resource/resourceCards'
import {
  AGENT_WORKSPACE_STORAGE_KEY,
  TASK_STORAGE_KEY,
  buildTaskSnapshot,
  buildWorkspaceSnapshot,
  readStorageJson,
  removeStorageItem,
  writeStorageJson,
} from '@/features/teacher-agent-resource/workspaceStorage'
import {
  agentNodeStatusClass,
  agentStatusText,
  buildDynamicEdges,
  edgeClassState,
  isAgentReady,
  isPendingAgent,
  isReceivingAgent,
} from '@/features/teacher-agent-resource/workflow'
import WeakPointsKnowledgeGraph from '@/components/teacher-agent/WeakPointsKnowledgeGraph.vue'
import { cLanguageKnowledgeGraph } from '@/data/cLanguageKnowledgeGraph'

const rootRef = ref(null)
const loading = ref(false)
const savingId = ref('')
const sendingId = ref('')
const stage = ref('month')
const studentId = ref('')
const selectedStudentId = ref('')
const students = ref([])
const teacherClasses = ref([])
const candidateResources = ref([])
const generatedResources = ref([])
const lastRun = ref(null)
const teacherRequirement = ref('')
const generationConfig = reactive({
  generationScope: 'student',
  classId: undefined,
  difficulty: 'improve',
  exerciseCount: 5,
  publishMode: 'draft_only',
  selectedWeakPoints: [],
  selectedResourceTypes: ['knowledge_video', 'remedial_exercise', 'knowledge_handout', 'error_reflection', 'learning_path'],
})
const revisionDraft = ref(null)
const revisionFeedback = ref('')
const completedAgents = ref([])
const activeAgentId = ref('')
const flowingEdges = ref([])
const logs = ref([])
const providerLoading = ref(false)
const providerOptions = ref([])
const providerDrawerVisible = ref(false)
const defaultProviderKey = ref('')
const agentOverrides = reactive({
  preprocess: '',
  coordinator: '',
  knowledge: '',
  ability: '',
  behavior: '',
  resource: '',
  practice: '',
  report: '',
  qualityReview: '',
  consistencyReview: '',
  generator: '',
  qualityReviewer: '',
  consistencyReviewer: '',
})
const generationTaskId = ref('')
const generationTaskStatus = ref('idle')
const generationTaskMessage = ref('')
const executionStopped = ref(false)
const activeConsultationTab = ref('discussion')
const liveConsultationMessages = ref([])
const pendingConsultationMessages = ref([])
const consultationLoadingAgentId = ref('')
const meetingLoading = ref(false)
const consultationThreadRef = ref(null)
const consultationWindowOpen = ref(false)
const consultationState = ref('idle')
const pausedAgentId = ref('')
const reviewingMessageId = ref('')
const followUpText = ref('')
const meetingPlaybackToken = ref(0)
const videoPlayback = reactive({})
const quizAnswers = reactive({})
const VIDEO_SEARCH_URL_MESSAGE = '当前链接是搜索结果页，不是具体视频。请打开某个视频后复制视频播放页链接。'

let ctx
let taskPollTimer = null
let videoTimer = null
let normalizingGeneratedResources = false
let componentMounted = false
const workspaceEventSource = `teacher-agent-resource-${Date.now()}-${Math.random().toString(16).slice(2)}`

const agentOutputs = reactive({
  preprocess: '',
  coordinator: '',
  knowledge: '',
  ability: '',
  behavior: '',
  resource: '',
  practice: '',
  report: '',
  qualityReview: '',
  consistencyReview: '',
})

const agentPositions = reactive(JSON.parse(JSON.stringify(defaultAgentPositions)))

const dynamicEdges = computed(() => buildDynamicEdges(edgesBase, agentPositions))

// ===== 拖拽交互逻辑 =====
const dragState = reactive({
  isDragging: false,
  agentId: null,
  startX: 0,
  startY: 0,
  initialNodeX: 0,
  initialNodeY: 0
});
let hasDragged = false; 

function startDrag(event, agentId) {
  if (event.button !== 0) return
  dragState.isDragging = true
  dragState.agentId = agentId;
  dragState.startX = event.clientX;
  dragState.startY = event.clientY;
  dragState.initialNodeX = agentPositions[agentId].x;
  dragState.initialNodeY = agentPositions[agentId].y;
  hasDragged = false;

  window.addEventListener('mousemove', onDrag);
  window.addEventListener('mouseup', stopDrag);
}

function onDrag(event) {
  if (!dragState.isDragging || !dragState.agentId) return;
  const dx = event.clientX - dragState.startX;
  const dy = event.clientY - dragState.startY;

  if (Math.abs(dx) > 3 || Math.abs(dy) > 3) {
    hasDragged = true;
  }

  agentPositions[dragState.agentId].x = Math.max(40, Math.min(1940, dragState.initialNodeX + dx));
  agentPositions[dragState.agentId].y = Math.max(20, Math.min(552, dragState.initialNodeY + dy));
}

function stopDrag() {
  dragState.isDragging = false;
  dragState.agentId = null;
  window.removeEventListener('mousemove', onDrag);
  window.removeEventListener('mouseup', stopDrag);
}

async function handleAgentClick(agentId) {
  if (hasDragged) return
  if (!selectedStudent.value) {
    ElMessage.warning('请先选择学生画像')
    return
  }
  if (consultationState.value === 'running' || consultationState.value === 'resuming' || meetingLoading.value) {
    pauseConsultationForAgent(agentId)
    return
  }
  openFollowUpForAgent(agentId)
}
// ==========================

const selectedStudent = computed(() =>
  students.value.find((item) => String(item.studentId) === String(selectedStudentId.value)) || null,
)

const weakPoints = computed(() => selectedStudent.value?.weakKnowledgePoints || [])

const lowestDimensions = computed(() =>
  [...(selectedStudent.value?.dimensions || [])]
    .filter((item) => item && item.name)
    .sort((a, b) => Number(a.score || 0) - Number(b.score || 0))
    .slice(0, 3),
)

// ===== 学科知识图谱（完整图谱 + 薄弱知识点高亮）=====
// 与公开知识图谱页共用同一套环形布局：中心 C 语言核心，分组沿圆周分布，
// 每个分组的知识点落在对应扇区内。薄弱知识点按名称匹配后高亮展示。
const kgViewBox = { width: 1600, height: 1000, cx: 800, cy: 500, groupRadius: 245, pointRadius: 410 }

const kgCenter = computed(() => ({
  id: cLanguageKnowledgeGraph.center.id,
  name: 'C 语言核心',
}))

const kgGroups = computed(() =>
  cLanguageKnowledgeGraph.groups.map((group, index) => {
    const angle = -90 + (360 / cLanguageKnowledgeGraph.groups.length) * index
    const rad = (Math.PI / 180) * angle
    return {
      id: group.id,
      name: group.name,
      x: kgViewBox.cx + Math.cos(rad) * kgViewBox.groupRadius,
      y: kgViewBox.cy + Math.sin(rad) * kgViewBox.groupRadius,
    }
  }),
)

// 学生画像中的薄弱知识点名称集合（用于在图谱中匹配高亮）
const kgWeakNames = computed(() =>
  new Set(weakPoints.value.map((wp) => wp.tagName || wp.name).filter(Boolean)),
)

// 薄弱点名称 → 图谱知识点名称 的别名映射
// 画像标签与图谱节点命名粒度不一致（如“指针与地址”对应“指针变量/指针与数组”），
// 这里兜底保证薄弱点一定能命中图谱中的相关知识点并高亮。
const WEAK_POINT_ALIAS = {
  '指针与地址': ['指针变量', '指针与数组', '指针解引用', '地址运算符', '指针运算', '指针初始化'],
  '文件读写': ['文件指针', 'fopen 模式', 'fclose 关闭', 'fscanf 读取', 'fprintf 写入', 'fgetc 读取', 'fputc 写入', 'feof 判断'],
  '结构体数组': ['结构体数组', '结构体定义', '结构体变量', '结构体指针'],
  '指针': ['指针变量', '指针初始化', '指针解引用', '指针运算', '指针与数组', '指针与函数'],
  '结构体': ['结构体定义', '结构体变量', '成员访问', '结构体初始化', '结构体数组', '结构体指针'],
  '文件': ['文件指针', 'fopen 模式', 'fclose 关闭', 'fscanf 读取', 'fprintf 写入'],
}

// 判断某个图谱知识点是否命中薄弱点（精确 / 子串 / 别名 / 所属分组名）
function isKpWeak(pointName, groupName) {
  for (const w of kgWeakNames.value) {
    if (!w) continue
    if (pointName === w) return true
    if (pointName.includes(w) || w.includes(pointName)) return true
    const aliases = WEAK_POINT_ALIAS[w] || []
    if (aliases.includes(pointName)) return true
    if (groupName && (groupName.includes(w) || w.includes(groupName))) return true
  }
  return false
}

// 完整学科图谱的全部知识点节点（带坐标 + 是否薄弱标记）
const kgPoints = computed(() => {
  const groups = cLanguageKnowledgeGraph.groups
  const pointsByGroup = new Map()
  groups.forEach((g) => pointsByGroup.set(g.id, []))
  cLanguageKnowledgeGraph.points.forEach((p) => {
    if (!pointsByGroup.has(p.groupId)) pointsByGroup.set(p.groupId, [])
    pointsByGroup.get(p.groupId).push(p)
  })

  const nodes = []
  groups.forEach((group, groupIndex) => {
    const groupPoints = pointsByGroup.get(group.id) || []
    const sector = 360 / groups.length
    const startAngle = -90 + sector * groupIndex - sector * 0.38
    const step = groupPoints.length > 1 ? (sector * 0.76) / (groupPoints.length - 1) : 0

    groupPoints.forEach((point, pointIndex) => {
      const angle = startAngle + step * pointIndex
      const rad = (Math.PI / 180) * angle
      const laneOffset = (pointIndex % 3) * 18
      const radius = kgViewBox.pointRadius + laneOffset
      nodes.push({
        id: point.id,
        groupId: group.id,
        name: point.name,
        description: point.description || '',
        x: kgViewBox.cx + Math.cos(rad) * radius,
        y: kgViewBox.cy + Math.sin(rad) * radius,
        weak: isKpWeak(point.name, group.name),
      })
    })
  })
  return nodes
})

// 传给知识图谱组件做高亮匹配的薄弱点（以图谱节点 id 为 knowledgePointId）
const kgWeakPoints = computed(() =>
  kgPoints.value
    .filter((p) => p.weak)
    .map((p) => ({ knowledgePointId: p.id, name: p.name })),
)

function handleKgPointSelect(point) {
  if (point?.name) {
    ElMessage.info(`已选中知识点：${point.name}${point.weak ? '（薄弱）' : ''}`)
  }
}

const completedSet = computed(() => new Set(completedAgents.value))
const flowingSet = computed(() => new Set(flowingEdges.value.map((edge) => edge.id)))

const profileStats = computed(() => {
  const student = selectedStudent.value
  return [
    { label: '能力值', value: student?.abilityScore ?? '-' },
    { label: '掌握度', value: student?.masteryAverage == null ? '-' : `${Math.round(Number(student.masteryAverage) * 100)}%` },
    { label: '薄弱点', value: weakPoints.value.length },
    { label: '完成次数', value: student?.completedAttemptCount ?? 0 },
  ]
})

const providerSummaryText = computed(() => buildProviderSummaryText(providerOptions.value, defaultProviderKey.value))

const consultationMessages = computed(() => {
  if (liveConsultationMessages.value.length > 0 && !Array.isArray(lastRun.value?.discussionMessages)) {
    return sortConsultationMessages(liveConsultationMessages.value)
  }
  const messages = Array.isArray(lastRun.value?.discussionMessages) ? lastRun.value.discussionMessages : []
  if (messages.length > 0) {
    const backendMessages = messages.map((item, index) => normalizeConsultationMessage(item, index))
    if (liveConsultationMessages.value.length === 0) return sortConsultationMessages(backendMessages)
    const existingIds = new Set(backendMessages.map((item) => item.id))
    return sortConsultationMessages([
      ...backendMessages,
      ...liveConsultationMessages.value.filter((item) => !existingIds.has(item.id)),
    ])
  }
  return buildFallbackConsultationMessages()
})

const consultationDockVisible = computed(() =>
  !consultationWindowOpen.value
  && (
    meetingLoading.value
    || consultationMessages.value.length > 0
    || consultationState.value !== 'idle'
  ),
)

const consultationDecision = computed(() => {
  const summary = lastRun.value?.decisionSummary
  if (summary && typeof summary === 'object') {
    return {
      status: summary.status || 'ready',
      headline: sanitizeConsultationText(summary.headline || '会诊共识已生成'),
      currentProblem: sanitizeConsultationText(summary.currentProblem || '系统已根据学生画像、作答表现和资源审核结果形成综合判断。'),
      weakPoints: normalizeStringList(summary.weakPoints),
      recommendedResources: normalizeStringList(summary.recommendedResources),
      recommendedPractice: normalizeStringList(summary.recommendedPractice),
      teacherAction: sanitizeConsultationText(summary.teacherAction || '建议教师结合资源草案和审核报告进行发布前复核。'),
      evidenceRefs: normalizeStringList(summary.evidenceRefs),
    }
  }
  const weakPointNames = weakPoints.value.map((item) => item.tagName).filter(Boolean).slice(0, 4)
  const dimensionNames = lowestDimensions.value.map((item) => item.name).filter(Boolean)
  return {
    status: consultationMessages.value.length ? 'fallback' : 'empty',
    headline: weakPointNames.length ? `优先围绕“${weakPointNames.join('、')}”完成一轮补强` : '等待生成会诊共识',
    currentProblem: dimensionNames.length
      ? `当前画像提示需关注 ${dimensionNames.join('、')}，建议先完成基础补强再进入综合练习。`
      : '选择学生并生成资源后，系统会汇总会诊共识。',
    weakPoints: weakPointNames,
    recommendedResources: generationConfig.selectedResourceTypes.map(resourceTypeDisplayLabel),
    recommendedPractice: ['查看知识讲解', '完成补救练习', '复盘最近错题'],
    teacherAction: '建议教师先查看资源草案，再决定是否保存或发送给学生。',
    evidenceRefs: buildConsultationEvidenceRefs(),
  }
})

const consultationEvidenceItems = computed(() => {
  const refs = consultationDecision.value.evidenceRefs || []
  if (refs.length > 0) return refs
  return buildConsultationEvidenceRefs()
})

const consultationEvidenceLinks = computed(() => {
  const student = selectedStudent.value || {}
  const weakPointRows = weakPoints.value.slice(0, 6)
  const dimensionRows = lowestDimensions.value.slice(0, 4)
  const weakPointNames = weakPointRows.map((item) => item.tagName).filter(Boolean)
  const dimensionNames = dimensionRows.map((item) => item.name).filter(Boolean)
  const resourceLabels = generationConfig.selectedResourceTypes.map(resourceTypeDisplayLabel)
  const messages = consultationMessages.value.filter((item) => item.agentId !== 'teacher')
  const latestByAgent = (agentIds) => messages.filter((item) => agentIds.includes(item.agentId)).slice(-2)
  const rows = []

  rows.push({
    id: 'summary-problem',
    type: '综合诊断',
    title: consultationDecision.value.headline || '当前学习问题定位',
    conclusion: consultationDecision.value.currentProblem || '会议结果会根据学生画像生成学习问题定位。',
    profileData: [
      student.stageName ? `画像阶段：${student.stageName}` : '',
      student.abilityScore != null ? `能力值：${student.abilityScore}` : '',
      student.completedAttemptCount != null ? `完成作答：${student.completedAttemptCount} 次` : '',
      student.masteryAverage != null ? `平均掌握度：${Math.round(Number(student.masteryAverage) * 100)}%` : '',
    ].filter(Boolean),
    meetingBasis: latestByAgent(['preprocess', 'coordinator', 'report']).map((item) => `${item.agentName}：${item.role}`),
    action: consultationDecision.value.teacherAction || '教师复核资源草案后再发布给学生。',
  })

  if (weakPointRows.length) {
    rows.push({
      id: 'weak-points',
      type: '知识证据',
      title: `薄弱知识点对应：${weakPointNames.join('、')}`,
      conclusion: `会议结果要求资源优先围绕 ${weakPointNames.join('、')} 展开，避免生成泛化材料。`,
      profileData: weakPointRows.map((item) => profileWeakPointEvidence(item)),
      meetingBasis: latestByAgent(['knowledge', 'resource', 'consistencyReview']).map((item) => `${item.agentName}：${sanitizeConsultationText(item.content, 120)}`),
      action: `生成 ${resourceLabels.join('、') || '知识讲解、补救练习、错因复盘'} 时必须绑定这些薄弱点。`,
    })
  }

  if (dimensionRows.length) {
    rows.push({
      id: 'ability-dimensions',
      type: '能力证据',
      title: `能力维度对应：${dimensionNames.join('、')}`,
      conclusion: `会议结果要求练习设计兼顾 ${dimensionNames.join('、')}，不能只做概念讲解。`,
      profileData: dimensionRows.map((item) => `${item.name}：${item.score ?? '-'} 分`),
      meetingBasis: latestByAgent(['ability', 'practice', 'report']).map((item) => `${item.agentName}：${sanitizeConsultationText(item.content, 120)}`),
      action: `练习数量 ${generationConfig.exerciseCount || 5} 题，覆盖基础识别、变式迁移和错因复盘。`,
    })
  }

  if (generatedResources.value.length || resourceLabels.length) {
    const resourceNames = generatedResources.value.length
      ? generatedResources.value.map((item) => item.title).filter(Boolean).slice(0, 6)
      : resourceLabels
    rows.push({
      id: 'resource-plan',
      type: '资源证据',
      title: `资源方案对应：${resourceNames.join('、') || '待生成资源'}`,
      conclusion: '会议结果要求每个个性化资源都能说明服务哪个画像薄弱点，以及如何帮助学生完成下一步学习。',
      profileData: [
        `资源类型：${resourceLabels.join('、') || '未选择'}`,
        `生成对象：${generationConfig.generationScope === 'class' ? '班级共性' : '单个学生'}`,
        `发布模式：${generationConfig.publishMode === 'publish_after_review' ? '审核后发布' : '只生成草稿'}`,
      ],
      meetingBasis: latestByAgent(['resource', 'qualityReview', 'consistencyReview']).map((item) => `${item.agentName}：${sanitizeConsultationText(item.content, 120)}`),
      action: '教师需要检查资源内容、题目答案解析、视频或讲义是否完整，再保存或发送。',
    })
  }

  if (teacherRequirement.value) {
    rows.push({
      id: 'teacher-requirement',
      type: '教师补充',
      title: '教师补充要求已纳入会诊',
      conclusion: teacherRequirement.value,
      profileData: ['来源：教师输入', `目标学生：${student.studentName || student.studentId || '当前学生'}`],
      meetingBasis: messages.filter((item) => item.content?.includes('教师')).slice(-2).map((item) => `${item.agentName}：${item.role}`),
      action: '后续智能体和资源生成需要优先满足该补充要求。',
    })
  }

  return rows
})

const finalMeetingResult = computed(() => {
  const messages = consultationMessages.value.filter((item) => item.agentId !== 'teacher')
  const agentIds = [...new Set(messages.map((item) => item.agentId).filter(Boolean))]
  const maxRound = messages
    .map((item) => Number(item.round || 1))
    .reduce((max, value) => Math.max(max, value), 1)
  const finalMessages = messages
    .filter((item) => ['report', 'qualityReview', 'consistencyReview', 'decision-agent'].includes(item.agentId))
    .slice(-4)
  const weakPointNames = weakPoints.value.map((item) => item.tagName).filter(Boolean).slice(0, 5)
  const dimensionNames = lowestDimensions.value.map((item) => item.name).filter(Boolean).slice(0, 4)
  const resourceNames = generatedResources.value.length
    ? generatedResources.value.map((item) => item.title).filter(Boolean).slice(0, 6)
    : consultationDecision.value.recommendedResources
  const consensus = [
    consultationDecision.value.currentProblem,
    weakPointNames.length
      ? `知识补强优先级：${weakPointNames.join('、')}，资源内容需要围绕这些薄弱点展开。`
      : '',
    dimensionNames.length
      ? `能力诊断关注：${dimensionNames.join('、')}，练习设计需兼顾理解迁移、作答表现和学习参与。`
      : '',
    finalMessages[0]?.content ? sanitizeConsultationText(finalMessages[0].content, 0) : '',
  ].filter(Boolean)
  const resourcePlan = (resourceNames.length ? resourceNames : ['知识讲解视频', '补救练习', '错因复盘', '学习路径']).map((name, index) => ({
    title: name,
    detail: [
      '先用讲解资源补齐概念和例题',
      '再用专项练习验证掌握情况',
      '最后用错因复盘和学习路径巩固迁移',
      '教师审核后再发送给学生',
    ][index % 4],
  }))
  const actionPlan = [
    '第一步：先复盘画像证据和最近作答，确认薄弱知识点是否与当前教学目标一致。',
    `第二步：围绕 ${weakPointNames.join('、') || '当前薄弱点'} 完成知识讲解与例题补强。`,
    `第三步：安排 ${generationConfig.exerciseCount || 5} 道左右的补救练习，覆盖基础题、变式题和错因复盘题。`,
    '第四步：教师检查资源是否可直接使用，确认无泛化讲解、无题目答案缺失、无知识点偏移。',
    '第五步：发布后继续观察学生作答结果，并将新证据回流到下一轮画像和资源会诊。',
  ]
  const reviewPoints = [
    '资源必须贴合画像薄弱点',
    '题目难度要与当前能力值匹配',
    '讲解内容避免泛泛而谈',
    '练习题需包含答案与解析',
    '发布前由教师最终确认',
  ]
  const digest = finalMessages.length
    ? finalMessages.map((item) => ({
      title: `${item.agentName}：${item.role}`,
      content: sanitizeConsultationText(item.content, 0),
    }))
    : [{
      title: '会诊决策摘要',
      content: consultationDecision.value.teacherAction,
    }]
  return {
    headline: consultationDecision.value.headline,
    status: consultationDecision.value.status,
    rounds: maxRound,
    agentCount: agentIds.length,
    evidenceCount: consultationEvidenceLinks.value.length || consultationEvidenceItems.value.length,
    resourceCount: resourcePlan.length,
    consensus,
    resourcePlan,
    actionPlan,
    reviewPoints,
    digest,
  }
})

function isReady(agentId) {
  return isAgentReady(agentId, {
    hasStudent: Boolean(selectedStudent.value),
    completedSet: completedSet.value,
  })
}

function normalizeStringList(value) {
  if (!Array.isArray(value)) return []
  return value.map((item) => sanitizeConsultationText(String(item || ''), 80)).filter(Boolean)
}

function profileWeakPointEvidence(item) {
  const parts = [item?.tagName || item?.name || '薄弱知识点']
  const score = item?.score ?? item?.masteryScore ?? item?.mastery
  const wrongCount = item?.wrongCount ?? item?.mistakeCount ?? item?.errorCount
  const attemptCount = item?.attemptCount ?? item?.questionCount ?? item?.totalCount
  if (score != null && score !== '') {
    const numericScore = Number(score)
    parts.push(Number.isFinite(numericScore) && numericScore <= 1 ? `掌握度 ${Math.round(numericScore * 100)}%` : `掌握度 ${score}`)
  }
  if (wrongCount != null && wrongCount !== '') parts.push(`错题 ${wrongCount} 次`)
  if (attemptCount != null && attemptCount !== '') parts.push(`相关作答 ${attemptCount} 次`)
  return parts.join(' · ')
}

function hasActiveConsultationState() {
  return Boolean(
    generationTaskId.value
    || lastRun.value
    || liveConsultationMessages.value.length
    || pendingConsultationMessages.value.length
    || generatedResources.value.length
    || completedAgents.value.length
    || ['running', 'resuming', 'refining', 'paused_for_teacher', 'completed'].includes(consultationState.value),
  )
}

function sanitizeConsultationText(value, maxLength = 220) {
  const text = String(value || '').replace(/\s+/g, ' ').trim()
  if (!text) return ''
  const blockedPatterns = [
    /system prompt/i,
    /developer prompt/i,
    /严格\s*JSON/i,
    /不要输出\s*markdown/i,
    /字段必须包含/i,
    /model parameters?/i,
    /temperature/i,
    /top_p/i,
  ]
  if (blockedPatterns.some((pattern) => pattern.test(text))) {
    return '系统已将内部分析过程整理为可读学习建议。'
  }
  if (!maxLength || text.length <= maxLength) return text
  return `${text.slice(0, maxLength)}...`
}

function normalizeConsultationMessage(item, index) {
  const agentId = item.agentId || `agent-${index}`
  const localAgent = agents[agentId] || {}
  const agentName = preferReadableText(item.agentName, localAgent.cnTitle || localAgent.title || '学习诊断智能体')
  const role = preferReadableText(item.role, consultationRole(agentId))
  return {
    id: item.id || `${item.agentId || 'agent'}-${item.turnIndex ?? index}-${index}`,
    turnIndex: Number(item.turnIndex ?? index + 1),
    round: item.round || 1,
    agentId,
    agentName: sanitizeConsultationText(agentName, 40),
    replyToAgentId: item.replyToAgentId || '',
    replyToAgentName: sanitizeConsultationText(preferReadableText(item.replyToAgentName, agents[item.replyToAgentId]?.cnTitle || ''), 40),
    role: sanitizeConsultationText(role, 40),
    content: sanitizeConsultationText(item.content || item.summary || '已完成本轮学习诊断。', 0),
    evidenceRefs: normalizeStringList(item.evidenceRefs),
    createdAt: sanitizeConsultationText(item.createdAt || '', 40),
    status: item.status || '',
    modelName: sanitizeConsultationText(item.modelName || '', 40),
    llmCallId: item.llmCallId || null,
  }
}

function preferReadableText(value, fallback) {
  const text = String(value || '').trim()
  if (!text) return fallback
  if (/[�]|[鏅璧鐭鑳涓绋绾]/.test(text)) return fallback
  return text
}

function sortConsultationMessages(messages) {
  return [...messages].sort((a, b) => {
    const aTurn = Number(a.turnIndex ?? 9999)
    const bTurn = Number(b.turnIndex ?? 9999)
    if (aTurn !== bTurn) return aTurn - bTurn
    return String(a.createdAt || '').localeCompare(String(b.createdAt || ''))
  })
}

watch(consultationMessages, () => {
  scrollConsultationToBottom()
}, { flush: 'post' })

function scrollConsultationToBottom() {
  nextTick(() => {
    const thread = consultationThreadRef.value
    if (!thread) return
    thread.scrollTop = thread.scrollHeight
  })
}

async function openConsultationWindow(autoStart = false) {
  consultationWindowOpen.value = true
  await nextTick()
  scrollConsultationToBottom()
  if (autoStart && !meetingLoading.value) {
    await startConsultationMeeting()
  }
}

function closeConsultationWindow() {
  consultationWindowOpen.value = false
}

function buildFallbackConsultationMessages() {
  if (!lastRun.value && generatedResources.value.length === 0) return []
  const traces = Array.isArray(lastRun.value?.agentTrace) ? lastRun.value.agentTrace : []
  const traceMessages = traces
    .filter((item) => item?.summary || item?.agentName)
    .slice(0, 6)
    .map((item, index) => normalizeConsultationMessage({
      round: 1,
      agentId: item.agentId || `trace-${index}`,
      agentName: item.agentName || '学习诊断智能体',
      role: item.status === 'failed' ? '需要复核' : '流程意见',
      content: item.summary || '该智能体已完成当前流程节点。',
      evidenceRefs: buildConsultationEvidenceRefs(),
    }, index))
  if (traceMessages.length > 0) return traceMessages
  return [
    normalizeConsultationMessage({
      round: 1,
      agentId: 'decision-agent',
      agentName: '会诊决策智能体',
      role: '最终共识',
      content: `已生成 ${generatedResources.value.length} 个个性化资源草案，建议教师结合画像薄弱点进行发布前复核。`,
      evidenceRefs: buildConsultationEvidenceRefs(),
    }, 0),
  ]
}

function buildConsultationEvidenceRefs(agentId = '') {
  const refs = []
  const student = selectedStudent.value
  if (student?.stageName) refs.push(`${student.stageName}画像`)
  if (student?.abilityScore != null) refs.push(`能力值 ${student.abilityScore}`)
  if (student?.completedAttemptCount != null) refs.push(`完成作答 ${student.completedAttemptCount} 次`)
  const weakPointNames = weakPoints.value.map((item) => item.tagName).filter(Boolean).slice(0, 4)
  if (weakPointNames.length) refs.push(`薄弱知识点：${weakPointNames.join('、')}`)
  if (generatedResources.value.length) refs.push(`资源草案 ${generatedResources.value.length} 个`)
  const agentEvidence = {
    knowledge: weakPointNames.length ? [`知识点：${weakPointNames.join('、')}`] : [],
    ability: lowestDimensions.value.map((item) => `${item.name} ${item.score ?? '-'}分`).slice(0, 3),
    behavior: student?.suggestions?.length ? student.suggestions.slice(0, 2) : [],
    resource: generationConfig.selectedResourceTypes.map(resourceTypeDisplayLabel),
    practice: [`练习数量 ${generationConfig.exerciseCount} 题`, generationConfig.difficulty === 'basic' ? '基础补弱' : generationConfig.difficulty === 'comprehensive' ? '综合迁移' : '提升训练'],
    report: generatedResources.value.length ? [`资源草案 ${generatedResources.value.length} 个`] : [],
    qualityReview: ['完整性', '可用性', '难度适配'],
    consistencyReview: ['画像一致', '主题一致', '课程目标一致'],
  }
  return [...new Set([...(agentEvidence[agentId] || []), ...refs])].slice(0, 6)
}

function resourceTypeDisplayLabel(type) {
  const labels = {
    knowledge_video: '知识讲解视频',
    remedial_exercise: '补救练习',
    knowledge_handout: '知识点讲义',
    error_reflection: '错因复盘',
    learning_path: '学习路径',
  }
  return labels[type] || '个性化学习资源'
}

function isCompleted(agentId) {
  return completedSet.value.has(agentId)
}

function isActive(agentId) {
  return activeAgentId.value === agentId
}

function isReceiving(agentId) {
  return isReceivingAgent(agentId, flowingEdges.value)
}

function isPending(agentId) {
  return isPendingAgent(agentId, {
    ready: isReady(agentId),
    completed: isCompleted(agentId),
    active: isActive(agentId),
    flowingEdges: flowingEdges.value,
  })
}

function statusClass(agentId) {
  return agentNodeStatusClass({
    active: isActive(agentId),
    receiving: isReceiving(agentId),
    completed: isCompleted(agentId),
  })
}

function agentStatus(agentId) {
  return agentStatusText({
    completed: isCompleted(agentId),
    active: isActive(agentId),
    ready: isReady(agentId),
  })
}

function edgeClass(edge) {
  return edgeClassState(edge, {
    flowingSet: flowingSet.value,
    completedSet: completedSet.value,
  })
}

function addLog(agentId, content) {
  logs.value = prependActivityLog(logs.value, agentId, content, agents)
  persistLogs()
}

function addConsultationMessage(agentId, content, options = {}) {
  const agent = agents[agentId] || {}
  const message = normalizeConsultationMessage({
    id: options.id,
    turnIndex: options.turnIndex,
    round: options.round || 1,
    agentId,
    agentName: options.agentName || agent.cnTitle || agent.title || '学习诊断智能体',
    replyToAgentId: options.replyToAgentId || '',
    replyToAgentName: options.replyToAgentName || '',
    role: options.role || consultationRole(agentId),
    content: options.content || content,
    evidenceRefs: options.evidenceRefs || buildConsultationEvidenceRefs(agentId),
    createdAt: new Date().toLocaleTimeString('zh-CN', { hour12: false }),
    status: options.status || '',
    modelName: options.modelName || '',
    llmCallId: options.llmCallId || null,
  }, liveConsultationMessages.value.length)
  const replaceId = options.replaceId || message.id
  const existingIndex = liveConsultationMessages.value.findIndex((item) => item.id === replaceId)
  if (existingIndex >= 0) {
    liveConsultationMessages.value = liveConsultationMessages.value.map((item, index) => (index === existingIndex ? message : item))
  } else {
    liveConsultationMessages.value = [...liveConsultationMessages.value, message]
  }
  activeConsultationTab.value = 'discussion'
  return message
}

function isMessageAwaitingReview(message) {
  return Boolean(
    message
    && message.status === 'fallback'
    && consultationState.value === 'paused_for_teacher'
    && reviewingMessageId.value === message.id
  )
}

function markConsultationMessageReviewed(messageId) {
  if (!messageId) return
  liveConsultationMessages.value = liveConsultationMessages.value.map((item) => {
    if (item.id !== messageId) return item
    return {
      ...item,
      status: 'reviewed',
      evidenceRefs: [...new Set([...(item.evidenceRefs || []), '教师已复核'])],
    }
  })
}

async function addModelConsultationMessage(agentId, fallbackOutput) {
  if (meetingLoading.value || liveConsultationMessages.value.length > 0) return
  if (['coordinator', 'knowledge', 'ability'].includes(agentId)) {
    await startConsultationMeeting({ silent: true })
  }
}

async function startConsultationMeeting(options = {}) {
  const student = selectedStudent.value
  activeConsultationTab.value = 'discussion'
  consultationWindowOpen.value = true
  if (!student) {
    ElMessage.warning('请先选择学生')
    return
  }
  meetingPlaybackToken.value += 1
  const token = meetingPlaybackToken.value
  liveConsultationMessages.value = []
  pendingConsultationMessages.value = []
  completedAgents.value = []
  generatedResources.value = []
  lastRun.value = null
  activeAgentId.value = ''
  flowingEdges.value = []
  pausedAgentId.value = ''
  reviewingMessageId.value = ''
  followUpText.value = ''
  consultationState.value = 'running'
  meetingLoading.value = true
  const pendingId = `meeting-pending-${Date.now()}`
  addConsultationMessage('coordinator', '', {
    id: pendingId,
    content: '正在组织多智能体会诊对话，知识、能力、错因、资源和审核智能体将依次回应前一位观点...',
    role: '会诊主持',
    status: 'running',
    evidenceRefs: buildConsultationEvidenceRefs('coordinator'),
  })
  persistWorkspace()
  try {
    const messages = await teacherApi.discussAgentMeeting({
      studentId: Number(student.studentId),
      stage: stage.value,
      resourceTypes: generationConfig.selectedResourceTypes?.length ? generationConfig.selectedResourceTypes : defaultResourceTypes,
      selectedResourceTypes: generationConfig.selectedResourceTypes?.length ? generationConfig.selectedResourceTypes : defaultResourceTypes,
      generationScope: generationConfig.generationScope,
      classId: generationConfig.classId,
      difficulty: generationConfig.difficulty,
      exerciseCount: generationConfig.exerciseCount,
      publishMode: generationConfig.publishMode,
      selectedWeakPoints: generationConfig.selectedWeakPoints,
      providerKey: defaultProviderKey.value || undefined,
      agentProviderKeys: buildAgentProviderPayload(agentOverrides),
      teacherRequirement: teacherRequirement.value,
      discussionMessages: [],
    })
    const normalized = Array.isArray(messages)
      ? messages.map((item, index) => normalizeConsultationMessage(item, index))
      : []
    if (normalized.length === 0) {
      addConsultationMessage('coordinator', '', {
        replaceId: pendingId,
        content: '会诊暂未返回有效对话，请稍后重试或先完成单个智能体流程。',
        role: '会诊主持',
        status: 'fallback',
      })
    } else {
      liveConsultationMessages.value = []
      await playConsultationMessages(normalized, { token, reset: false })
      if (!options.silent) ElMessage.success('多智能体会诊对话已生成')
    }
  } catch (error) {
    liveConsultationMessages.value = []
    await playConsultationMessages(buildLocalMeetingMessages(error.message || '会诊接口暂不可用'), { token, reset: false })
    if (!options.silent) {
      ElMessage.warning('会诊接口暂不可用，已展示待接入模型的对话样例；重启后端后可使用真实模型会诊。')
    }
  } finally {
    if (!componentMounted && consultationState.value === 'running' && pendingConsultationMessages.value.length > 0) {
      persistWorkspace()
      return
    }
    if (consultationState.value !== 'paused_for_teacher') {
      meetingLoading.value = false
      consultationState.value = generatedResources.value.length ? 'completed' : 'idle'
      activeAgentId.value = ''
      flowingEdges.value = []
    }
    persistWorkspace()
  }
}

async function playConsultationMessages(messages, options = {}) {
  const token = options.token ?? meetingPlaybackToken.value
  const rows = sortConsultationMessages(messages)
  if (options.replacePending !== false) {
    pendingConsultationMessages.value = rows
  }
  persistWorkspace()
  for (const message of [...pendingConsultationMessages.value]) {
    if (token !== meetingPlaybackToken.value) return
    if (consultationState.value === 'paused_for_teacher') return
    await playConsultationStep(message, token)
  }
}

async function playConsultationStep(message, token) {
  const agentId = message.agentId
  activeAgentId.value = agentId
  flowingEdges.value = incomingEdgesForAgent(agentId, message.replyToAgentId)
  await nextTick()
  await Promise.all([
    animateNode(agentId),
    animateEdges(flowingEdges.value),
  ])
  if (token !== meetingPlaybackToken.value || consultationState.value === 'paused_for_teacher') return
  await new Promise(resolve => setTimeout(resolve, 420))
  if (token !== meetingPlaybackToken.value || consultationState.value === 'paused_for_teacher') return
  liveConsultationMessages.value = sortConsultationMessages([
    ...liveConsultationMessages.value.filter((item) => item.id !== message.id),
    message,
  ])
  pendingConsultationMessages.value = pendingConsultationMessages.value.filter((item) => item.id !== message.id)
  if (!completedAgents.value.includes(agentId)) completedAgents.value.push(agentId)
  agentOutputs[agentId] = message.content
  addLog(agentId, message.content)
  if (message.status === 'fallback') {
    consultationState.value = 'paused_for_teacher'
    meetingLoading.value = false
    pausedAgentId.value = agentId
    reviewingMessageId.value = message.id
    activeAgentId.value = agentId
    flowingEdges.value = []
    persistWorkspace()
    ElMessage.warning(`${agentDisplayName(agentId)} 的会诊意见需要教师复核，确认或补充后才会继续。`)
    return
  }
  if (agentId === 'consistencyReview') {
    await generateResourcesFromConsultation()
  }
  activeAgentId.value = ''
  flowingEdges.value = []
  persistWorkspace()
}

function incomingEdgesForAgent(agentId, replyToAgentId = '') {
  if (!agentId || agentId === 'preprocess') return []
  const incoming = edgesBase.filter((edge) => edge.to === agentId)
  if (incoming.length) return incoming
  if (replyToAgentId) {
    return edgesBase.filter((edge) => edge.from === replyToAgentId && edge.to === agentId)
  }
  return []
}

async function generateResourcesFromConsultation() {
  if (generatedResources.value.length > 0 && consultationState.value !== 'refining') return
  if (String(selectedStudent.value?.studentId).startsWith('DEMO')) {
    generatedResources.value = normalizeGeneratedCards(buildGeneratedResources(), resourceCardContext())
  } else {
    await generateAgentResources()
  }
  await nextTick()
  animateGeneratedCards()
}

function pauseConsultationForAgent(agentId) {
  meetingPlaybackToken.value += 1
  consultationWindowOpen.value = true
  consultationState.value = 'paused_for_teacher'
  meetingLoading.value = false
  pausedAgentId.value = agentId
  reviewingMessageId.value = ''
  followUpText.value = ''
  activeAgentId.value = agentId
  flowingEdges.value = []
  activeConsultationTab.value = 'discussion'
  ElMessage.info(`已暂停会诊，可补充或追问 ${agentDisplayName(agentId)}`)
}

function openFollowUpForAgent(agentId) {
  consultationWindowOpen.value = true
  pausedAgentId.value = agentId
  reviewingMessageId.value = ''
  followUpText.value = ''
  consultationState.value = generatedResources.value.length ? 'refining' : 'paused_for_teacher'
  activeConsultationTab.value = 'discussion'
}

function prepareMessageFollowUp(message) {
  if (!message) return
  consultationWindowOpen.value = true
  pausedAgentId.value = message.agentId
  reviewingMessageId.value = message.id
  followUpText.value = ''
  consultationState.value = 'paused_for_teacher'
  activeConsultationTab.value = 'discussion'
}

function cancelFollowUp() {
  pausedAgentId.value = ''
  reviewingMessageId.value = ''
  followUpText.value = ''
  if (consultationState.value === 'paused_for_teacher' || consultationState.value === 'refining') {
    consultationState.value = generatedResources.value.length ? 'completed' : 'idle'
  }
}

async function continueConsultationAfterReview() {
  const messageId = reviewingMessageId.value
  if (!messageId) return
  const reviewedMessage = liveConsultationMessages.value.find((item) => item.id === messageId)
  markConsultationMessageReviewed(messageId)
  pausedAgentId.value = ''
  reviewingMessageId.value = ''
  followUpText.value = ''
  consultationState.value = pendingConsultationMessages.value.length ? 'resuming' : 'completed'
  meetingLoading.value = pendingConsultationMessages.value.length > 0
  meetingPlaybackToken.value += 1
  const token = meetingPlaybackToken.value
  try {
    if (pendingConsultationMessages.value.length) {
      await playConsultationMessages(pendingConsultationMessages.value, { token, replacePending: false })
    } else if (reviewedMessage?.agentId === 'consistencyReview') {
      await generateResourcesFromConsultation()
    }
    if (consultationState.value !== 'paused_for_teacher') {
      meetingLoading.value = false
      consultationState.value = generatedResources.value.length || reviewedMessage?.agentId === 'consistencyReview' ? 'completed' : 'idle'
      activeAgentId.value = ''
      flowingEdges.value = []
    }
    persistWorkspace()
  } catch (error) {
    meetingLoading.value = false
    consultationState.value = 'paused_for_teacher'
    pausedAgentId.value = reviewedMessage?.agentId || ''
    reviewingMessageId.value = reviewedMessage?.id || ''
    ElMessage.error(error.message || '继续会诊失败')
    persistWorkspace()
  }
}

async function submitAgentFollowUp() {
  const agentId = pausedAgentId.value
  const feedback = followUpText.value.trim()
  consultationWindowOpen.value = true
  if (!agentId) return
  if (!feedback) {
    ElMessage.warning('请先输入要补充或追问的内容')
    return
  }
  const teacherMessage = normalizeConsultationMessage({
    id: `teacher-followup-${Date.now()}`,
    turnIndex: nextConsultationTurnIndex(),
    round: currentConsultationRound(),
    agentId: 'teacher',
    agentName: '教师补充',
    replyToAgentId: agentId,
    replyToAgentName: agentDisplayName(agentId),
    role: '追问 / 补充',
    content: feedback,
    evidenceRefs: [`追问对象：${agentDisplayName(agentId)}`],
    createdAt: new Date().toLocaleTimeString('zh-CN', { hour12: false }),
    status: 'success',
  }, liveConsultationMessages.value.length)
  if (reviewingMessageId.value) {
    markConsultationMessageReviewed(reviewingMessageId.value)
  }
  liveConsultationMessages.value = sortConsultationMessages([...liveConsultationMessages.value, teacherMessage])
  const startAgentId = agentId
  pausedAgentId.value = ''
  reviewingMessageId.value = ''
  followUpText.value = ''
  pendingConsultationMessages.value = []
  consultationState.value = generatedResources.value.length ? 'refining' : 'resuming'
  meetingLoading.value = true
  meetingPlaybackToken.value += 1
  const token = meetingPlaybackToken.value
  try {
    const messages = await teacherApi.discussAgentMeeting({
      studentId: Number(selectedStudent.value.studentId),
      stage: stage.value,
      resourceTypes: generationConfig.selectedResourceTypes?.length ? generationConfig.selectedResourceTypes : defaultResourceTypes,
      selectedResourceTypes: generationConfig.selectedResourceTypes?.length ? generationConfig.selectedResourceTypes : defaultResourceTypes,
      generationScope: generationConfig.generationScope,
      classId: generationConfig.classId,
      difficulty: generationConfig.difficulty,
      exerciseCount: generationConfig.exerciseCount,
      publishMode: generationConfig.publishMode,
      selectedWeakPoints: generationConfig.selectedWeakPoints,
      providerKey: defaultProviderKey.value || undefined,
      agentProviderKeys: buildAgentProviderPayload(agentOverrides),
      teacherRequirement: teacherRequirement.value,
      feedback,
      agentId: startAgentId,
      discussionMessages: consultationMessages.value.filter((item) => item.status !== 'running'),
    })
    const currentMaxTurn = nextConsultationTurnIndex() - 1
    const normalized = Array.isArray(messages)
      ? messages.map((item, index) => normalizeConsultationMessage(item, index)).filter((item) => Number(item.turnIndex || 0) > currentMaxTurn)
      : []
    const rows = normalized.length ? normalized : buildLocalMeetingMessages('', startAgentId, feedback)
    await playConsultationMessages(rows, { token, reset: false })
  } catch (error) {
    await playConsultationMessages(buildLocalMeetingMessages(error.message || '会诊接口暂不可用', startAgentId, feedback), { token, reset: false })
    ElMessage.warning('会诊接口暂不可用，已用本地会诊流继续演示。')
  } finally {
    if (!componentMounted && ['resuming', 'refining'].includes(consultationState.value) && pendingConsultationMessages.value.length > 0) {
      persistWorkspace()
      return
    }
    if (consultationState.value !== 'paused_for_teacher') {
      meetingLoading.value = false
      consultationState.value = 'completed'
      activeAgentId.value = ''
      flowingEdges.value = []
    }
    persistWorkspace()
  }
}

function nextConsultationTurnIndex() {
  return liveConsultationMessages.value
    .map((item) => Number(item.turnIndex || 0))
    .reduce((max, value) => Math.max(max, value), 0) + 1
}

function currentConsultationRound() {
  return liveConsultationMessages.value
    .map((item) => Number(item.round || 1))
    .reduce((max, value) => Math.max(max, value), 1)
}

function agentDisplayName(agentId) {
  if (agentId === 'teacher') return '教师'
  const agent = agents[agentId] || {}
  return preferReadableText(agent.cnTitle || agent.title, consultationRole(agentId))
}

function buildLocalMeetingMessages(reason = '', startAgentId = '', feedback = '') {
  const weakPointNames = weakPoints.value.map((item) => item.tagName).filter(Boolean).slice(0, 4)
  const dimensionNames = lowestDimensions.value.map((item) => item.name).filter(Boolean).slice(0, 3)
  const weakText = weakPointNames.length ? weakPointNames.join('、') : '当前薄弱知识点'
  const dimensionText = dimensionNames.length ? dimensionNames.join('、') : '理解迁移、实践能力'
  const resourceText = generationConfig.selectedResourceTypes.map(resourceTypeDisplayLabel).join('、') || '知识讲解、补救练习、学习路径'
  const suffix = reason ? '（当前后端会诊接口未连接，重启后端后将由绑定模型实时生成。）' : ''
  const rows = [
    {
      round: 1,
      agentId: 'preprocess',
      role: '证据整理',
      content: `我先整理学生画像、作答记录和资源需求：本轮会诊重点围绕 ${weakText} 与 ${dimensionText} 展开，后续所有建议都要引用这些证据。${suffix}`,
    },
    {
      round: 1,
      agentId: 'coordinator',
      replyToAgentId: 'preprocess',
      role: '会诊主持',
      content: `我回应预处理证据：大家围绕 ${weakText} 和 ${dimensionText} 判断学生卡在哪里，再共同形成资源方案。`,
    },
    {
      round: 1,
      agentId: 'knowledge',
      replyToAgentId: 'coordinator',
      role: '知识诊断',
      content: `我已阅读预处理和协调意见。知识层面确实应先补 ${weakText}，但需要校准一点：资源不能只按知识点平铺，要把概念、例题、易错点和作答证据串成一条可复盘的学习链。`,
    },
    {
      round: 1,
      agentId: 'ability',
      replyToAgentId: 'knowledge',
      role: '能力评估',
      content: `我综合前面全部意见后补充：这些薄弱点会继续影响 ${dimensionText}。如果方案只强调补知识还不够，资源后面必须接变式练习，验证学生能否把概念迁移到新题。`,
    },
    {
      round: 1,
      agentId: 'behavior',
      replyToAgentId: 'ability',
      role: '错因复盘',
      content: '我看完知识和能力判断后补充错因视角：如果学生只看讲解不复盘错因，仍可能在边界条件、输入输出和变量状态上反复出错。因此资源方案必须包含错题复盘清单，而不是只给讲义和练习。',
    },
    {
      round: 2,
      agentId: 'resource',
      replyToAgentId: 'behavior',
      role: '资源建议',
      content: `我基于前面所有诊断意见提出资源包：先给 ${weakText} 的讲解材料，再配补救练习、错因复盘和学习路径，资源类型建议为 ${resourceText}。如果某类资源没有直接服务薄弱点，应退回重写。`,
    },
    {
      round: 2,
      agentId: 'practice',
      replyToAgentId: 'resource',
      role: '练习设计',
      content: '我综合知识、能力、错因和资源方案后设计练习：练习分三层，先基础识别，再变式迁移，最后用错因复盘题让学生写出判断依据。若题目只考记忆、不检验迁移，需要调整。',
    },
    {
      round: 2,
      agentId: 'report',
      replyToAgentId: 'practice',
      role: '阶段汇总',
      content: '我汇总当前多方意见：诊断结论、资源包和练习梯度已经形成。这里先不直接发布，下一步应由审核智能体逐项检查资源是否完整、可用、难度匹配且与画像一致。',
    },
    {
      round: 3,
      agentId: 'qualityReview',
      replyToAgentId: 'report',
      role: '质量审核',
      content: '我阅读了前面全部会诊意见后进行质量审核：资源方向可以采用，但题干、答案、解析和知识点标签必须完整，视频或讲义要能直接给学生使用；缺少任何一项都应标记为需修改。',
    },
    {
      round: 3,
      agentId: 'consistencyReview',
      replyToAgentId: 'qualityReview',
      role: '一致性审核',
      content: `我在质量审核基础上做一致性复核：资源方向与画像证据基本一致，重点仍应锁定 ${weakText}。如果前面方案中出现过宽泛的综合材料，需要删减或改写为针对性资源。`,
    },
  ]
  const startIndex = startAgentId ? Math.max(0, rows.findIndex((item) => item.agentId === startAgentId)) : 0
  const selectedRows = rows.slice(startIndex >= 0 ? startIndex : 0)
  const baseTurn = nextConsultationTurnIndex()
  return selectedRows.map((item, index) => normalizeConsultationMessage({
    id: `local-meeting-${Date.now()}-${index + 1}-${item.agentId}`,
    turnIndex: baseTurn + index,
    agentName: agents[item.agentId]?.cnTitle || agents[item.agentId]?.title || consultationRole(item.agentId),
    replyToAgentName: item.replyToAgentId ? (agents[item.replyToAgentId]?.cnTitle || agents[item.replyToAgentId]?.title || consultationRole(item.replyToAgentId)) : '',
    evidenceRefs: buildConsultationEvidenceRefs(item.agentId),
    status: 'fallback',
    createdAt: new Date().toLocaleTimeString('zh-CN', { hour12: false }),
    ...item,
    content: feedback && index === 0 ? `${item.content} 我已纳入教师补充：“${feedback}”。` : item.content,
  }, index))
}

function consultationRole(agentId) {
  const roles = {
    preprocess: '证据整理',
    coordinator: '会诊主持',
    knowledge: '知识诊断',
    ability: '能力评估',
    behavior: '错因复盘',
    resource: '资源建议',
    practice: '练习设计',
    report: '最终共识',
    qualityReview: '质量审核',
    consistencyReview: '一致性审核',
  }
  return roles[agentId] || '学习诊断'
}

function consultationSpeech(agentId, output) {
  const weakPointNames = weakPoints.value.map((item) => item.tagName).filter(Boolean).slice(0, 4)
  const dimensionNames = lowestDimensions.value.map((item) => item.name).filter(Boolean).slice(0, 3)
  const weakText = weakPointNames.length ? weakPointNames.join('、') : '当前薄弱知识点'
  const dimensionText = dimensionNames.length ? dimensionNames.join('、') : '当前能力短板'
  const resourceText = generationConfig.selectedResourceTypes.map(resourceTypeDisplayLabel).join('、') || '知识讲解、补救练习、学习路径'
  const speeches = {
    preprocess: `我已读取并整理学生画像证据，本轮会诊将围绕 ${weakText} 和 ${dimensionText} 展开。`,
    coordinator: '我将本轮会诊分配给知识诊断、能力评估、错因复盘、资源生成和审核智能体，先形成意见，再汇总为教师可采纳建议。',
    knowledge: `从知识掌握角度看，优先补强 ${weakText}。资源内容需要直接讲清概念、例题和易错点。`,
    ability: `从能力维度看，当前重点是 ${dimensionText}。建议先完成基础识别题，再过渡到变式练习和综合任务。`,
    behavior: '从错题和学习轨迹看，学生需要把错误原因写清楚，尤其是变量状态、边界条件和输入输出依据。',
    resource: `我建议本轮生成 ${resourceText}，并把每个资源都绑定到画像薄弱点，避免泛泛讲解。`,
    practice: `练习设计建议控制在 ${generationConfig.exerciseCount} 题左右，包含基础题、变式题和错因复盘题。`,
    report: `会诊共识已形成：先围绕 ${weakText} 完成资源补强，再由教师审核后保存或发送给学生。`,
    qualityReview: '我会重点检查资源是否完整、可直接使用、难度是否适合当前学生。',
    consistencyReview: '我会重点检查资源主题是否与学生画像、薄弱点和课程目标保持一致。',
  }
  return speeches[agentId] || output
}

function persistLogs() {
  persistActivityLogs(logs.value)
}

function restoreLogs() {
  logs.value = readActivityLogs()
}

function deleteLog(logId) {
  logs.value = removeActivityLog(logs.value, logId)
  persistLogs()
}

function clearLogs() {
  logs.value = []
  persistLogs()
}

function resetOutputs() {
  Object.keys(agentOutputs).forEach((key) => {
    agentOutputs[key] = ''
  })
}

function resetFlow(options = {}) {
  const preserveGeneratedResources = options.preserveGeneratedResources === true
  meetingPlaybackToken.value += 1
  completedAgents.value = []
  activeAgentId.value = ''
  flowingEdges.value = []
  executionStopped.value = false
  meetingLoading.value = false
  consultationState.value = 'idle'
  pausedAgentId.value = ''
  reviewingMessageId.value = ''
  followUpText.value = ''
  candidateResources.value = []
  if (!preserveGeneratedResources) {
    generatedResources.value = []
  }
  lastRun.value = null
  liveConsultationMessages.value = []
  pendingConsultationMessages.value = []
  revisionDraft.value = null
  revisionFeedback.value = ''
  resetOutputs()
  persistWorkspace()
}

function resetFlowAndTask() {
  stopTaskPolling()
  clearTaskPersistence()
  resetFlow({ preserveGeneratedResources: true })
}

function stopTaskPolling() {
  if (taskPollTimer) {
    window.clearInterval(taskPollTimer)
    taskPollTimer = null
  }
}

function persistTask(task) {
  if (!task?.taskId) return
  generationTaskId.value = task.taskId
  generationTaskStatus.value = task.status || 'queued'
  generationTaskMessage.value = task.message || ''
  writeStorageJson(TASK_STORAGE_KEY, buildTaskSnapshot(task, {
    studentId: selectedStudent.value?.studentId || null,
    stage: stage.value,
  }))
  persistWorkspace()
}

function clearTaskPersistence(options = {}) {
  const preserveMessage = options.preserveMessage === true
  const preservedMessage = generationTaskMessage.value
  generationTaskId.value = ''
  generationTaskStatus.value = 'idle'
  generationTaskMessage.value = preserveMessage ? preservedMessage : ''
  removeStorageItem(TASK_STORAGE_KEY)
  persistWorkspace()
}

function syncGeneratedOutputs() {
  if (!lastRun.value) return
  executionStopped.value = false
  completedAgents.value = ['preprocess', 'coordinator', 'knowledge', 'ability', 'behavior', 'resource', 'practice', 'report', 'qualityReview', 'consistencyReview']
  agentOutputs.report = traceSummary('generator', `已生成 ${generatedResources.value.length} 个个性化资源草案，并完成后端审核。`)
  agentOutputs.qualityReview = traceSummary('quality-review', '资源质量审核已完成。')
  agentOutputs.consistencyReview = traceSummary('consistency-review', '主题一致性审核已完成。')
}

function applyGenerationResult(result) {
  lastRun.value = result || {}
  generatedResources.value = normalizeGeneratedCards(result?.resources, resourceCardContext())
  syncGeneratedOutputs()
  persistWorkspace()
}

function persistWorkspace() {
  writeStorageJson(AGENT_WORKSPACE_STORAGE_KEY, buildWorkspaceSnapshot({
    selectedStudentId: selectedStudentId.value,
    stage: stage.value,
    completedAgents: completedAgents.value,
    activeAgentId: activeAgentId.value,
    flowingEdges: flowingEdges.value,
    candidateResources: candidateResources.value,
    generatedResources: generatedResources.value,
    lastRun: lastRun.value,
    liveConsultationMessages: liveConsultationMessages.value,
    pendingConsultationMessages: pendingConsultationMessages.value,
    consultationState: consultationState.value,
    pausedAgentId: pausedAgentId.value,
    reviewingMessageId: reviewingMessageId.value,
    teacherRequirement: teacherRequirement.value,
    generationConfig: { ...generationConfig },
    agentOutputs,
    generationTaskId: generationTaskId.value,
    generationTaskStatus: generationTaskStatus.value,
    generationTaskMessage: generationTaskMessage.value,
  }))
  window.dispatchEvent(new CustomEvent('teacher-agent-resource-workspace-updated', {
    detail: { source: workspaceEventSource },
  }))
}

function restoreWorkspace() {
  try {
    const saved = readStorageJson(AGENT_WORKSPACE_STORAGE_KEY)
    if (!saved) return
    if (saved.selectedStudentId) selectedStudentId.value = String(saved.selectedStudentId)
    if (saved.stage) stage.value = saved.stage
    completedAgents.value = Array.isArray(saved.completedAgents) ? saved.completedAgents : []
    activeAgentId.value = saved.activeAgentId || ''
    flowingEdges.value = Array.isArray(saved.flowingEdges) ? saved.flowingEdges : []
    candidateResources.value = Array.isArray(saved.candidateResources) ? saved.candidateResources : []
    generatedResources.value = normalizeGeneratedCards(saved.generatedResources, resourceCardContext())
    lastRun.value = saved.lastRun || null
    liveConsultationMessages.value = Array.isArray(saved.liveConsultationMessages) ? saved.liveConsultationMessages : []
    pendingConsultationMessages.value = Array.isArray(saved.pendingConsultationMessages) ? saved.pendingConsultationMessages : []
    consultationState.value = saved.consultationState || (generatedResources.value.length ? 'completed' : 'idle')
    if (['running', 'resuming', 'refining'].includes(consultationState.value)) {
      meetingLoading.value = true
    }
    pausedAgentId.value = saved.pausedAgentId || ''
    reviewingMessageId.value = saved.reviewingMessageId || ''
    teacherRequirement.value = saved.teacherRequirement || ''
    Object.assign(generationConfig, saved.generationConfig || {})
    Object.keys(agentOutputs).forEach((key) => {
      agentOutputs[key] = saved.agentOutputs?.[key] || ''
    })
    generationTaskId.value = saved.generationTaskId || ''
    generationTaskStatus.value = saved.generationTaskStatus || 'idle'
    generationTaskMessage.value = saved.generationTaskMessage || ''
  } catch {
    // ignore
  }
}

async function resumePendingConsultationPlayback() {
  if (!pendingConsultationMessages.value.length) return
  if (!['running', 'resuming', 'refining'].includes(consultationState.value)) return
  meetingLoading.value = true
  meetingPlaybackToken.value += 1
  const token = meetingPlaybackToken.value
  await playConsultationMessages(pendingConsultationMessages.value, {
    token,
    replacePending: false,
  })
  if (consultationState.value !== 'paused_for_teacher') {
    meetingLoading.value = false
    consultationState.value = generatedResources.value.length ? 'completed' : 'idle'
    activeAgentId.value = ''
    flowingEdges.value = []
    persistWorkspace()
  }
}

function handleWorkspaceUpdated(event) {
  if (event?.detail?.source === workspaceEventSource) return
  const saved = readStorageJson(AGENT_WORKSPACE_STORAGE_KEY)
  if (!saved || !Array.isArray(saved.pendingConsultationMessages) || saved.pendingConsultationMessages.length === 0) return
  const currentPendingIds = new Set(pendingConsultationMessages.value.map((item) => item.id))
  const hasNewPending = saved.pendingConsultationMessages.some((item) => !currentPendingIds.has(item.id))
  if (!hasNewPending) return
  liveConsultationMessages.value = Array.isArray(saved.liveConsultationMessages) ? saved.liveConsultationMessages : liveConsultationMessages.value
  pendingConsultationMessages.value = saved.pendingConsultationMessages
  consultationState.value = saved.consultationState || 'running'
  meetingLoading.value = true
  resumePendingConsultationPlayback()
}

function loadDemoData(showMessage = true) {
  clearTaskPersistence()
  stopTaskPolling()
  students.value = [demoStudent]
  selectedStudentId.value = String(demoStudent.studentId)
  candidateResources.value = demoResources
  resetFlow()
  persistWorkspace()
  if (showMessage) ElMessage.success('已载入演示学生画像')
}

async function loadStudents() {
  loading.value = true
  try {
    const rows = await stageEvaluationApi.teacherStudents({
      stage: stage.value,
      studentId: studentId.value,
    })
    students.value = Array.isArray(rows) ? rows : []
    if (students.value.length > 0) {
      const stillExists = students.value.some((item) => String(item.studentId) === String(selectedStudentId.value))
      selectedStudentId.value = stillExists ? selectedStudentId.value : String(students.value[0].studentId)
      if (!hasActiveConsultationState()) {
        resetFlow()
      }
    } else {
      loadDemoData(false)
      ElMessage.info('当前接口暂无数据，已展示演示学生画像')
    }
  } catch (error) {
    loadDemoData(false)
    ElMessage.warning(error.message || '画像接口暂不可用，已展示演示学生画像')
  } finally {
    loading.value = false
  }
}

async function loadTeacherClasses() {
  try {
    const rows = await classApi.mine()
    teacherClasses.value = Array.isArray(rows) ? rows : []
    if (!generationConfig.classId && teacherClasses.value.length > 0) {
      generationConfig.classId = teacherClasses.value[0].id
    }
  } catch (error) {
    teacherClasses.value = []
  }
}

function primaryWeakPoint() {
  return weakPoints.value[0] || {}
}

function primaryDimension() {
  return lowestDimensions.value[0] || {}
}

function formatWeakNames(limit = 3) {
  return formatWeakPointNames(weakPoints.value, limit)
}

function formatDimensionNames(limit = 3) {
  return formatResourceDimensionNames(lowestDimensions.value, limit)
}

async function loadCandidateResources() {
  const weak = primaryWeakPoint()
  if (String(selectedStudent.value?.studentId) === String(demoStudent.studentId)) {
    candidateResources.value = demoResources
    return
  }
  try {
    const rows = await learningApi.resources({
      keyword: weak.tagName || selectedStudent.value?.summary || '',
      knowledgePointId: weak.knowledgePointId,
      limit: 6,
    })
    candidateResources.value = Array.isArray(rows) ? rows : []
  } catch (error) {
    candidateResources.value = demoResources
    ElMessage.warning(error.message || '候选资源加载失败，已使用演示候选资源')
  }
}

function buildGeneratedResources() {
  return buildFallbackGeneratedResources({
    student: selectedStudent.value || {},
    weak: primaryWeakPoint(),
    dimension: primaryDimension(),
  })
}

function isEditableAgent(agentId) {
  return editableAgentIds.includes(agentId)
}

function currentAgentProviderLabel(agentId) {
  return buildCurrentAgentProviderLabel(agentId, {
    agentOverrides,
    defaultProviderKey: defaultProviderKey.value,
    providerFieldMap: agentProviderFieldMap,
    providerOptions: providerOptions.value,
  })
}

function providerBindingMode(agentKey) {
  return buildProviderBindingMode(agentKey, {
    agentOverrides,
    defaultProviderKey: defaultProviderKey.value,
  })
}

async function loadProviders() {
  providerLoading.value = true
  try {
    const rows = await teacherApi.teacherLlmProviders({ enabled: true })
    providerOptions.value = Array.isArray(rows) ? rows : []
    const defaultProvider = providerOptions.value.find((item) => item.isDefault) || providerOptions.value[0]
    if (!defaultProviderKey.value && defaultProvider) {
      defaultProviderKey.value = defaultProvider.providerKey
    }
  } catch (error) {
    if (error?.status !== 404) {
      ElMessage.warning(error.message || '加载模型列表失败')
    }
    providerOptions.value = []
  } finally {
    providerLoading.value = false
  }
}

function resourceCardContext() {
  return {
    student: selectedStudent.value || {},
    weakPoints: weakPoints.value,
    selectedStudentId: selectedStudentId.value,
    abilityGaps: lowestDimensions.value,
    generationScope: generationConfig.generationScope,
    exerciseCount: generationConfig.exerciseCount,
    difficulty: generationConfig.difficulty,
    teacherRequirement: teacherRequirement.value,
  }
}

function videoState(card) {
  if (!videoPlayback[card.id]) {
    videoPlayback[card.id] = { sceneIndex: 0, playing: false }
  }
  return videoPlayback[card.id]
}

function currentVideoScene(card) {
  const scenes = Array.isArray(card.videoScenes) ? card.videoScenes : []
  if (!scenes.length) return null
  const state = videoState(card)
  return scenes[Math.min(state.sceneIndex || 0, scenes.length - 1)] || scenes[0]
}

function videoProgress(card) {
  const scenes = Array.isArray(card.videoScenes) ? card.videoScenes : []
  if (!scenes.length) return 0
  return Math.round(((videoState(card).sceneIndex + 1) / scenes.length) * 100)
}

function toggleVideo(card) {
  const state = videoState(card)
  state.playing = !state.playing
}

function selectVideoScene(card, index) {
  const state = videoState(card)
  state.sceneIndex = index
  state.playing = false
}

function nextVideoScene(card) {
  const scenes = Array.isArray(card.videoScenes) ? card.videoScenes : []
  if (!scenes.length) return
  const state = videoState(card)
  state.sceneIndex = (state.sceneIndex + 1) % scenes.length
}

function openRecommendedVideo(video) {
  if (!video?.url) return
  window.open(video.url, '_blank', 'noopener,noreferrer')
}

function watchRecommendedVideo(card) {
  const video = primaryVideo(card)
  if (!video) {
    ElMessage.warning('请先补充具体视频播放页链接')
    return
  }
  openRecommendedVideo(video)
}

async function copyVideoLink(card) {
  const video = primaryVideo(card)
  if (!video?.url) {
    ElMessage.warning('当前还没有具体视频链接')
    return
  }
  await navigator.clipboard.writeText(video.url)
  ElMessage.success('已复制视频链接')
}

async function pasteVideoLink(card) {
  try {
    const { value } = await ElMessageBox.prompt('请粘贴 Bilibili 或 YouTube 的具体视频播放页链接', '补充视频链接', {
      confirmButtonText: '淇濆瓨閾炬帴',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：https://www.bilibili.com/video/BVxxxxxx',
    })
    const url = String(value || '').trim()
    if (!isConcreteVideoUrl(url)) {
      ElMessage.warning(isVideoSearchUrl(url) ? VIDEO_SEARCH_URL_MESSAGE : '请填写具体视频播放页链接')
      return
    }
    const focus = weakPoints.value.map((item) => item.tagName).filter(Boolean)[0] || 'C语言薄弱知识点'
    const video = {
      id: `${card.id}-manual-video`,
      title: card.title.replace(/\s*知识讲解视频推荐$/, '') || `${focus} 知识讲解视频`,
      platform: url.includes('bilibili.com') ? 'Bilibili' : 'YouTube',
      url,
      cover: buildVideoCover(card.title, url.includes('bilibili.com') ? 'Bilibili' : 'YouTube'),
      duration: '教师确认',
      viewCount: '手动补充',
      reason: `教师补充的具体视频播放页，建议用于“${focus}”薄弱点的针对性讲解与复习。`,
      matchedKnowledgePoints: [focus],
      sourceStatus: 'manual',
    }
    card.sourceUrl = url
    card.status = 'approved'
    card.videoConfig = {
      ...(card.videoConfig || {}),
      sourceStatus: 'manual',
      platform: video.platform,
      sourceUrl: url,
    }
    card.videoRecommendations = [video]
    persistWorkspace()
    ElMessage.success('已补充具体视频链接')
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.message || '补充链接失败')
  }
}

function quizAnswerKey(card, index) {
  return `${card.id}-${index}`
}

function quizAnswer(card, index) {
  return quizAnswers[quizAnswerKey(card, index)] || ''
}

function setQuizAnswer(card, index, value) {
  quizAnswers[quizAnswerKey(card, index)] = value
}

function isQuizCorrect(card, question, index) {
  return isQuizAnswerCorrect(quizAnswer(card, index), question)
}

function quizFeedback(card, question, index) {
  return quizFeedbackText(quizAnswer(card, index), question)
}

function downloadRemotionProps(card) {
  const data = buildRemotionProps(card, resourceCardContext())
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = remotionPropsFilename(card)
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
  ElMessage.success('已导出 Remotion 渲染数据')
}

async function copyRemotionCommand(card) {
  const propsFile = remotionPropsFilename(card)
  const outputFile = `${safeFileName(card.title || card.id)}.mp4`
  const command = `npm run remotion:render -- out/${outputFile} --props ${propsFile}`
  try {
    await navigator.clipboard.writeText(command)
    ElMessage.success('已复制 Remotion 渲染命令')
  } catch {
    ElMessage.info(command)
  }
}

async function generateAgentResources(feedback = '') {
  executionStopped.value = false
  const student = selectedStudent.value
  if (!student) {
    throw new Error('请先选择学生')
  }
  if (String(student.studentId).startsWith('DEMO')) {
    throw new Error('演示学生没有真实后端画像，请选择真实学生后生成')
  }
  let task
  const payload = {
    studentId: Number(student.studentId),
    stage: stage.value,
    resourceTypes: generationConfig.selectedResourceTypes?.length ? generationConfig.selectedResourceTypes : defaultResourceTypes,
    selectedResourceTypes: generationConfig.selectedResourceTypes?.length ? generationConfig.selectedResourceTypes : defaultResourceTypes,
    generationScope: generationConfig.generationScope,
    classId: generationConfig.classId,
    difficulty: generationConfig.difficulty,
    exerciseCount: generationConfig.exerciseCount,
    publishMode: generationConfig.publishMode,
    selectedWeakPoints: generationConfig.selectedWeakPoints,
    providerKey: defaultProviderKey.value || undefined,
    agentProviderKeys: buildAgentProviderPayload(agentOverrides),
    teacherRequirement: teacherRequirement.value,
    feedback,
  }
  try {
    task = await teacherApi.startGenerateAgentResourcesTask(payload)
  } catch (error) {
    if (error?.status === 404) {
      const result = await teacherApi.generateAgentResources(payload)
      applyGenerationResult(result || {})
      addLog('report', '当前后端未启用后台任务接口，已回退为当前请求直接生成。')
      return {
        status: 'completed',
        message: '当前后端未启用后台任务接口，已直接生成结果。',
        result,
      }
    }
    throw error
  }
  persistTask(task)
  await pollGenerationTask(true)
  startTaskPolling()
  return task
}

function startTaskPolling() {
  stopTaskPolling()
  taskPollTimer = window.setInterval(() => {
    pollGenerationTask().catch((error) => {
      generationTaskMessage.value = error.message || '网络波动，后台任务仍可能继续执行，请稍后重试查看状态。'
      persistWorkspace()
    })
  }, 3000)
}

async function pollGenerationTask(showToast = false) {
  if (!generationTaskId.value) return null
  const task = await teacherApi.generateAgentResourcesTaskStatus(generationTaskId.value)
  generationTaskStatus.value = task?.status || 'idle'
  generationTaskMessage.value = task?.message || ''
  persistWorkspace()
  if (task?.status === 'completed') {
    stopTaskPolling()
    applyGenerationResult(task.result || {})
    clearTaskPersistence()
    if (showToast) ElMessage.success('个性化资源生成完成')
  } else if (task?.status === 'failed') {
    stopTaskPolling()
    const message = task?.message || '个性化资源生成失败'
    throw new Error(message)
  } else if (task?.status === 'canceled') {
    stopTaskPolling()
    generationTaskMessage.value = task?.message || '任务已停止'
    clearTaskPersistence({ preserveMessage: true })
    persistWorkspace()
    if (showToast) ElMessage.warning(generationTaskMessage.value)
  }
  return task
}

async function stopGenerationTask() {
  executionStopped.value = true
  activeAgentId.value = ''
  flowingEdges.value = []
  gsap.killTweensOf('*')
  if (!generationTaskId.value) return
  try {
    const task = await teacherApi.cancelGenerateAgentResourcesTask(generationTaskId.value)
    generationTaskStatus.value = task?.status || 'canceled'
    generationTaskMessage.value = task?.message || '任务已停止'
    stopTaskPolling()
    addLog('report', generationTaskMessage.value)
    clearTaskPersistence({ preserveMessage: true })
    persistWorkspace()
    ElMessage.warning(generationTaskMessage.value)
  } catch (error) {
    ElMessage.error(error.message || '停止后台任务失败')
  }
}

function restoreGenerationTask() {
  const saved = readStorageJson(TASK_STORAGE_KEY)
  if (!saved) return
  try {
    if (!saved?.taskId) return
    if (saved.studentId && String(saved.studentId) !== String(selectedStudent.value?.studentId || '')) {
      return
    }
    generationTaskId.value = saved.taskId
    generationTaskStatus.value = 'running'
    generationTaskMessage.value = '已恢复后台生成任务，正在同步状态。'
    pollGenerationTask().catch((error) => {
      generationTaskMessage.value = error.message || '恢复后台任务状态失败，请稍后重试。'
      persistWorkspace()
    })
    startTaskPolling()
  } catch (error) {
    generationTaskMessage.value = '后台任务恢复失败，请重新生成。'
  }
}

function traceSummary(agentId, fallback) {
  return buildTraceSummary(lastRun.value, agentId, fallback)
}

async function completeAgent(agentId) {
  let output = ''
  if (agentId === 'preprocess') {
    output = `已标准化 ${selectedStudent.value?.studentName || '学生'} 的画像：${weakPoints.value.length} 个薄弱点，${selectedStudent.value?.dimensions?.length || 0} 个评价维度。`
  } else if (agentId === 'coordinator') {
    output = '已将画像分发给知识诊断、能力分析和错因/行为分析智能体。'
  } else if (agentId === 'knowledge') {
    output = `薄弱知识点优先级：${formatWeakNames()}。`
  } else if (agentId === 'ability') {
    output = `能力短板排序：${formatDimensionNames()}。`
  } else if (agentId === 'behavior') {
    const suggestions = selectedStudent.value?.suggestions || []
    output = suggestions.length
      ? `行为复盘建议：${suggestions.slice(0, 2).join('；')}。`
      : `完成 ${selectedStudent.value?.completedAttemptCount || 0} 次作答，建议补充错题复盘记录。`
  } else if (agentId === 'resource') {
    await loadCandidateResources()
    output = `已召回 ${candidateResources.value.length} 条候选资源，作为个性化资源生成依据。`
  } else if (agentId === 'practice') {
    output = `已按 ${generationConfig.exerciseCount} 题配置生成补救练习与阶段任务。`
  } else if (agentId === 'report') {
    if (String(selectedStudent.value?.studentId).startsWith('DEMO')) {
      generatedResources.value = normalizeGeneratedCards(buildGeneratedResources(), resourceCardContext())
    } else {
      await generateAgentResources()
    }
    output = `资源草案已汇总，当前共有 ${generatedResources.value.length} 个待审核资源。`
  } else if (agentId === 'qualityReview') {
    output = '资源质量审核完成，等待教师确认后保存或发送。'
  } else if (agentId === 'consistencyReview') {
    output = '主题一致性审核完成，资源已对齐学生画像与薄弱点。'
  }
  if (!completedAgents.value.includes(agentId)) {
    completedAgents.value.push(agentId)
  }
  if (output) {
    agentOutputs[agentId] = output
    addLog(agentId, output)
    await addModelConsultationMessage(agentId, output)
  }
  persistWorkspace()
  await nextTick()
}
function reduceMotion() {
  return window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
}

function animateNode(agentId) {
  if (reduceMotion() || !rootRef.value) return Promise.resolve()
  const node = rootRef.value.querySelector(`[data-agent-id="${agentId}"]`)
  if (!node) return Promise.resolve()
  return new Promise((resolve) => {
    gsap.fromTo(
      node,
      { y: 0, scale: 1 },
      { y: -8, scale: 1.03, duration: 0.18, yoyo: true, repeat: 1, ease: 'power2.out', onComplete: resolve },
    )
  })
}

function animateEdges(edgeList) {
  if (reduceMotion() || !rootRef.value || edgeList.length === 0) return Promise.resolve()
  return Promise.all(
    edgeList.map((edge) => {
      const path = rootRef.value.querySelector(`[data-edge-path="${edge.id}"]`)
      const dot = rootRef.value.querySelector(`[data-edge-dot="${edge.id}"]`)
      if (!path || !dot) return Promise.resolve()
      const length = path.getTotalLength()
      const state = { progress: 0 }
      return new Promise((resolve) => {
        gsap.set(dot, { autoAlpha: 1 })
        gsap.to(state, {
          progress: 1,
          duration: 0.85,
          ease: 'power2.inOut',
          onUpdate() {
            const point = path.getPointAtLength(length * state.progress)
            gsap.set(dot, { attr: { cx: point.x, cy: point.y } })
          },
          onComplete() {
            gsap.set(dot, { autoAlpha: 0 })
            resolve()
          },
        })
      })
    }),
  )
}

async function runAgent(agentId) {
  if (executionStopped.value) return false
  if (activeAgentId.value) return false
  if (!isReady(agentId)) {
    ElMessage.warning('请先完成上游智能体')
    return false
  }

  const outgoing = edgesBase.filter((edge) => edge.from === agentId)
  activeAgentId.value = agentId
  flowingEdges.value = outgoing

  try {
    await animateNode(agentId)
    if (executionStopped.value) return

    // 启动线条和圆点动画
    const edgeAnimPromise = animateEdges(outgoing)

    // 等待 1.5 秒展示处理效果
    await new Promise(resolve => setTimeout(resolve, 1500))
    if (executionStopped.value) return

    await edgeAnimPromise
    if (executionStopped.value) return
    await completeAgent(agentId)
    if (executionStopped.value) return

    await nextTick()
    animateGeneratedCards()
    return true
  } catch (error) {
    if (!executionStopped.value) {
      ElMessage.error(error.message || '智能体执行失败')
    }
    return false
  } finally {
    activeAgentId.value = ''
    flowingEdges.value = []
  }
}

function animateGeneratedCards() {
  if (reduceMotion() || !rootRef.value || generatedResources.value.length === 0) return
  gsap.fromTo(
    rootRef.value.querySelectorAll('.resource-card-anim'),
    { autoAlpha: 0, y: 18 },
    { autoAlpha: 1, y: 0, duration: 0.42, stagger: 0.08, ease: 'power2.out', overwrite: 'auto' },
  )
}

async function ensureResourceSaved(card) {
  if (card.saved && card.resourceId) return card.resourceId
  const validation = validateResourceBeforeSave(card, {
    videoSearchUrlMessage: VIDEO_SEARCH_URL_MESSAGE,
  })
  if (!validation.valid) {
    ElMessage.warning(validation.message)
    return null
  }
  savingId.value = card.id
  const resourceId = await learningApi.createResource(buildResourceSavePayload(card, validation.video))
  card.resourceId = resourceId
  card.saved = true
  return resourceId
}

async function sendResourceToTargets(card, resourceId) {
  const studentId = selectedStudent.value?.studentId
  const target = buildRecommendTargetPayload({
    generationScope: generationConfig.generationScope,
    classId: generationConfig.classId,
    studentId: studentId && String(studentId) !== String(demoStudent.studentId) ? studentId : undefined,
  })
  if (!target.valid) {
    ElMessage.warning(target.warning)
    return
  }
  sendingId.value = card.id
  const result = await learningApi.recommendResourceTargets(resourceId, target.payload)
  card.sent = true
  const count = result?.targetCount || 0
  ElMessage.success(target.targetType === 'class' ? `已发送给班级内 ${count} 名学生` : '已发送给该学生')
}

async function saveResource(card, options = {}) {
  try {
    if (!card.saved) {
      const confirm = buildResourceSaveConfirm(card, options)
      await ElMessageBox.confirm(confirm.message, confirm.title, {
        confirmButtonText: confirm.confirmButtonText,
        cancelButtonText: '取消',
        type: 'info',
      })
    }
    const resourceId = await ensureResourceSaved(card)
    if (!resourceId) return
    if (options.send) {
      await sendResourceToTargets(card, resourceId)
    } else {
      ElMessage.success('已保存到学习资源库')
    }
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.message || (options.send ? '发送失败' : '保存失败'))
  } finally {
    savingId.value = ''
    sendingId.value = ''
  }
}
async function reviseResource(card) {
  try {
    const { value } = await ElMessageBox.prompt(
      `请说明“${card.title}”需要如何修改，系统会带着反馈重新生成并重新审核。`,
      '退回修改',
      {
        confirmButtonText: '重新生成',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '例如：降低难度，增加指针图解，减少综合项目篇幅。',
        inputValue: revisionFeedback.value,
      },
    )
    if (!value || !value.trim()) {
      ElMessage.warning('请填写修改意见')
      return
    }
    revisionDraft.value = card
    revisionFeedback.value = value.trim()
    const feedback = buildRevisionFeedback(card, revisionFeedback.value)
    await generateAgentResources(feedback)
    completedAgents.value = ['preprocess', 'coordinator', 'knowledge', 'ability', 'behavior', 'resource', 'practice', 'report']
    agentOutputs.report = '已根据退回意见重新提交后端生成任务，新的资源草案完成后将自动刷新。'
    addLog('report', agentOutputs.report)
    ElMessage.success('已按退回意见重新提交后端生成任务')
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.message || '重新生成失败')
  } finally {
    revisionDraft.value = null
  }
}
watch(selectedStudentId, (value, oldValue) => {
  if (oldValue && value !== oldValue && !generationTaskId.value) {
    resetFlow()
  }
  persistWorkspace()
})

watch(providerOptions, (rows) => {
  const keys = new Set((rows || []).map((item) => item.providerKey))
  if (defaultProviderKey.value && !keys.has(defaultProviderKey.value)) {
    defaultProviderKey.value = ''
  }
  Object.keys(agentOverrides).forEach((agentKey) => {
    if (agentOverrides[agentKey] && !keys.has(agentOverrides[agentKey])) {
      agentOverrides[agentKey] = ''
    }
  })
  persistWorkspace()
}, { deep: true })

watch([teacherRequirement, defaultProviderKey], () => {
  persistWorkspace()
})

watch(generatedResources, (rows) => {
  if (normalizingGeneratedResources || !Array.isArray(rows) || rows.length === 0 || hasExternalVideoResource(rows)) {
    return
  }
  normalizingGeneratedResources = true
  generatedResources.value = ensureVideoResource(rows, resourceCardContext())
  persistWorkspace()
  nextTick(() => {
    normalizingGeneratedResources = false
  })
}, { deep: true })

onMounted(async () => {
  componentMounted = true
  window.addEventListener('teacher-agent-resource-workspace-updated', handleWorkspaceUpdated)
  restoreWorkspace()
  restoreLogs()
  if (rootRef.value) {
    ctx = gsap.context(() => {
      if (reduceMotion()) return
      gsap.from('.agent-hero', { autoAlpha: 0, y: 18, duration: 0.5, ease: 'power2.out' })
      gsap.from('.agent-node', { autoAlpha: 0, y: 20, duration: 0.5, stagger: 0.05, ease: 'power2.out', delay: 0.08 })
    }, rootRef.value)
  }
  await Promise.all([loadStudents(), loadProviders(), loadTeacherClasses()])
  if (lastRun.value) {
    syncGeneratedOutputs()
  }
  restoreGenerationTask()
  await resumePendingConsultationPlayback()
  videoTimer = window.setInterval(() => {
    generatedResources.value.forEach((card) => {
      const state = videoPlayback[card.id]
      if (card.isVideo && state?.playing) nextVideoScene(card)
    })
  }, 2600)
})

onUnmounted(() => {
  componentMounted = false
  meetingPlaybackToken.value += 1
  persistWorkspace()
  window.removeEventListener('teacher-agent-resource-workspace-updated', handleWorkspaceUpdated)
  stopTaskPolling()
  if (videoTimer) window.clearInterval(videoTimer)
  ctx?.revert()
})
</script>

<template>
  <div ref="rootRef" v-loading="loading" class="agent-resource-page">
    
    <section class="agent-hero">
      <div>
        <p class="eyebrow">Mapis-style Agent Workflow</p>
        <h1>智能体资源生成</h1>
        <p class="hero-copy">以学生画像为输入，点击智能体后触发数据流动，并逐步生成多个个性化学习资源。</p>
      </div>
      <div class="hero-stats">
        <div v-for="item in profileStats" :key="item.label" class="stat-pill">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </div>
    </section>

    <section class="control-strip">
      <el-segmented v-model="stage" :options="stageOptions" @change="loadStudents" />
      <el-select v-model="selectedStudentId" filterable placeholder="选择学生" class="student-select">
        <el-option
          v-for="student in students"
          :key="student.studentId"
          :label="`${student.studentName || '学生'} (${student.studentId})`"
          :value="String(student.studentId)"
        />
      </el-select>
      <el-input v-model="studentId" clearable placeholder="按学生 ID 查询" class="student-id-input" @keyup.enter="loadStudents" />
      <el-input v-model="teacherRequirement" clearable placeholder="教师补充要求，如重点补强指针" class="requirement-input" />
      <span class="provider-summary">{{ providerSummaryText }}</span>
      <el-button type="primary" :icon="Search" @click="loadStudents">查询画像</el-button>
      <el-button :icon="Setting" :loading="providerLoading" @click="providerDrawerVisible = true">模型配置</el-button>
      <el-button :icon="UserFilled" @click="loadDemoData()">载入演示数据</el-button>
      <el-button
        type="success"
        plain
        :icon="Finished"
        :loading="meetingLoading"
        :disabled="!selectedStudent || meetingLoading"
        @click="openConsultationWindow(true)"
      >
        开始会诊
      </el-button>
      <el-button
        v-if="generationTaskId && ['queued', 'running'].includes(generationTaskStatus)"
        type="danger"
        plain
        :icon="SwitchButton"
        @click="stopGenerationTask"
      >
        停止生成
      </el-button>
      <el-button :icon="Refresh" @click="resetFlowAndTask">重置流程</el-button>
    </section>

    <section class="generation-config-strip">
      <el-segmented
        v-model="generationConfig.generationScope"
        :options="[
          { label: '单个学生', value: 'student' },
          { label: '班级共性', value: 'class' },
        ]"
      />
      <el-select
        v-if="generationConfig.generationScope === 'class'"
        v-model="generationConfig.classId"
        class="config-select class-target-select"
        placeholder="选择投放班级"
      >
        <el-option
          v-for="clazz in teacherClasses"
          :key="clazz.id"
          :label="`${clazz.className || '班级'} (${clazz.classCode || clazz.id})`"
          :value="clazz.id"
        />
      </el-select>
      <el-select v-model="generationConfig.difficulty" class="config-select" placeholder="难度">
        <el-option label="基础补弱" value="basic" />
        <el-option label="提升训练" value="improve" />
        <el-option label="综合迁移" value="comprehensive" />
      </el-select>
      <el-input-number v-model="generationConfig.exerciseCount" :min="1" :max="10" controls-position="right" />
      <el-select v-model="generationConfig.publishMode" class="config-select" placeholder="发布模式">
        <el-option label="只生成草稿" value="draft_only" />
        <el-option label="审核后发布" value="publish_after_review" />
      </el-select>
      <el-select v-model="generationConfig.selectedResourceTypes" multiple collapse-tags class="resource-type-select" placeholder="资源类型">
        <el-option label="知识讲解视频" value="knowledge_video" />
        <el-option label="补救练习" value="remedial_exercise" />
        <el-option label="知识点讲义" value="knowledge_handout" />
        <el-option label="错因复盘" value="error_reflection" />
        <el-option label="学习路径" value="learning_path" />
      </el-select>
    </section>

    <el-alert
      v-if="generationTaskMessage"
      :title="generationTaskMessage"
      :type="generationTaskStatus === 'failed' ? 'error' : generationTaskStatus === 'canceled' ? 'warning' : 'info'"
      show-icon
      :closable="false"
    />

    <section class="agent-guide">
      <article v-for="agentId in agentInfoCards" :key="agentId" class="guide-card">
        <span class="guide-avatar">
          <img
            v-if="agents[agentId].image"
            :src="agents[agentId].image"
            :alt="agents[agentId].title"
            :style="{ objectPosition: agents[agentId].imagePosition || '50% center' }"
          />
          <el-icon v-else><component :is="agents[agentId].icon" /></el-icon>
        </span>
        <div>
          <strong>{{ agents[agentId].title }}</strong>
          <small>{{ agents[agentId].cnTitle }} / {{ agents[agentId].subtitle }}</small>
          <p>{{ agents[agentId].note }}</p>
          <p v-if="isEditableAgent(agentId)" class="guide-config-text">模型：{{ currentAgentProviderLabel(agentId) }}</p>
          <p v-else class="guide-config-text is-muted">流程节点 / 规则节点，不支持模型配置</p>
          <el-button v-if="isEditableAgent(agentId)" link type="primary" @click="providerDrawerVisible = true">配置模型</el-button>
        </div>
      </article>
    </section>

    <!-- 学科知识图谱（替换协同画布） -->
    <section class="knowledge-graph-section">
      <div class="kg-layout">
        <!-- 左侧：知识图谱 -->
        <div class="kg-left-panel">
          <WeakPointsKnowledgeGraph
            :weak-points="kgWeakPoints"
            :center="kgCenter"
            :groups="kgGroups"
            :points="kgPoints"
            @select-point="handleKgPointSelect"
          />
        </div>
        
        <!-- 右侧：个性化路径生成过程 + 个性化资源输出 -->
        <div class="kg-right-panel">
          <div class="process-flow">
            <h3 class="process-title">个性化路径生成过程</h3>
            
            <div class="process-steps">
              <!-- 步骤 1：画像分析 -->
              <div class="process-step" :class="{'active': completedAgents.includes('preprocess'), 'complete': completedAgents.includes('preprocess')}">
                <div class="step-icon">📊</div>
                <div class="step-content">
                  <h4>Step 1: 画像分析</h4>
                  <p>提取 {{ weakPoints.length }} 个薄弱知识点，{{ lowestDimensions.length }} 个能力维度</p>
                  <ul v-if="weakPoints.length > 0" class="weak-point-list">
                    <li v-for="wp in weakPoints.slice(0, 3)" :key="wp.knowledgePointId">• {{ wp.tagName || wp.name }}</li>
                  </ul>
                </div>
                <el-icon v-if="completedAgents.includes('preprocess')"><Finished /></el-icon>
              </div>
              
              <!-- 步骤 2：知识诊断 -->
              <div class="process-step" :class="{'active': completedAgents.includes('knowledge'), 'complete': completedAgents.includes('knowledge')}">
                <div class="step-icon">🔬</div>
                <div class="step-content">
                  <h4>Step 2: 知识诊断</h4>
                  <p>识别薄弱点优先级与因果关系链</p>
                </div>
                <el-icon v-if="completedAgents.includes('knowledge')"><Finished /></el-icon>
              </div>
              
              <!-- 步骤 3：资源推荐 -->
              <div class="process-step" :class="{'active': completedAgents.includes('resource'), 'complete': generatedResources.length > 0}">
                <div class="step-icon">📚</div>
                <div class="step-content">
                  <h4>Step 3: 资源推荐</h4>
                  <p>生成 {{ generatedResources.length }} 个个性化学习资源</p>
                  <div v-if="generatedResources.length > 0" class="resource-preview">
                    <el-tag v-for="resType in [...new Set(generatedResources.map(r => r.resourceType))].slice(0, 3)" :key="resType" size="small" type="success">
                      {{ resType }}
                    </el-tag>
                  </div>
                </div>
                <el-icon v-if="generatedResources.length > 0"><Finished /></el-icon>
              </div>
              
              <!-- 步骤 4：质量审核 -->
              <div class="process-step" :class="{'active': completedAgents.includes('qualityReview'), 'complete': generatedResources.every(r => r.saved || r.status === 'approved')}">
                <div class="step-icon">✅</div>
                <div class="step-content">
                  <h4>Step 4: 质量审核</h4>
                  <p>确保资源完整、可用、难度适配</p>
                </div>
                <el-icon v-if="generatedResources.every(r => r.saved || r.status === 'approved')"><Finished /></el-icon>
              </div>
            </div>
            
            <!-- 当前状态 -->
            <div class="process-status">
              <div class="status-badge" :class="consultationState">
                <span class="status-icon">⚡</span>
                <span class="status-text">
                  {{ consultationState === 'running' ? '智能体协同中...' : 
                     consultationState === 'completed' ? '路径生成完成' : 
                     consultationState === 'paused_for_teacher' ? '等待教师确认' : 
                     '等待启动' }}
                </span>
              </div>
            </div>
          </div>
          <div class="resource-output-section">
            <div class="section-header">
              <h3>个性化资源输出</h3>
              <span class="count-badge">{{ generatedResources.length }}</span>
            </div>
            
            <div v-if="generatedResources.length === 0" class="empty-state">
              <el-empty description="点击“开始会诊”后开始生成资源" :image-size="60" />
            </div>
            
            <div v-else class="resource-list">
              <article v-for="card in generatedResources" :key="card.id" class="resource-item">
                <div class="resource-header">
                  <el-tag effect="dark" round>{{ card.sourceAgent }}</el-tag>
                  <el-tag :type="card.isMultimodal ? 'success' : 'info'" round>{{ card.resourceTypeLabel }}</el-tag>
                </div>
                <h4>{{ card.title }}</h4>
                <p class="resource-summary">{{ card.summary }}</p>
                
                <!-- 关联的薄弱知识点 -->
                <div v-if="card.personalizationBasis?.['薄弱知识点']" class="linked-weak-points">
                  <el-tag v-for="point in card.personalizationBasis['薄弱知识点'].split(/[,,]/).map(s => s.trim())" 
                          :key="point" 
                          size="small" 
                          type="danger" 
                          effect="plain"
                  >
                    {{ point }}
                  </el-tag>
                </div>
                
                <!-- 操作按钮 -->
                <div class="resource-actions-mini">
                  <el-button v-if="!card.sent" size="small" type="primary" plain @click="saveResource(card)">保存</el-button>
                  <el-button v-if="!card.sent" size="small" success plain @click="saveResource(card, { send: true })">发送</el-button>
                  <el-button size="small" @click="reviseResource(card)">退回</el-button>
                </div>
              </article>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 处理日志 -->
    <section class="log-section">
      <div class="log-header">
        <h2>学习诊断处理日志</h2>
        <span>{{ logs.length }} 条记录</span>
      </div>
      <div class="log-grid">
        <div v-if="logs.length === 0" class="log-empty">
          <el-empty description="点击智能体后显示详细处理日志" :image-size="60" />
        </div>
        <ul v-else class="log-list">
          <li v-for="log in logs" :key="log.id">
            <time>{{ log.time }}</time>
            <strong>{{ log.agent }}</strong>
            <p>{{ log.content }}</p>
            <el-button text type="danger" size="small" :icon="Delete" @click="deleteLog(log.id)">删除</el-button>
          </li>
        </ul>
      </div>
    </section>

    <section class="agent-results-section">
      <div v-if="false" class="consultation-panel">
        <div class="section-title">
          <div class="section-title-main">
            <h2>多智能体会诊讨论</h2>
            <span>{{ consultationMessages.length }} 条对话</span>
          </div>
          <div class="section-title-actions">
            <el-button
              size="small"
              type="primary"
              plain
              :loading="meetingLoading"
              :disabled="!selectedStudent || meetingLoading"
              @click="startConsultationMeeting"
            >
              {{ consultationState === 'completed' ? '重新会诊' : '开始会诊' }}
            </el-button>
            <el-tag :type="consultationDecision.status === 'ready' ? 'success' : 'warning'" effect="plain" round>
              {{
                consultationState === 'running' || consultationState === 'resuming'
                  ? '自动会诊中'
                  : consultationState === 'paused_for_teacher'
                    ? '等待教师补充'
                    : consultationState === 'refining'
                      ? '二次优化'
                      : consultationDecision.status === 'ready'
                        ? '会诊已生成'
                        : liveConsultationMessages.length ? '实时会诊' : '等待会诊'
              }}
            </el-tag>
          </div>
        </div>
        <el-tabs v-model="activeConsultationTab" class="consultation-tabs">
          <el-tab-pane label="会诊讨论" name="discussion">
            <el-empty v-if="consultationMessages.length === 0" description="点击“开始会诊对话”后展示智能体之间的讨论过程" :image-size="86" />
            <div v-else ref="consultationThreadRef" class="consultation-thread">
              <article
                v-for="message in consultationMessages"
                :key="message.id"
                class="consultation-message"
                :class="{ 'is-teacher': message.agentId === 'teacher', 'is-decision': message.agentId === 'decision-agent' || message.agentId === 'report', 'is-running': message.status === 'running', 'is-fallback': message.status === 'fallback' }"
              >
                <div class="consultation-avatar">{{ message.agentId === 'decision-agent' || message.agentId === 'report' ? '会' : message.agentName.slice(0, 1) }}</div>
                <div class="consultation-bubble">
                  <div class="consultation-message-head">
                    <div>
                      <strong>{{ message.agentName }}</strong>
                      <em>{{ message.role }}</em>
                      <small v-if="message.replyToAgentName" class="consultation-reply">回应 {{ message.replyToAgentName }}</small>
                    </div>
                    <span>
                      第 {{ message.round }} 轮{{ message.createdAt ? ` · ${message.createdAt}` : '' }}
                      {{ message.status === 'running' ? ' · 模型生成中' : message.modelName ? ` · ${message.modelName}` : '' }}
                    </span>
                  </div>
                  <p>{{ message.content }}</p>
                  <div v-if="message.evidenceRefs.length" class="consultation-tags">
                    <el-tag v-for="item in message.evidenceRefs" :key="`${message.id}-${item}`" size="small" round>
                      {{ item }}
                    </el-tag>
                    <el-tag v-if="message.llmCallId" size="small" type="info" round>调用 {{ message.llmCallId }}</el-tag>
                    <el-tag v-if="message.status === 'fallback'" size="small" type="warning" round>待复核</el-tag>
                  </div>
                </div>
              </article>
            </div>
            <div v-if="pausedAgentId" class="consultation-followup">
              <div class="followup-head">
                <strong>{{ consultationState === 'refining' ? '二次追问' : '暂停后补充' }}：{{ agentDisplayName(pausedAgentId) }}</strong>
                <span>提交后将从该智能体开始，继续让后续智能体讨论并更新资源方案。</span>
              </div>
              <el-input
                v-model="followUpText"
                type="textarea"
                :rows="3"
                maxlength="500"
                show-word-limit
                resize="none"
                :placeholder="`请输入对 ${agentDisplayName(pausedAgentId)} 的补充或追问，例如：把数组越界和字符串结束符讲得更细。`"
                @keydown.ctrl.enter.prevent="submitAgentFollowUp"
              />
              <div class="followup-actions">
                <el-button @click="cancelFollowUp">取消</el-button>
                <el-button type="primary" :loading="meetingLoading" @click="submitAgentFollowUp">提交并继续会诊</el-button>
              </div>
            </div>
          </el-tab-pane>
          <el-tab-pane label="画像证据" name="evidence">
            <div v-if="consultationEvidenceLinks.length" class="evidence-link-grid">
              <article v-for="item in consultationEvidenceLinks" :key="item.id" class="evidence-link-card">
                <div class="evidence-link-head">
                  <el-tag size="small" type="success" effect="plain" round>{{ item.type }}</el-tag>
                  <strong>{{ item.title }}</strong>
                </div>
                <p class="evidence-link-conclusion">{{ item.conclusion }}</p>
                <div class="evidence-link-columns">
                  <section>
                    <span>对应画像数据</span>
                    <ul>
                      <li v-for="value in item.profileData" :key="`${item.id}-profile-${value}`">{{ value }}</li>
                    </ul>
                  </section>
                  <section>
                    <span>会议对应依据</span>
                    <ul>
                      <li v-for="value in item.meetingBasis" :key="`${item.id}-meeting-${value}`">{{ value }}</li>
                      <li v-if="!item.meetingBasis.length">等待后续智能体补充会议依据</li>
                    </ul>
                  </section>
                </div>
                <div class="evidence-link-action">
                  <span>后续动作</span>
                  <strong>{{ item.action }}</strong>
                </div>
              </article>
            </div>
            <el-empty v-else description="暂无可对应的画像证据，开始会诊后自动生成证据链" :image-size="90" />
          </el-tab-pane>
          <el-tab-pane label="会议结果" name="decision">
            <div class="meeting-result-card">
              <div class="meeting-result-hero">
                <el-tag type="success" effect="dark" round>最终会议结果</el-tag>
                <h3>{{ finalMeetingResult.headline }}</h3>
                <p>基于 {{ finalMeetingResult.rounds }} 轮、{{ finalMeetingResult.agentCount }} 个智能体的会诊意见，系统已将知识诊断、能力评估、资源生成和审核意见整理为可执行方案。</p>
                <div class="meeting-result-stats">
                  <span>{{ finalMeetingResult.evidenceCount }} 条依据</span>
                  <span>{{ finalMeetingResult.resourceCount }} 类资源</span>
                  <span>{{ finalMeetingResult.status === 'ready' ? '已形成共识' : '待教师复核' }}</span>
                </div>
              </div>
              <section class="meeting-result-section">
                <h4>一、最终共识</h4>
                <ul>
                  <li v-for="item in finalMeetingResult.consensus" :key="item">{{ item }}</li>
                </ul>
              </section>
              <section class="meeting-result-section">
                <h4>二、个性化资源方案</h4>
                <div class="meeting-resource-plan">
                  <article v-for="item in finalMeetingResult.resourcePlan" :key="item.title">
                    <strong>{{ item.title }}</strong>
                    <span>{{ item.detail }}</span>
                  </article>
                </div>
              </section>
              <section class="meeting-result-section">
                <h4>三、执行路径</h4>
                <ol>
                  <li v-for="item in finalMeetingResult.actionPlan" :key="item">{{ item }}</li>
                </ol>
              </section>
              <section class="meeting-result-section">
                <h4>四、审核与复核点</h4>
                <div class="meeting-review-tags">
                  <el-tag v-for="item in finalMeetingResult.reviewPoints" :key="item" type="success" effect="plain" round>
                    {{ item }}
                  </el-tag>
                </div>
              </section>
              <section class="meeting-result-section">
                <h4>五、会议整理摘要</h4>
                <div class="meeting-digest-list">
                  <article v-for="item in finalMeetingResult.digest" :key="item.title">
                    <strong>{{ item.title }}</strong>
                    <p>{{ item.content }}</p>
                  </article>
                </div>
              </section>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <div class="resource-panel" :class="{ 'is-targeted': generatedResources.length > 0 }">
        <div class="section-title">
          <h2>个性化资源生成结果</h2>
          <span>{{ generatedResources.length }} 个</span>
        </div>
        <el-empty v-if="generatedResources.length === 0" description="完成资源生成智能体后展示结果" :image-size="88" />
        <div v-else class="resource-grid">
          <article v-for="card in generatedResources" :key="card.id" class="resource-card">
            <div class="resource-card-head">
              <el-tag effect="dark" round>{{ card.sourceAgent }}</el-tag>
              <el-tag :type="card.isMultimodal ? 'success' : 'info'" round>{{ card.resourceTypeLabel }}</el-tag>
              <el-tag v-if="card.isMultimodal" type="primary" effect="plain" round>多模态</el-tag>
              <el-tag :type="card.status === 'approved' ? 'success' : 'warning'" round>
                {{ card.status === 'approved' ? '审核通过' : '需修改' }}
              </el-tag>
            </div>
            <h3>{{ card.title }}</h3>
            <p class="resource-summary">{{ card.summary }}</p>

            <div v-if="card.isVideo && currentVideoScene(card)" class="video-preview">
              <div class="video-player-bar">
                <span class="video-dot red"></span>
                <span class="video-dot yellow"></span>
                <span class="video-dot green"></span>
                <strong>知识点讲解视频</strong>
              </div>
              <div class="video-stage">
                <div class="video-visual">
                  <span>{{ currentVideoScene(card).title }}</span>
                  <strong>{{ currentVideoScene(card).boardText }}</strong>
                </div>
                <div class="video-caption">
                  {{ currentVideoScene(card).narration }}
                </div>
              </div>
              <div class="video-controls">
                <el-button size="small" type="primary" @click="toggleVideo(card)">
                  {{ videoState(card).playing ? '暂停' : '播放' }}
                </el-button>
                <el-button size="small" @click="nextVideoScene(card)">下一幕</el-button>
                <el-progress :percentage="videoProgress(card)" :show-text="false" />
                <span>{{ videoState(card).sceneIndex + 1 }} / {{ card.videoScenes.length }}</span>
              </div>
              <div class="video-scenes">
                <button
                  v-for="(scene, index) in card.videoScenes"
                  :key="`${card.id}-scene-${index}`"
                  type="button"
                  :class="{ active: videoState(card).sceneIndex === index }"
                  @click="selectVideoScene(card, index)"
                >
                  {{ index + 1 }}. {{ scene.title }}
                </button>
              </div>
            </div>
            <div v-else-if="card.isVideo && (card.sourceUrl || card.videoConfig?.sourceStatus === 'fallback' || card.videoRecommendations?.length)" class="video-preview existing-video-preview">
              <div class="video-player-bar">
                <span class="video-dot red"></span>
                <span class="video-dot yellow"></span>
                <span class="video-dot green"></span>
                <strong>外部知识讲解视频片段</strong>
              </div>
              <div class="video-stage">
                <div class="video-visual external-video-visual">
                  <span>{{ card.videoConfig?.query || 'C语言知识点讲解' }}</span>
                  <strong>{{ card.title }}</strong>
                </div>
                <div class="video-caption">
                  系统已根据学生薄弱点匹配具体知识讲解视频，教师可预览、替换或补充视频链接后推荐给学生。
                </div>
              </div>
              <div v-if="card.videoRecommendations?.length && hasConcreteVideo(card)" class="video-recommendation-grid">
                <article
                  v-for="video in card.videoRecommendations"
                  :key="video.id"
                  class="video-recommendation-card"
                  role="button"
                  tabindex="0"
                  @click="openRecommendedVideo(video)"
                  @keydown.enter.prevent="openRecommendedVideo(video)"
                >
                  <img :src="video.cover" :alt="video.title" loading="lazy" />
                  <div class="video-recommendation-body">
                    <div class="video-recommendation-meta">
                      <span>{{ video.platform }}</span>
                      <small>{{ video.duration }} · {{ video.viewCount }}</small>
                    </div>
                    <strong>{{ video.title }}</strong>
                    <p>{{ video.reason }}</p>
                    <div class="video-knowledge-tags">
                      <span v-for="point in video.matchedKnowledgePoints" :key="`${video.id}-${point}`">{{ point }}</span>
                    </div>
                    <el-button size="small" type="primary" @click.stop="openRecommendedVideo(video)">观看推荐视频</el-button>
                  </div>
                </article>
              </div>
              <div class="external-video-actions">
                <template v-if="hasConcreteVideo(card)">
                  <el-button type="primary" @click="watchRecommendedVideo(card)">观看推荐视频</el-button>
                  <el-button plain @click="pasteVideoLink(card)">更换视频</el-button>
                  <el-button plain @click="copyVideoLink(card)">复制链接</el-button>
                </template>
                <template v-else>
                  <el-button
                    v-for="item in externalVideoLinks(card)"
                    :key="item.label"
                    type="primary"
                    tag="a"
                    :href="item.url"
                    target="_blank"
                    rel="noreferrer"
                  >
                    搜索候选视频
                  </el-button>
                  <el-button plain @click="pasteVideoLink(card)">粘贴视频链接</el-button>
                  <el-button plain disabled>暂存待完善</el-button>
                </template>
              </div>
            </div>
            <div v-if="card.isQuiz && (card.quizQuestions.length || card.exerciseQuestions.length)" class="quiz-preview">
              <div class="quiz-preview-head">
                <strong>互动测验题目</strong>
                <span>{{ quizDisplayQuestions(card).length }} 题</span>
              </div>
              <div class="quiz-question" v-for="(question, index) in quizDisplayQuestions(card)" :key="`${card.id}-quiz-${index}`">
                <div class="quiz-question-title">
                  <span>{{ index + 1 }}</span>
                  <strong>{{ question.stem }}</strong>
                </div>
                <div v-if="question.options?.length" class="quiz-options">
                  <button
                    v-for="(option, optionIndex) in question.options"
                    :key="`${card.id}-quiz-${index}-${optionIndex}`"
                    type="button"
                    :class="{ active: normalizeAnswer(quizAnswer(card, index)) === String.fromCharCode(65 + optionIndex) }"
                    @click="setQuizAnswer(card, index, String.fromCharCode(65 + optionIndex))"
                  >
                    {{ option }}
                  </button>
                </div>
                <el-input
                  v-else
                  :model-value="quizAnswer(card, index)"
                  size="small"
                  placeholder="请输入答案"
                  @update:model-value="setQuizAnswer(card, index, $event)"
                />
                <div v-if="quizFeedback(card, question, index)" class="quiz-feedback" :class="{ correct: isQuizCorrect(card, question, index) }">
                  {{ quizFeedback(card, question, index) }}
                </div>
              </div>
            </div>

            <el-collapse class="resource-detail" accordion>
              <el-collapse-item title="资源内容" name="content">
                <div v-if="card.structuredContent?.sections?.length" class="structured-resource-content">
                  <section v-for="section in card.structuredContent.sections" :key="section.title" class="structured-resource-section">
                    <h4>{{ section.title }}</h4>
                    <ul>
                      <li v-for="item in section.items" :key="item">{{ item }}</li>
                    </ul>
                  </section>
                </div>
                <div v-else class="resource-content">{{ card.content }}</div>
              </el-collapse-item>
              <el-collapse-item title="个性化依据" name="basis">
                <dl class="meta-list">
                  <template v-for="(value, key) in card.personalizationBasis" :key="key">
                    <dt>{{ key }}</dt>
                    <dd>{{ Array.isArray(value) ? value.join('、') : value }}</dd>
                  </template>
                </dl>
              </el-collapse-item>
              <el-collapse-item title="审核报告" name="review">
                <div class="score-row">
                  <span>质量 {{ card.reviewReport?.qualityScore ?? '-' }}</span>
                  <span>相关 {{ card.reviewReport?.relevanceScore ?? '-' }}</span>
                  <span>一致 {{ card.reviewReport?.consistencyScore ?? '-' }}</span>
                </div>
                <p class="review-comment">{{ card.reviewReport?.comments || '暂无审核意见' }}</p>
                <ul v-if="card.reviewReport?.revisionSuggestions?.length" class="suggestion-list">
                  <li v-for="item in card.reviewReport.revisionSuggestions" :key="item">{{ item }}</li>
                </ul>
              </el-collapse-item>
              <el-collapse-item title="模型来源" name="model">
                <dl class="meta-list">
                  <dt>生成模型</dt>
                  <dd>
                    {{ card.modelSource?.generatorModel || '-' }}
                    <small class="meta-hint">{{ providerBindingMode('generator') }}</small>
                  </dd>
                  <dt>质量审核模型</dt>
                  <dd>
                    {{ card.modelSource?.qualityReviewerModel || '-' }}
                    <small class="meta-hint">{{ providerBindingMode('qualityReviewer') }}</small>
                  </dd>
                  <dt>一致性审核模型</dt>
                  <dd>
                    {{ card.modelSource?.consistencyReviewerModel || '-' }}
                    <small class="meta-hint">{{ providerBindingMode('consistencyReviewer') }}</small>
                  </dd>
                  <dt>调用 ID</dt>
                  <dd>{{ card.modelSource?.llmCallIds?.join('、') || '-' }}</dd>
                </dl>
              </el-collapse-item>
            </el-collapse>

            <div class="teacher-review-box">
              <el-checkbox v-model="card.reviewState.suitabilityApproved">适合当前对象</el-checkbox>
              <el-checkbox v-model="card.reviewState.usabilityApproved">可直接使用</el-checkbox>
              <el-input
                v-model="card.reviewState.teacherComment"
                size="small"
                clearable
                placeholder="教师审核备注，可选"
              />
            </div>

            <div class="resource-actions">
              <el-button v-if="card.isVideo && card.videoScenes?.length" type="success" plain @click="downloadRemotionProps(card)">
                导出 Remotion 数据
              </el-button>
              <el-button v-if="card.isVideo && card.videoScenes?.length" plain @click="copyRemotionCommand(card)">
                复制渲染命令
              </el-button>
              <el-button
                type="primary"
                :icon="card.saved ? Finished : DocumentChecked"
                :loading="savingId === card.id"
                :disabled="card.saved || !canSaveGeneratedResource(card)"
                @click="saveResource(card)"
              >
                {{ card.saved ? '已保存' : '保存到资源库' }}
              </el-button>
              <el-button
                type="success"
                plain
                :loading="sendingId === card.id"
                :disabled="card.sent || !canSaveGeneratedResource(card)"
                @click="saveResource(card, { send: true })"
              >
                {{ card.sent ? '已发送' : '发送到学生端' }}
              </el-button>
              <el-button :icon="Refresh" @click="reviseResource(card)">退回修改</el-button>
            </div>
          </article>
        </div>
      </div>
    </section>

    <button
      v-if="consultationDockVisible"
      type="button"
      class="consultation-dock-tab"
      @click="openConsultationWindow(false)"
    >
      <span>‹</span>
      <strong>会诊窗口</strong>
      <small>{{ consultationState === 'running' || meetingLoading ? '进行中' : `${consultationMessages.length} 条` }}</small>
    </button>

    <Teleport to="body">
      <section v-if="consultationWindowOpen" class="consultation-window-shell">
        <div class="consultation-window">
          <header class="consultation-window-head">
            <div>
              <p class="eyebrow">Live Agent Consultation</p>
              <h2>多智能体会诊讨论</h2>
              <span>按照流程节点依次会诊；点击节点可暂停并向对应智能体补充或追问。</span>
            </div>
            <div class="consultation-window-actions">
              <el-tag :type="consultationDecision.status === 'ready' ? 'success' : 'warning'" effect="plain" round>
                {{
                  consultationState === 'running' || consultationState === 'resuming'
                    ? '自动会诊中'
                    : consultationState === 'paused_for_teacher'
                      ? '等待教师补充'
                      : consultationState === 'refining'
                        ? '二次优化'
                        : consultationDecision.status === 'ready'
                          ? '会诊已生成'
                          : liveConsultationMessages.length ? '实时会诊' : '等待会诊'
                }}
              </el-tag>
              <el-button
                type="primary"
                plain
                :loading="meetingLoading"
                :disabled="!selectedStudent || meetingLoading"
                @click="startConsultationMeeting"
              >
                {{ consultationState === 'completed' ? '重新会诊' : '开始会诊' }}
              </el-button>
              <button type="button" class="consultation-window-close" @click="closeConsultationWindow">关闭</button>
            </div>
          </header>

          <div class="consultation-window-body">
            <aside class="consultation-stage-rail">
              <div class="stage-rail-title">
                <strong>会诊流程</strong>
                <span>{{ completedAgents.length }} / {{ agentInfoCards.length }}</span>
              </div>
              <button
                v-for="agentId in agentInfoCards"
                :key="`window-${agentId}`"
                type="button"
                class="stage-agent-item"
                :class="{ active: isActive(agentId), complete: isCompleted(agentId), paused: pausedAgentId === agentId }"
                @click="handleAgentClick(agentId)"
              >
                <span>{{ agents[agentId].cnTitle.slice(0, 1) }}</span>
                <div>
                  <strong>{{ agents[agentId].title }}</strong>
                  <small>{{ agents[agentId].cnTitle }}</small>
                </div>
              </button>
            </aside>

            <main class="consultation-window-main">
              <div class="section-title consultation-window-title">
                <div class="section-title-main">
                  <h2>会诊对话</h2>
                  <span>{{ consultationMessages.length }} 条对话</span>
                </div>
                <div class="section-title-actions">
                  <el-tag v-if="pausedAgentId" type="warning" effect="plain" round>
                    正在等待 {{ agentDisplayName(pausedAgentId) }} 的补充
                  </el-tag>
                  <el-tag v-else-if="meetingLoading" type="success" effect="plain" round>模型生成中</el-tag>
                </div>
              </div>

              <el-tabs v-model="activeConsultationTab" class="consultation-tabs consultation-window-tabs">
                <el-tab-pane label="会诊讨论" name="discussion">
                  <el-empty v-if="consultationMessages.length === 0" description="点击“开始会诊”后展示智能体之间的连续讨论过程" :image-size="96" />
                  <div v-else ref="consultationThreadRef" class="consultation-thread">
                    <article
                      v-for="message in consultationMessages"
                      :key="message.id"
                      class="consultation-message"
                      :class="{ 'is-teacher': message.agentId === 'teacher', 'is-decision': message.agentId === 'decision-agent' || message.agentId === 'report', 'is-running': message.status === 'running', 'is-fallback': message.status === 'fallback' }"
                    >
                      <div class="consultation-avatar">{{ message.agentId === 'teacher' ? '师' : message.agentId === 'decision-agent' || message.agentId === 'report' ? '会' : message.agentName.slice(0, 1) }}</div>
                      <div class="consultation-bubble">
                        <div class="consultation-message-head">
                          <div>
                            <strong>{{ message.agentName }}</strong>
                            <em>{{ message.role }}</em>
                            <small v-if="message.replyToAgentName" class="consultation-reply">回应 {{ message.replyToAgentName }}</small>
                          </div>
                          <span>
                            第 {{ message.round }} 轮{{ message.createdAt ? ` · ${message.createdAt}` : '' }}
                            {{ message.status === 'running' ? ' · 模型生成中' : message.modelName ? ` · ${message.modelName}` : '' }}
                          </span>
                        </div>
                        <p>{{ message.content }}</p>
                        <div v-if="message.evidenceRefs.length" class="consultation-tags">
                          <el-tag v-for="item in message.evidenceRefs" :key="`${message.id}-${item}`" size="small" round>
                            {{ item }}
                          </el-tag>
                          <el-tag v-if="message.llmCallId" size="small" type="info" round>调用 {{ message.llmCallId }}</el-tag>
                          <el-tag v-if="message.status === 'fallback'" size="small" type="warning" round>待复核</el-tag>
                          <el-tag v-if="message.status === 'reviewed'" size="small" type="success" round>教师已复核</el-tag>
                        </div>
                        <div v-if="isMessageAwaitingReview(message)" class="message-review-actions">
                          <el-button size="small" type="success" @click="continueConsultationAfterReview">确认该意见并继续</el-button>
                          <el-button size="small" plain @click="prepareMessageFollowUp(message)">补充 / 追问</el-button>
                        </div>
                      </div>
                    </article>
                  </div>

                  <div v-if="pausedAgentId" class="consultation-followup">
                    <div class="followup-head">
                      <strong>{{ reviewingMessageId ? '教师复核' : consultationState === 'refining' ? '二次追问' : '暂停后补充' }}：{{ agentDisplayName(pausedAgentId) }}</strong>
                      <span>{{ reviewingMessageId ? '这条意见来自兜底或异常返回。确认后才会继续后续智能体，也可以补充要求后再继续。' : '提交后将从该智能体开始，继续让后续智能体讨论并更新资源方案。' }}</span>
                    </div>
                    <el-input
                      v-model="followUpText"
                      type="textarea"
                      :rows="3"
                      resize="none"
                      :placeholder="`请输入对 ${agentDisplayName(pausedAgentId)} 的补充或追问，例如：把数组越界和字符串结束符讲得更细。`"
                      @keydown.ctrl.enter.prevent="submitAgentFollowUp"
                    />
                    <div class="followup-actions">
                      <el-button @click="cancelFollowUp">取消</el-button>
                      <el-button v-if="reviewingMessageId" type="success" plain :loading="meetingLoading" @click="continueConsultationAfterReview">确认通过并继续</el-button>
                      <el-button type="primary" :loading="meetingLoading" @click="submitAgentFollowUp">提交并继续会诊</el-button>
                    </div>
                  </div>
                </el-tab-pane>

                <el-tab-pane label="画像证据" name="evidence">
                  <div v-if="consultationEvidenceLinks.length" class="evidence-link-grid">
                    <article v-for="item in consultationEvidenceLinks" :key="item.id" class="evidence-link-card">
                      <div class="evidence-link-head">
                        <el-tag size="small" type="success" effect="plain" round>{{ item.type }}</el-tag>
                        <strong>{{ item.title }}</strong>
                      </div>
                      <p class="evidence-link-conclusion">{{ item.conclusion }}</p>
                      <div class="evidence-link-columns">
                        <section>
                          <span>对应画像数据</span>
                          <ul>
                            <li v-for="value in item.profileData" :key="`${item.id}-profile-${value}`">{{ value }}</li>
                          </ul>
                        </section>
                        <section>
                          <span>会议对应依据</span>
                          <ul>
                            <li v-for="value in item.meetingBasis" :key="`${item.id}-meeting-${value}`">{{ value }}</li>
                            <li v-if="!item.meetingBasis.length">等待后续智能体补充会议依据</li>
                          </ul>
                        </section>
                      </div>
                      <div class="evidence-link-action">
                        <span>后续动作</span>
                        <strong>{{ item.action }}</strong>
                      </div>
                    </article>
                  </div>
                  <el-empty v-else description="暂无可对应的画像证据，开始会诊后自动生成证据链" :image-size="90" />
                </el-tab-pane>

                <el-tab-pane label="会议结果" name="decision">
                  <div class="meeting-result-card">
                    <div class="meeting-result-hero">
                      <el-tag type="success" effect="dark" round>最终会议结果</el-tag>
                      <h3>{{ finalMeetingResult.headline }}</h3>
                      <p>基于 {{ finalMeetingResult.rounds }} 轮、{{ finalMeetingResult.agentCount }} 个智能体的会诊意见，系统已将知识诊断、能力评估、资源生成和审核意见整理为可执行方案。</p>
                      <div class="meeting-result-stats">
                        <span>{{ finalMeetingResult.evidenceCount }} 条依据</span>
                        <span>{{ finalMeetingResult.resourceCount }} 类资源</span>
                        <span>{{ finalMeetingResult.status === 'ready' ? '已形成共识' : '待教师复核' }}</span>
                      </div>
                    </div>
                    <section class="meeting-result-section">
                      <h4>一、最终共识</h4>
                      <ul>
                        <li v-for="item in finalMeetingResult.consensus" :key="item">{{ item }}</li>
                      </ul>
                    </section>
                    <section class="meeting-result-section">
                      <h4>二、个性化资源方案</h4>
                      <div class="meeting-resource-plan">
                        <article v-for="item in finalMeetingResult.resourcePlan" :key="item.title">
                          <strong>{{ item.title }}</strong>
                          <span>{{ item.detail }}</span>
                        </article>
                      </div>
                    </section>
                    <section class="meeting-result-section">
                      <h4>三、执行路径</h4>
                      <ol>
                        <li v-for="item in finalMeetingResult.actionPlan" :key="item">{{ item }}</li>
                      </ol>
                    </section>
                    <section class="meeting-result-section">
                      <h4>四、审核与复核点</h4>
                      <div class="meeting-review-tags">
                        <el-tag v-for="item in finalMeetingResult.reviewPoints" :key="item" type="success" effect="plain" round>
                          {{ item }}
                        </el-tag>
                      </div>
                    </section>
                    <section class="meeting-result-section">
                      <h4>五、会议整理摘要</h4>
                      <div class="meeting-digest-list">
                        <article v-for="item in finalMeetingResult.digest" :key="item.title">
                          <strong>{{ item.title }}</strong>
                          <p>{{ item.content }}</p>
                        </article>
                      </div>
                    </section>
                  </div>
                </el-tab-pane>
              </el-tabs>
            </main>
          </div>
        </div>
      </section>
    </Teleport>

    <el-drawer v-model="providerDrawerVisible" title="智能体模型配置" size="420px">
      <div class="provider-drawer">
        <p class="drawer-copy">仅对接入 LLM 的智能体开放模型选择。未单独配置时，自动继承页面默认模型；页面默认模型留空时，继续跟随系统默认。</p>
        <el-form label-position="top">
          <el-form-item label="页面默认模型">
            <el-select v-model="defaultProviderKey" clearable filterable placeholder="留空则跟随系统默认" :loading="providerLoading" style="width: 100%">
              <el-option
                v-for="item in providerOptions"
                :key="`${item.source}-${item.providerKey}`"
                :label="item.label || item.providerKey"
                :value="item.providerKey"
              />
            </el-select>
          </el-form-item>

          <div v-for="agentKey in editableAgentIds" :key="agentKey" class="provider-agent-block">
            <div class="provider-agent-head">
              <strong>{{ backendAgentLabels[agentKey] }}</strong>
              <span>{{ providerBindingMode(agentKey) }}</span>
            </div>
            <el-select v-model="agentOverrides[agentKey]" clearable filterable placeholder="未选择时继承页面默认模型" :loading="providerLoading" style="width: 100%">
              <el-option
                v-for="item in providerOptions"
                :key="`${agentKey}-${item.source}-${item.providerKey}`"
                :label="item.label || item.providerKey"
                :value="item.providerKey"
              />
            </el-select>
          </div>
        </el-form>

        <div class="provider-drawer-note">
          <strong>会诊模型说明</strong>
          <p>每个智能体都可以单独绑定模型。未单独配置时，会继承页面默认模型；页面默认模型为空时，继续使用系统默认模型。</p>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped src="@/styles/teacher-agent-resource.css"></style>
