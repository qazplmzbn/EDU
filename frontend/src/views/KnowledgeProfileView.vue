<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ChatDotRound,
  DataAnalysis,
  Delete,
  Document,
  Refresh,
  TrendCharts,
  View,
} from '@element-plus/icons-vue'
import { gsap } from 'gsap'
import { BarChart, LineChart, RadarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { graphic, init, use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { learningApi } from '@/api/services'
import {
  buildFallbackReport,
  clamp,
  dimensionMeta,
  levelText,
  recordTypeText,
} from '@/features/knowledge-profile/profileReport'
import {
  formatStudentRecordForDisplay,
  parseAssistantDetail,
  sanitizeStudentText,
} from '@/features/knowledge-profile/profileRecords'
import { useStudentAssistantStore } from '@/stores/studentAssistant'
import { useAuthStore } from '@/stores/auth'

use([RadarChart, LineChart, BarChart, GridComponent, TooltipComponent, CanvasRenderer])

const router = useRouter()
const assistant = useStudentAssistantStore()
const auth = useAuthStore()

const pageRef = ref(null)
const radarChartRef = ref(null)
const trendChartRef = ref(null)
const dimensionRadarChartRef = ref(null)
const scoringBarChartRef = ref(null)
const scoringRadarChartRef = ref(null)
const aminerRadarChartRef = ref(null)
const loading = ref(false)
const refreshing = ref(false)
const activeRecord = ref(null)
const detailOpen = ref(false)
const recordFilter = ref('all')
const diagnosticTab = ref('dimension')
const scoringSourceFilter = ref('all')
const evidenceExpanded = ref(false)
const report = ref(buildFallbackReport())

const PROFILE_REPORT_CACHE_KEY = 'knowledge-profile-report-cache'
const PROFILE_RECORD_DELETED_KEY = 'knowledge-profile-record-deleted'
const deletedRecordKeys = ref([])

let gsapContext = null
let mediaContext = null
let refreshTimer = null
let radarChart = null
let trendChart = null
let dimensionRadarChart = null
let scoringBarChart = null
let scoringRadarChart = null
let aminerRadarChart = null

const summaryCards = computed(() => {
  const summary = report.value.summary || {}
  const latest = latestAttemptRecord.value
  return [
    { label: '综合画像分', value: summary.profileScore ?? 0, suffix: '', hint: '综合诊断', icon: View },
    { label: '薄弱维度', value: weakestDimensions.value[0]?.name || '知识基础', suffix: '', hint: `${weakestDimensions.value[0]?.score ?? 0} 分`, icon: DataAnalysis },
    { label: '优势维度', value: strongestDimension.value?.name || '协作表达', suffix: '', hint: `${strongestDimension.value?.score ?? 0} 分`, icon: TrendCharts },
    {
      label: '最近测评',
      value: latest?.score !== null && latest?.score !== undefined ? latest.score : '待补充',
      suffix: latest?.score !== null && latest?.score !== undefined ? '分' : '',
      hint: latest?.title || '完成作答后更新',
      icon: Document,
    },
    { label: '推荐任务', value: Math.max(weakestDimensions.value.length, 1), suffix: '项', hint: '按薄弱维度生成', icon: ChatDotRound },
  ]
})

const radarDimensions = computed(() => {
  const source = Array.isArray(report.value.radar) && report.value.radar.length
    ? report.value.radar
    : buildFallbackReport().radar
  return source.map((item) => ({
    ...item,
    score: clamp(Number(item.score) || 0, 0, 100),
    ...(dimensionMeta[item.name] || { color: '#64748b', desc: '持续收集数据中。', action: '继续完成学习任务。' }),
  }))
})

const strongestDimension = computed(() => [...radarDimensions.value].sort((a, b) => b.score - a.score)[0])
const weakestDimensions = computed(() => [...radarDimensions.value].sort((a, b) => a.score - b.score).slice(0, 3))

// ===== AMiner 风格：左侧简历 + 右侧维度刻画 =====
// 学生简历信息（头像用姓名首字占位）
const studentName = computed(() => auth.displayName || auth.user?.realName || '学生')
const studentInitial = computed(() => (studentName.value || '学').slice(0, 1))

// 当前选中的能力维度（默认第一个维度，打开页面右侧即有刻画）
const activeDimension = ref(null)
const activeDim = computed(() => {
  const name = activeDimension.value || radarDimensions.value[0]?.name || ''
  return radarDimensions.value.find((d) => d.name === name) || radarDimensions.value[0] || null
})

function selectDimension(name) {
  activeDimension.value = name
}

// 与该维度相关的学习记录（按记录影响的维度匹配）
const dimRelatedRecords = computed(() => {
  if (!activeDim.value) return []
  return evidenceRecords.value
    .filter((record) => {
      const impacts = Array.isArray(record.impactChanges) ? record.impactChanges : []
      return impacts.some((impact) => impact && String(impact.name || '').includes(activeDim.value.name))
    })
    .slice(0, 5)
})

// 与该维度相关的知识点（从关联记录的 relatedKnowledge 汇总）
const dimKnowledgePoints = computed(() => {
  if (!activeDim.value) return []
  const list = []
  dimRelatedRecords.value.forEach((record) => {
    if (!Array.isArray(record.relatedKnowledge)) return
    record.relatedKnowledge.forEach((kp) => {
      if (kp && !list.includes(kp)) list.push(kp)
    })
  })
  if (list.length) return list.slice(0, 6)
  return ['变量与数据类型', '分支与循环', '数组与字符串', '函数与递归', '指针与内存'].slice(0, 4)
})
// ===== 能力识别：从画像数据中自动识别学生能力分级 =====
// 按得分把画像维度识别为：优势能力(≥70) / 平稳能力(60~69) / 待提升能力(<60)
const recognizedAbilities = computed(() => {
  const groups = { strong: [], steady: [], weak: [] }
  radarDimensions.value.forEach((dim) => {
    const score = Number(dim.score) || 0
    if (score >= 70) groups.strong.push(dim)
    else if (score >= 60) groups.steady.push(dim)
    else groups.weak.push(dim)
  })
  return groups
})

// 识别结论文案（类似简历中的能力自评）
const abilitySummary = computed(() => {
  const strong = recognizedAbilities.value.strong
  const weak = recognizedAbilities.value.weak
  const strongText = strong.length ? `在${strong.map((d) => d.name).join('、')}上表现突出` : '暂无突出优势能力'
  const weakText = weak.length ? `，${weak.map((d) => d.name).join('、')}有待提升` : ''
  return `${strongText}${weakText}，整体画像分为 ${report.value.summary?.profileScore ?? 0} 分。`
})

// ===== 简历信息（基于简历内容） =====
const resumeInfo = {
  name: '张明宇',
  title: '计算机科学与技术 · 本科在读',
  org: '西南大学 · 计算机与信息科学学院',
  email: 'zhangmingyu@swu.edu.cn',
  homepage: 'https://github.com/zhangmingyu',
  education: [
    { year: '2023', school: '西南大学', degree: '计算机科学与技术 本科' },
    { year: '2020', school: '重庆市第一中学', degree: '理科' },
  ],
  work: [
    { year: '2025', org: '某科技有限公司', role: '前端开发实习生' },
    { year: '2024', org: '课程项目', role: '校园导航系统（C / EasyX）' },
    { year: '2024', org: '高教社杯数学建模竞赛', role: '重庆赛区二等奖' },
    { year: '2023', org: '学生工作', role: '班长 / 年级助理' },
  ],
  bio: '计算机科学与技术专业大三学生，熟练掌握 C/C++ 与数据结构算法，熟悉 Vue、Java Web 开发，有前端实习与课程项目经验。曾获数学建模竞赛重庆二等奖，担任班长组织多项学生活动。目前关注前端工程化与算法学习，准备攻读研究生。',
}

// ===== 从简历中筛选出的核心能力（每个能力带维度刻画数据） =====
const scholarAbilities = [
  {
    name: '知识点掌握状态',
    enName: 'Knowledge State',
    color: '#2563eb',
    score: 64,
    level: '中等',
    desc: '表示学生对每个知识点当前的掌握程度，是整个学生画像最底层、最核心的状态。该维度不压缩成单一总分，而是保存"学生 × 知识点"的 mastery 向量；课程层面的"知识基础"可由这些知识点状态进一步聚合得到。',
    formula: 'S_l = Σ(w_i × t_i) / Σw_i ，其中 t_i 为每个知识点涉及题目的准确率，前端通过加权融合得到课程层面分数。',
    dataSource: '各知识点题目准确率，按题目权重加权融合',
    subIndicators: [
      { name: '变量与数据类型', value: 85, desc: '基础语法掌握良好' },
      { name: '分支与循环', value: 72, desc: '控制流较熟练' },
      { name: '数组与字符串', value: 68, desc: '基本掌握，需加强' },
      { name: '函数与递归', value: 55, desc: '递归思想理解不足' },
      { name: '指针与内存', value: 40, desc: '薄弱环节，需重点突破' },
    ],
  },
  {
    name: '认知层级画像',
    enName: 'Cognitive Profile',
    color: '#7c3aed',
    score: 56,
    level: '待提升',
    desc: '描述学生在不同认知层级上的实际表现，用于区分"会记忆""能理解""能应用""能分析"等不同层次。该维度依赖资源蓝图智能体在题目生成前明确标注 cognitive_level，而非题目生成后由模型随意贴标签。',
    formula: 'S_l = Σ(w_i × s_i) / Σw_i ，对认知层级 l 汇总该层级下有效题目的归一化得分 s_i，按证据可信程度加权 w_i（隐藏检测题权重更高）。',
    dataSource: '带 cognitive_level 的个性化学习题与隐藏检测题',
    subIndicators: [
      { name: 'REMEMBER 记忆', value: 88, desc: '识别、回忆、定义、列举已有知识' },
      { name: 'UNDERSTAND 理解', value: 75, desc: '解释、比较、分类、概括、推断知识含义与关系' },
      { name: 'APPLY 应用', value: 62, desc: '在给定或新情境中使用知识完成任务' },
      { name: 'ANALYZE 分析', value: 48, desc: '拆解材料、区分组成部分并分析关系' },
      { name: 'EVALUATE 评价', value: 35, desc: '依据明确标准进行判断、验证或论证' },
      { name: 'CREATE 创造', value: 28, desc: '重组已有知识形成新方案、设计或完整产出' },
    ],
  },
  {
    name: '学习主动性',
    enName: 'Learning Initiative',
    color: '#16a34a',
    score: 55,
    level: '中等',
    desc: '表示学生是否主动发起、扩展和修复自己的学习过程。必须记录 action_origin，区分 SYSTEM_REQUIRED、SYSTEM_RECOMMENDED 与 STUDENT_INITIATED；系统要求完成的任务不能直接视为高主动性，真正重要的是额外练习、主动提问、答错后自主回看等。',
    formula: 'Initiative = w₁×ExtraLearningRate + w₂×HelpSeekingRate + w₃×ErrorRecoveryRate',
    dataSource: '学习会话行为日志，按 action_origin 区分主动发起与系统要求/推荐',
    subIndicators: [
      { name: 'ExtraLearningRate 额外学习率', value: 65, desc: '主动发起的额外学习行为次数 ÷ 有效学习会话数' },
      { name: 'HelpSeekingRate 主动求助率', value: 42, desc: '有效主动求助次数 ÷ 可观察的困难机会次数' },
      { name: 'ErrorRecoveryRate 错误恢复率', value: 58, desc: '答错后主动进入解释/资源/补充练习次数 ÷ 有效错误次数' },
    ],
  },
  {
    name: '学习行为规律性',
    enName: 'Learning Regularity',
    color: '#d97706',
    score: 70,
    level: '良好',
    desc: '描述学生学习行为在时间上的连续性、覆盖率和波动程度。时间窗口可按天或周配置，长期课程同时维护 active_day_rate 和 active_week_rate。',
    formula: 'Regularity = w₁×ActiveRate + w₂×RegularityCV ，其中 RegularityCV = 1 / (1 + WeeklyTimeCV)，CV 越低表示不同周学习投入越稳定。',
    dataSource: '有效学习行为时间窗口统计（按天/周配置）',
    subIndicators: [
      { name: 'ActiveRate 活跃率', value: 72, desc: '有效学习行为时间窗口数 ÷ 总观察时间窗口数' },
      { name: 'RegularityCV 稳定度', value: 68, desc: '由每周学习时长标准差÷均值（CV）归一化得到' },
    ],
  },
  {
    name: '学习资源偏好',
    enName: 'Resource Preference',
    color: '#e11d48',
    score: 60,
    level: '参考',
    desc: '描述学生当前更倾向使用哪些资源呈现方式。可通过记录学生对话以及平时使用的资源统计，内部统计不作为前端展示主指标，此处仅展示偏好分布供参考。',
    formula: '内部统计，不对外输出单一分数；展示各资源类型使用占比分布。',
    dataSource: '学生对话记录与资源使用行为统计',
    subIndicators: [
      { name: '视频讲解', value: 40, desc: '资源使用占比' },
      { name: '图文教程', value: 25, desc: '资源使用占比' },
      { name: '互动练习', value: 20, desc: '资源使用占比' },
      { name: '对话答疑', value: 15, desc: '资源使用占比' },
    ],
  },
  {
    name: '岗位匹配度',
    enName: 'Job Matching',
    color: '#0891b2',
    score: 72,
    level: '良好',
    desc: '综合评估学生当前技能水平与目标岗位要求的匹配程度，区分必备技能与可选技能，并单独展示技能证据覆盖率以说明匹配结果的可信程度。',
    formula: 'JobMatch = 100 × [α × EssentialFit + (1-α) × OptionalFit]，其中 α 为必备技能权重（建议取 0.8）；EvidenceCoverage 单独展示，不与技能掌握程度混合。',
    dataSource: '简历技能证据（q_j 技能水平、c_j 证据可信度、r_j 时效权重）+ 课程测评技能水平 CourseSkill_i + 目标岗位技能要求 TargetSkill_i',
    subIndicators: [
      { name: 'CurrentSkill 当前技能水平', value: 68, desc: 'CurrentSkill_i = λ×ResumeSkill_i + (1-λ)×CourseSkill_i，融合简历证据与课程测评' },
      { name: 'SkillMatch 单项技能匹配', value: 75, desc: 'SkillMatch_i = min(CurrentSkill_i / TargetSkill_i, 1)，取值范围 [0,1]' },
      { name: 'EssentialFit 必备技能匹配', value: 70, desc: '必备技能加权匹配度 Σ(w_i×SkillMatch_i×I_i)/Σw_i，α 建议 0.8' },
      { name: 'OptionalFit 可选技能匹配', value: 60, desc: '可选技能加权匹配度，作为岗位适配增强项，总分权重较低' },
      { name: 'EvidenceCoverage 证据覆盖率', value: 82, desc: '岗位技能中具有有效简历或课程证据的比例，说明匹配结果可信度' },
    ],
  }]

const activeAbility = ref(scholarAbilities[0])

function selectAbility(name) {
  const a = scholarAbilities.find((x) => x.name === name)
  if (a) {
    activeAbility.value = a
    nextTick(() => {
      updateAminerRadarChart()
      if (aminerRadarChart) aminerRadarChart.resize()
    })
  }
}

const radarSummaryCards = computed(() => [
  {
    label: '薄弱点',
    value: weakestDimensions.value[0]?.name || '知识基础',
    text: weakestDimensions.value[0]?.action || '先补齐低掌握知识点。',
    tone: 'risk',
  },
  {
    label: '优势点',
    value: strongestDimension.value?.name || '协作表达',
    text: '保留当前优势维度的训练频率，用它带动复盘和表达。',
    tone: 'strength',
  },
  {
    label: '下一步',
    value: '专项补强',
    text: weakestDimensions.value.slice(0, 2).map((item) => item.name).join('、') || '完成一次针对性练习',
    tone: 'advice',
  },
])
const insights = computed(() => {
  const list = Array.isArray(report.value.insights) ? report.value.insights : []
  const generated = [
    {
      type: 'risk',
      title: `优先补强 ${weakestDimensions.value[0]?.name || '知识基础'}`,
      description: weakestDimensions.value[0]?.action || '完成一次练习后系统会给出更准确建议。',
      priority: '高优先级',
    },
    {
      type: 'strength',
      title: `${strongestDimension.value?.name || '协作表达'}较稳定`,
      description: `当前得分 ${strongestDimension.value?.score ?? 0}，可作为后续迁移、讲解和复盘表达的支点。`,
      priority: '保持',
    },
    {
      type: 'attempt',
      title: '最近作答已纳入画像',
      description: latestAttemptRecord.value
        ? `${latestAttemptRecord.value.title} 已计入得分趋势和能力分布。`
        : '完成试卷或练习后，系统会自动更新画像依据。',
      priority: '已更新',
    },
    {
      type: 'assistant',
      title: '小C对话持续更新画像',
      description: `${recordStats.value.assistant} 条小C互动会影响学习路径、资源筛选和复盘建议。`,
      priority: '持续采集',
    },
    {
      type: 'advice',
      title: '系统建议',
      description: `建议优先复习 ${weakestDimensions.value.map((item) => item.name).join('、') || '知识基础'}，并完成对应练习。`,
      priority: '下一步',
    },
  ]
  if (!list.length) return generated
  return [...list.slice(0, 3).map((item) => ({ ...item, description: sanitizeStudentText(item.description), priority: item.priority || '诊断' })), ...generated.slice(1, 4)]
})

const trendSummary = computed(() => {
  const raw = Array.isArray(report.value.scoreTrend) ? report.value.scoreTrend.slice(-5) : []
  const scores = raw.map((item) => clamp(Number(item.score) || 0, 0, 100))
  if (!scores.length) {
    return {
      trend: '暂无',
      highest: '待更新',
      latest: '待更新',
      delta: 0,
      reason: '完成一次试卷或练习后，系统会生成最近画像趋势。',
      advice: '先完成一次阶段测评或专项练习，用真实作答数据更新画像。',
    }
  }
  const first = scores[0] ?? 0
  const latest = scores[scores.length - 1] ?? 0
  const highest = scores.length ? Math.max(...scores) : 0
  const delta = latest - first
  const weak = weakestDimensions.value[0]?.name || '知识基础'
  const trend = delta > 3 ? '上升' : delta < -3 ? '下降' : '平稳'
  return {
    trend,
    highest,
    latest,
    delta,
    reason: trend === '下降'
      ? `最近一次测评表现回落，主要拉低 ${weak} 与理解迁移相关判断。`
      : trend === '上升'
        ? `最近作答表现改善，${strongestDimension.value?.name || '优势维度'}正在稳定支撑画像。`
        : `最近画像分波动不大，建议继续补充 ${weak} 的练习证据。`,
    advice: trend === '下降'
      ? '优先复盘错题并完成知识基础补强练习。'
      : `继续围绕 ${weak} 完成专项练习和小C复盘。`,
  }
})

const scoringVariants = computed(() => {
  const analysis = report.value.scoringItemAnalysis || buildFallbackReport().scoringItemAnalysis
  return analysis?.variants || { all: analysis }
})

const activeScoringAnalysis = computed(() => {
  return scoringVariants.value?.[scoringSourceFilter.value] || scoringVariants.value?.all || buildFallbackReport().scoringItemAnalysis
})

const scoringItems = computed(() => {
  return Array.isArray(activeScoringAnalysis.value?.items) ? activeScoringAnalysis.value.items : []
})

const diagnosisSummary = computed(() => {
  const items = scoringItems.value.length
    ? scoringItems.value.map((item) => ({ name: item.name, score: clamp(Number(item.scoreRate) || 0, 0, 100) }))
    : radarDimensions.value.map((item) => ({ name: item.name, score: item.score }))
  const best = [...items].sort((a, b) => b.score - a.score)[0]
  const weak = [...items].sort((a, b) => a.score - b.score)[0]
  return {
    best: best?.name || strongestDimension.value?.name || '计算思维',
    weak: weak?.name || weakestDimensions.value[0]?.name || '边缘情况考虑',
    advice: `优先完成 ${weakestDimensions.value.slice(0, 3).map((item) => item.name).join('、') || '数组与字符串、分支循环、变量与数据类型'} 相关练习。`,
  }
})

const filteredRecords = computed(() => {
  const records = Array.isArray(report.value.records) ? report.value.records : []
  const visible = records.filter((item) => !deletedRecordKeys.value.includes(recordKey(item)))
  if (recordFilter.value === 'all') return visible
  return visible.filter((item) => item.type === recordFilter.value)
})
const evidenceRecords = computed(() => filteredRecords.value.map(displayRecord))
const visibleEvidenceRecords = computed(() => evidenceExpanded.value ? evidenceRecords.value : evidenceRecords.value.slice(0, 5))

const recordTabs = [
  { label: '全部', value: 'all' },
  { label: '试卷记录', value: 'exam' },
  { label: '练习题记录', value: 'practice' },
  { label: '小C对话', value: 'assistant' },
]

const currentRecordTabLabel = computed(() => {
  return recordTabs.find((tab) => tab.value === recordFilter.value)?.label || '全部'
})

const recordStats = computed(() => {
  const records = Array.isArray(report.value.records) ? report.value.records : []
  return {
    all: records.length,
    exam: records.filter((item) => item.type === 'exam').length,
    practice: records.filter((item) => item.type === 'practice').length,
    assistant: records.filter((item) => item.type === 'assistant').length,
  }
})

const latestAttemptRecord = computed(() => {
  const source = filteredRecords.value.find((item) => item.type === 'exam' || item.type === 'practice')
    || (Array.isArray(report.value.records) ? report.value.records.find((item) => item.type === 'exam' || item.type === 'practice') : null)
  return source ? displayRecord(source) : null
})

const latestAssistantRecord = computed(() => {
  const source = filteredRecords.value.find((item) => item.type === 'assistant')
    || (Array.isArray(report.value.records) ? report.value.records.find((item) => item.type === 'assistant') : null)
  return source ? displayRecord(source) : null
})

const recentTimelineRecords = computed(() => {
  return [latestAttemptRecord.value, latestAssistantRecord.value]
    .filter(Boolean)
    .sort((left, right) => String(right.time || '').localeCompare(String(left.time || '')))
})

const evidenceAdvice = computed(() => {
  const weak = weakestDimensions.value[0]?.name || '知识基础'
  if (latestAttemptRecord.value && latestAssistantRecord.value) {
    return `结合最近作答和小C对话，建议优先完成 ${weak} 专项练习。`
  }
  return `继续补充作答记录和小C复盘，让系统更准确判断 ${weak} 的补强路径。`
})
const profileSyncStatus = computed(() => {
  const updatedAt = report.value.summary?.updatedAt || latestAssistantRecord.value?.time || latestAttemptRecord.value?.time || '等待数据'
  if (refreshing.value) {
    return {
      label: '画像更新中',
      text: '正在根据最新记录重新计算画像',
      updatedAt,
      tone: 'syncing',
    }
  }
  if (recordStats.value.all > 0) {
    return {
      label: '画像已同步',
      text: '最近记录已纳入画像分析',
      updatedAt,
      tone: 'synced',
    }
  }
  return {
    label: '等待更新',
    text: '完成作答或小C对话后更新画像',
    updatedAt,
    tone: 'pending',
  }
})

const profileNarrative = computed(() => {
  const strong = strongestDimension.value
  const weak = weakestDimensions.value[0]
  return `当前画像显示，${strong?.name || '学习能力'}相对稳定，${weak?.name || '知识基础'}需要优先关注。系统会持续结合最新作答和小C对话记录更新判断。`
})

const activeAssistantQa = computed(() => parseAssistantDetail(activeRecord.value))

watch(recordFilter, () => {
  evidenceExpanded.value = false
})

async function loadReport(options = {}) {
  if (options.silent) {
    refreshing.value = true
  } else {
    loading.value = true
  }
  try {
    const data = await learningApi.profileReport()
    report.value = normalizeReport(data)
    persistReport()
    if (options.animate !== false) {
      await nextTick()
      animateRefresh()
    }
  } catch (error) {
    if (!options.silent) {
      ElMessage.warning(error.message || '画像报告暂时无法加载，已使用本地演示数据')
    }
    const cached = loadCachedReport()
    if (cached) {
      report.value = normalizeReport(cached)
    } else if (!report.value?.summary) {
      report.value = buildFallbackReport()
    }
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

function recordKey(record) {
  return `${record?.type || 'record'}-${record?.refId || record?.title || 'item'}-${record?.time || ''}`
}

function displayRecord(record) {
  return formatStudentRecordForDisplay(record, {
    recordKey,
    fallbackKnowledge: weakestDimensions.value.slice(0, 3).map((item) => item.name),
  })
}

function persistReport() {
  localStorage.setItem(PROFILE_REPORT_CACHE_KEY, JSON.stringify(report.value || buildFallbackReport()))
}

function loadCachedReport() {
  try {
    const raw = localStorage.getItem(PROFILE_REPORT_CACHE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

function persistDeletedRecordKeys() {
  localStorage.setItem(PROFILE_RECORD_DELETED_KEY, JSON.stringify(deletedRecordKeys.value))
}

function restoreDeletedRecordKeys() {
  try {
    const raw = localStorage.getItem(PROFILE_RECORD_DELETED_KEY)
    deletedRecordKeys.value = raw ? JSON.parse(raw) : []
  } catch {
    deletedRecordKeys.value = []
  }
}

function deleteRecord(record) {
  const key = record?.originalKey || recordKey(record)
  if (!deletedRecordKeys.value.includes(key)) {
    deletedRecordKeys.value = [key, ...deletedRecordKeys.value]
    persistDeletedRecordKeys()
  }
  if (activeRecord.value && (activeRecord.value.originalKey || recordKey(activeRecord.value)) === key) {
    detailOpen.value = false
    activeRecord.value = null
  }
}

function batchDeleteRecords() {
  const keys = filteredRecords.value.map(recordKey)
  if (!keys.length) return
  deletedRecordKeys.value = Array.from(new Set([...deletedRecordKeys.value, ...keys]))
  persistDeletedRecordKeys()
  if (activeRecord.value && deletedRecordKeys.value.includes(recordKey(activeRecord.value))) {
    detailOpen.value = false
    activeRecord.value = null
  }
}

function scheduleRefresh() {
  if (refreshTimer) window.clearTimeout(refreshTimer)
  refreshTimer = window.setTimeout(() => {
    refreshTimer = null
    loadReport({ silent: true })
  }, 360)
}

function openRecord(record) {
  activeRecord.value = record ? { ...record } : null
  detailOpen.value = true
  nextTick(() => animateDrawer())
}

function goRecord(record) {
  if (!record || record.type === 'assistant' || !record.refId) return
  router.push(`/attempts/${record.refId}/result`)
}

function normalizeReport(data = {}) {
  const fallback = buildFallbackReport()
  const cached = loadCachedReport()
  const incomingRecords = Array.isArray(data.records) ? data.records : cached?.records || fallback.records
  return {
    summary: { ...fallback.summary, ...(data.summary || {}) },
    radar: Array.isArray(data.radar) && data.radar.length ? data.radar : fallback.radar,
    insights: Array.isArray(data.insights) && data.insights.length ? data.insights : fallback.insights,
    scoreTrend: Array.isArray(data.scoreTrend) ? data.scoreTrend : fallback.scoreTrend,
    evaluationDistribution:
      Array.isArray(data.evaluationDistribution) && data.evaluationDistribution.length
        ? data.evaluationDistribution
        : fallback.evaluationDistribution,
    scoringItemAnalysis: data.scoringItemAnalysis || fallback.scoringItemAnalysis,
    records: incomingRecords,
  }
}

function initCharts() {
  if (radarChart && radarChartRef.value && radarChart.getDom() !== radarChartRef.value) {
    radarChart.dispose()
    radarChart = null
  }
  if (trendChart && trendChartRef.value && trendChart.getDom() !== trendChartRef.value) {
    trendChart.dispose()
    trendChart = null
  }
  if (dimensionRadarChart && dimensionRadarChartRef.value && dimensionRadarChart.getDom() !== dimensionRadarChartRef.value) {
    dimensionRadarChart.dispose()
    dimensionRadarChart = null
  }
  if (scoringBarChart && (!scoringBarChartRef.value || scoringBarChart.getDom() !== scoringBarChartRef.value)) {
    scoringBarChart.dispose()
    scoringBarChart = null
  }
  if (scoringRadarChart && scoringRadarChartRef.value && scoringRadarChart.getDom() !== scoringRadarChartRef.value) {
    scoringRadarChart.dispose()
    scoringRadarChart = null
  }
  if (radarChartRef.value && !radarChart) {
    radarChart = init(radarChartRef.value)
  }
  if (trendChartRef.value && !trendChart) {
    trendChart = init(trendChartRef.value)
  }
  if (dimensionRadarChartRef.value && !dimensionRadarChart) {
    dimensionRadarChart = init(dimensionRadarChartRef.value)
  }
  if (scoringBarChartRef.value && !scoringBarChart) {
    scoringBarChart = init(scoringBarChartRef.value)
  }
  if (scoringRadarChartRef.value && !scoringRadarChart) {
    scoringRadarChart = init(scoringRadarChartRef.value)
  }
  if (aminerRadarChartRef.value && !aminerRadarChart) {
    aminerRadarChart = init(aminerRadarChartRef.value)
  }
  updateCharts()
}

function updateCharts() {
  updateRadarChart()
  updateTrendChart()
  updateDimensionRadarChart()
  updateScoringBarChart()
  updateScoringRadarChart()
  updateAminerRadarChart()
}

function updateRadarChart() {
  if (!radarChart) return
  const dimensions = radarDimensions.value
  radarChart.setOption({
    color: ['#0f766e'],
    tooltip: {
      trigger: 'item',
      confine: true,
      formatter: () => dimensions.map((item) => {
        const advice = item.action || '继续补充学习证据。'
        return `${item.name}：${item.score} 分<br/>建议：${advice}`
      }).join('<br/><br/>'),
    },
    radar: {
      center: ['50%', '50%'],
      radius: '68%',
      indicator: dimensions.map((item) => ({ name: item.name, max: 100 })),
      splitNumber: 5,
      axisName: {
        color: '#334155',
        fontWeight: 700,
        fontSize: 12,
      },
      axisLine: { lineStyle: { color: 'rgba(100, 116, 139, 0.22)' } },
      splitLine: { lineStyle: { color: 'rgba(15, 118, 110, 0.18)' } },
      splitArea: {
        areaStyle: {
          color: ['rgba(20, 184, 166, 0.06)', 'rgba(37, 99, 235, 0.04)'],
        },
      },
    },
    series: [{
      type: 'radar',
      smooth: true,
      symbol: 'circle',
      symbolSize: 7,
      lineStyle: { width: 3, color: '#0f766e' },
      areaStyle: {
        color: new graphic.RadialGradient(0.5, 0.5, 0.8, [
          { offset: 0, color: 'rgba(56, 189, 248, 0.34)' },
          { offset: 1, color: 'rgba(15, 118, 110, 0.08)' },
        ]),
      },
      itemStyle: { color: '#0f766e', borderColor: '#ffffff', borderWidth: 2 },
      data: [{ value: dimensions.map((item) => item.score), name: '学习特征' }],
    }],
  })
}

function updateDimensionRadarChart() {
  if (!dimensionRadarChart) return
  const dimensions = radarDimensions.value
  dimensionRadarChart.setOption({
    color: ['#14b8a6'],
    tooltip: {
      trigger: 'item',
      confine: true,
      formatter: () => dimensions.map((item) => `${item.name}：${item.score} 分`).join('<br/>'),
    },
    radar: {
      center: ['50%', '50%'],
      radius: '62%',
      indicator: dimensions.map((item) => ({ name: item.name, max: 100 })),
      splitNumber: 4,
      axisName: { color: '#64748b', fontSize: 18 , fontweight: 'bold'},
      axisLine: { lineStyle: { color: 'rgba(100, 116, 139, 0.2)' } },
      splitLine: { lineStyle: { color: 'rgba(15, 118, 110, 0.15)' } },
      splitArea: {
        areaStyle: {
          color: ['rgba(240, 253, 250, 0.9)', 'rgba(255, 255, 255, 0.9)'],
        },
      },
    },
    series: [{
      type: 'radar',
      smooth: true,
      symbol: 'circle',
      symbolSize: 5,
      lineStyle: { width: 2, color: '#14b8a6' },
      areaStyle: { color: 'rgba(20, 184, 166, 0.16)' },
      itemStyle: { color: '#14b8a6' },
      data: [{ value: dimensions.map((item) => item.score), name: '画像维度' }],
    }],
  })
}

// AMiner 右侧维度刻画：全维度对比雷达 + 高亮当前选中维度
function updateAminerRadarChart() {
  if (!aminerRadarChart) return
  const abilities = scholarAbilities
  const current = activeAbility.value?.name
  const idx = abilities.findIndex((d) => d.name === current)
  const accent = activeAbility.value?.color || '#2563eb'
  const highlight = abilities.map((d, i) => (i === idx ? d.score : 0))
  const allValues = abilities.map((d) => d.score)
  aminerRadarChart.setOption({
    tooltip: {
      trigger: 'item',
      confine: true,
      formatter: (params) => {
        const dim = abilities[params.dataIndex]
        if (!dim) return ''
        return dim.name + '：' + dim.score + ' 分 · ' + dim.level
      },
    },
    legend: {
      bottom: 0,
      itemWidth: 14,
      itemHeight: 8,
      textStyle: { color: '#64748b', fontSize: 12 },
      data: ['能力对比', current || ''],
    },
    radar: {
      center: ['50%', '52%'],
      radius: '62%',
      indicator: abilities.map((d) => ({ name: d.name, max: 100 })),
      splitNumber: 4,
      axisName: { color: '#475569', fontSize: 11, fontWeight: 600 },
      axisLine: { lineStyle: { color: 'rgba(100, 116, 139, 0.25)' } },
      splitLine: { lineStyle: { color: 'rgba(100, 116, 139, 0.16)' } },
      splitArea: {
        areaStyle: { color: ['rgba(248, 250, 252, 0.9)', 'rgba(241, 245, 249, 0.9)'] },
      },
    },
    series: [
      {
        type: 'radar',
        name: '能力对比',
        smooth: true,
        symbol: 'circle',
        symbolSize: 4,
        lineStyle: { width: 2, color: 'rgba(148, 163, 184, 0.6)' },
        areaStyle: { color: 'rgba(148, 163, 184, 0.10)' },
        itemStyle: { color: '#94a3b8' },
        data: [{ value: allValues, name: '能力对比' }],
      },
      {
        type: 'radar',
        name: current || '',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: { width: 3, color: accent },
        areaStyle: { color: accent + '33' },
        itemStyle: { color: accent, borderColor: '#fff', borderWidth: 2 },
        data: [{ value: highlight, name: current || '' }],
      },
    ],
  }, true)
}

function updateTrendChart() {
  if (!trendChart) return
  const points = Array.isArray(report.value.scoreTrend) ? report.value.scoreTrend.slice(-5) : []
  trendChart.setOption({
    grid: { left: 34, right: 22, top: 28, bottom: 32 },
    tooltip: {
      trigger: 'axis',
      confine: true,
      formatter: (params) => {
        const item = params?.[0]
        if (!item) return ''
        const raw = points[item.dataIndex]
        const source = raw?.sourceType === 'practice' ? '练习' : '试卷'
        const rawScore = raw?.rawScore !== undefined && raw?.maxScore ? `<br/>原始得分：${raw.rawScore} / ${raw.maxScore}` : ''
        return `${source} ${raw?.date || item.axisValue}<br/>画像得分：${item.value}${rawScore}`
      },
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: points.map((item) => item.date || '未记录'),
      axisLine: { lineStyle: { color: '#cbd5e1' } },
      axisTick: { show: false },
      axisLabel: { color: '#64748b' , fontSize: 14, fontweight: 'bold'},
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      splitNumber: 4,
      axisLabel: { color: '#64748b' , fontSize: 14, fontweight: 'bold'},
      splitLine: { lineStyle: { color: '#e2e8f0' } },
    },
    series: [{
      name: '画像得分',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      data: points.map((item) => clamp(Number(item.score) || 0, 0, 100)),
      lineStyle: { width: 3, color: '#0f766e' },
      itemStyle: { color: '#0f766e', borderColor: '#ffffff', borderWidth: 2 },
      areaStyle: {
        color: new graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(20, 184, 166, 0.26)' },
          { offset: 1, color: 'rgba(20, 184, 166, 0.02)' },
        ]),
      },
    }],
  })
}

function updateScoringRadarChart() {
  if (!scoringRadarChart) return
  const items = scoringItems.value
  scoringRadarChart.setOption({
    color: ['#8b5cf6'],
    tooltip: {
      trigger: 'item',
      confine: true,
      formatter: () => items.map((item) => `${item.name}：${clamp(Number(item.scoreRate) || 0, 0, 100)}%`).join('<br/>'),
    },
    radar: {
      center: ['50%', '50%'],
      radius: '58%',
      indicator: items.map((item) => ({ name: item.name, max: 100 })),
      splitNumber: 4,
      axisName: { color: '#64748b', fontSize: 18 , fontweight: 'bold'},
      axisLine: { lineStyle: { color: 'rgba(100, 116, 139, 0.2)' } },
      splitLine: { lineStyle: { color: 'rgba(139, 92, 246, 0.16)' } },
      splitArea: {
        areaStyle: {
          color: ['rgba(245, 243, 255, 0.9)', 'rgba(255, 255, 255, 0.9)'],
        },
      },
    },
    series: [{
      type: 'radar',
      smooth: true,
      symbol: 'circle',
      symbolSize: 5,
      lineStyle: { width: 2, color: '#8b5cf6' },
      areaStyle: { color: 'rgba(139, 92, 246, 0.18)' },
      itemStyle: { color: '#8b5cf6' },
      data: [{ value: items.map((item) => clamp(Number(item.scoreRate) || 0, 0, 100)), name: '评分项' }],
    }],
  })
}

function updateScoringBarChart() {
  if (!scoringBarChart) return
  const items = scoringItems.value
  scoringBarChart.setOption({
    color: ['#7c3aed'],
    grid: { left: 42, right: 20, top: 48, bottom: 86 },
    tooltip: {
      trigger: 'axis',
      confine: true,
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const item = params?.[0]
        if (!item) return ''
        return `${item.name}<br/>得分率：${item.value}%`
      },
    },
    xAxis: {
      type: 'category',
      data: items.map((item) => item.name),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        color: '#475569',
        fontWeight: 'bold',
        fontSize: 13,
        interval: 0,
        width: 80,
        overflow: 'break',
      },
    },
    yAxis: {
      type: 'value',
      max: 100,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#64748b', fontSize: 12 },
      splitLine: { lineStyle: { type: 'dashed', color: '#e2e8f0' } },
    },
    series: [{
      name: '得分率',
      type: 'bar',
      barWidth: 20,
      showBackground: true,
      backgroundStyle: {
        color: '#f1f5f9',
        borderRadius: 6,
      },
      itemStyle: {
        borderRadius: 6,
        color: new graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#a78bfa' },
          { offset: 1, color: '#7c3aed' },
        ]),
      },
      label: {
        show: true,
        position: 'top',
        formatter: '{c}%',
        color: '#1e293b',
        fontWeight: 'bold',
      },
      data: items.map((item) => clamp(Number(item.scoreRate) || 0, 0, 100)),
    }],
  })
}

function resizeCharts() {
  radarChart?.resize()
  trendChart?.resize()
  dimensionRadarChart?.resize()
  scoringBarChart?.resize()
  scoringRadarChart?.resize()
  aminerRadarChart?.resize()
}

function disposeCharts() {
  radarChart?.dispose()
  trendChart?.dispose()
  dimensionRadarChart?.dispose()
  scoringBarChart?.dispose()
  scoringRadarChart?.dispose()
  aminerRadarChart?.dispose()
  radarChart = null
  trendChart = null
  dimensionRadarChart = null
  scoringBarChart = null
  scoringRadarChart = null
  aminerRadarChart = null
}

function animateIntro() {
  if (!pageRef.value) return
  mediaContext = gsap.matchMedia()
  mediaContext.add(
    {
      reduceMotion: '(prefers-reduced-motion: reduce)',
      isDesktop: '(min-width: 900px)',
    },
    (context) => {
      if (context.conditions.reduceMotion) return
      gsapContext = gsap.context(() => {
        gsap.from('.profile-animate', {
          autoAlpha: 0,
          y: 18,
          duration: 0.56,
          ease: 'power2.out',
          stagger: 0.055,
          clearProps: 'opacity,visibility,transform',
        })
        gsap.from('.summary-card', {
          autoAlpha: 0,
          y: 14,
          scale: 0.98,
          duration: 0.42,
          ease: 'power2.out',
          stagger: 0.045,
          clearProps: 'opacity,visibility,transform',
        })
        gsap.from('.profile-chart', {
          autoAlpha: 0,
          scale: 0.98,
          duration: 0.5,
          ease: 'power2.out',
          stagger: 0.06,
          clearProps: 'opacity,visibility,transform',
        })
      }, pageRef.value)
    },
  )
}

function animateRefresh() {
  if (!pageRef.value || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  gsap.fromTo(
    pageRef.value.querySelectorAll('.live-pulse'),
    { scale: 0.985 },
    { scale: 1, duration: 0.26, ease: 'power2.out', overwrite: 'auto' },
  )
  gsap.fromTo(
    pageRef.value.querySelectorAll('.record-row:first-child'),
    { x: 18 },
    { x: 0, duration: 0.38, ease: 'power2.out', overwrite: 'auto', clearProps: 'transform' },
  )
}

function animateDrawer() {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  const drawer = document.querySelector('.profile-record-drawer')
  if (!drawer) return
  gsap.fromTo(drawer, { scale: 0.96, autoAlpha: 0 }, { scale: 1, autoAlpha: 1, duration: 0.22, ease: 'power2.out' })
}

watch(
  () => assistant.profileRefreshToken,
  (value, oldValue) => {
    if (value && value !== oldValue) scheduleRefresh()
  },
)

watch(
  report,
  async () => {
    await nextTick()
    initCharts()
    updateCharts()
  },
  { deep: true },
)

watch(
  [diagnosticTab, scoringSourceFilter],
  async () => {
    await nextTick()
    initCharts()
    resizeCharts()
    updateCharts()
  },
)

// 点击左侧能力 → 刷新右侧维度刻画雷达
watch(
  activeDimension,
  async () => {
    await nextTick()
    if (aminerRadarChart) updateAminerRadarChart()
  },
)

onMounted(async () => {
  restoreDeletedRecordKeys()
  const cached = loadCachedReport()
  if (cached) {
    report.value = normalizeReport(cached)
  }
  assistant.registerContext({
    routeName: 'KnowledgeProfile',
    pageTitle: '学生个性画像',
    profileReportVisible: true,
  })
  await loadReport({ animate: false })
  await nextTick()
  if (!activeDimension.value && radarDimensions.value.length) {
    activeDimension.value = radarDimensions.value[0].name
  }
  await nextTick()
  initCharts()
  window.addEventListener('resize', resizeCharts)
  animateIntro()
})

onBeforeUnmount(() => {
  if (refreshTimer) window.clearTimeout(refreshTimer)
  window.removeEventListener('resize', resizeCharts)
  disposeCharts()
  gsapContext?.revert()
  mediaContext?.revert()
})
</script>

<template>
  <div ref="pageRef" class="profile-report-page">
    <div class="profile-dashboard-inner">
    <section class="profile-hero profile-section profile-animate">
      <div>
        <p class="eyebrow">Student Profile Report</p>
        <h1>学生个性画像</h1>
        <p>{{ profileNarrative }}</p>
      </div>
      <el-button :icon="Refresh" :loading="refreshing || loading" @click="loadReport({ silent: true })">
        刷新画像
      </el-button>
    </section>

    <!-- AMiner 风格：左侧简历 + 右侧维度刻画 -->
    <!-- AMiner 风格：左侧简历 + 右侧能力维度刻画 -->
    <section class="profile-aminer profile-section profile-animate scholar-aminer">
      <!-- 左侧：个人简历 -->
      <aside class="aminer-resume scholar-resume">
        <!-- 顶部信息卡 -->
        <div class="scholar-hero">
          <div class="scholar-avatar">张</div>
          <div class="scholar-hero-info">
            <h2 class="scholar-name">{{ resumeInfo.name }}</h2>
            <p class="scholar-title">{{ resumeInfo.title }}</p>
            <p class="scholar-org">{{ resumeInfo.org }}</p>
          </div>
        </div>

        <!-- 操作栏 -->
        <div class="scholar-actions">
          <span class="scholar-action-btn"><em class="ico-follow"></em>关注</span>
          <span class="scholar-action-btn"><em class="ico-share"></em>分享</span>
        </div>

        <!-- 个人信息 -->
        <div class="scholar-detail">
          <h3 class="detail-heading">个人信息</h3>
          <div class="detail-row"><label>年级</label><span>本科大三</span></div>
          <div class="detail-row"><label>机构</label><span>{{ resumeInfo.org }}</span></div>
                    <div class="detail-row"><label>专业方向</label>
            <span class="research-tags">
              <button type="button" class="research-tag" @click="selectAbility('知识点掌握状态')">编程基础</button>
              <button type="button" class="research-tag" @click="selectAbility('认知层级画像')">算法思维</button>
              <button type="button" class="research-tag" @click="selectAbility('学习主动性')">自主学习</button>
            </span>
          </div>
          <div class="detail-row"><label>邮箱</label><span class="detail-link">{{ resumeInfo.email }}</span></div>
          <div class="detail-row"><label>主页</label><span class="detail-link">{{ resumeInfo.homepage }}</span></div>
        </div>

        <!-- 教育经历 -->
        <div class="scholar-detail">
          <h3 class="detail-heading">教育经历</h3>
          <div class="timeline">
            <div v-for="edu in resumeInfo.education" :key="edu.year + edu.school" class="timeline-item">
              <span class="timeline-year">{{ edu.year }}</span>
              <div class="timeline-content">
                <strong>{{ edu.school }}</strong>
                <span>{{ edu.degree }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 工作经历 -->
        <div class="scholar-detail">
          <h3 class="detail-heading">实习与项目</h3>
          <div class="timeline">
            <div v-for="job in resumeInfo.work" :key="job.year + job.org" class="timeline-item">
              <span class="timeline-year">{{ job.year }}</span>
              <div class="timeline-content">
                <strong>{{ job.org }}</strong>
                <span>{{ job.role }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 个人简介 -->
        <div class="scholar-detail">
          <h3 class="detail-heading">个人简介</h3>
          <p class="scholar-bio">{{ resumeInfo.bio }}</p>
        </div>

        <!-- 核心能力筛选（从简历提取，可点击） -->
        <div class="scholar-abilities">
          <h3 class="detail-heading">核心能力 <span>点击查看维度刻画</span></h3>
          <div class="scholar-ability-list">
            <button
              v-for="a in scholarAbilities"
              :key="a.name"
              type="button"
              :class="['scholar-ability-item', { active: activeAbility?.name === a.name }]"
              :style="{ '--ab': a.color }"
              @click="selectAbility(a.name)"
            >
              <span class="sa-dot" :style="{ background: a.color }"></span>
              <span class="sa-name">{{ a.name }}</span>
              <span class="sa-level" :style="{ color: a.color }">{{ a.level }}</span>
              <span class="sa-score">{{ a.score }}</span>
            </button>
          </div>
        </div>
      </aside>

      <!-- 右侧：能力维度刻画 -->
      <main class="aminer-dimension scholar-dimension">
        <!-- 顶部横幅 -->
        <div class="dim-banner">
          <div class="dim-banner-text">
            <h2>学生画像六维度刻画</h2>
            <p>基于知识点掌握、认知层级、学习主动性、行为规律性、资源偏好与岗位匹配度的多维度画像</p>
          </div>
          <button type="button" class="dim-banner-btn">查看完整报告</button>
        </div>

        <div v-if="activeAbility" class="dim-content">
          <!-- 维度标题 -->
          <div class="dim-head">
            <div class="dim-title">
              <span class="dim-dot" :style="{ background: activeAbility.color }"></span>
              <div class="dim-title-text">
                <h3>{{ activeAbility.name }}</h3>
                <p class="dim-en">{{ activeAbility.enName }}</p>
              </div>
              <span class="dim-level-badge" :style="{ background: activeAbility.color }">{{ activeAbility.level }}</span>
            </div>
            <div class="dim-score-box">
              <strong>{{ activeAbility.score }}</strong><em>分</em>
            </div>
          </div>

          <!-- 维度定义 -->
          <div class="dim-section">
            <h4 class="dim-section-title">维度定义</h4>
            <p class="dim-desc">{{ activeAbility.desc }}</p>
          </div>

          <!-- 子指标 -->
          <div class="dim-section">
            <h4 class="dim-section-title">子指标 <span class="section-sub">共 {{ activeAbility.subIndicators.length }} 项</span></h4>
            <div class="sub-indicator-grid">
              <div v-for="sub in activeAbility.subIndicators" :key="sub.name" class="sub-indicator-card">
                <div class="si-header">
                  <span class="si-name">{{ sub.name }}</span>
                  <span class="si-value" :style="{ color: activeAbility.color }">{{ sub.value }}</span>
                </div>
                <div class="si-bar">
                  <div class="si-bar-fill" :style="{ width: Math.min(sub.value, 100) + '%', background: activeAbility.color }"></div>
                </div>
                <p class="si-desc">{{ sub.desc }}</p>
              </div>
            </div>
          </div>

          <!-- 六维度雷达对比 -->
          <div class="dim-section">
            <h4 class="dim-section-title">六维度对比</h4>
            <div class="dim-radar-box">
              <div ref="aminerRadarChartRef" class="profile-chart" style="width: 100%; min-height: 280px;"></div>
            </div>
          </div>
        </div>
      </main>
    </section>

    <section class="summary-grid profile-section profile-animate live-pulse">
      <article v-for="card in summaryCards" :key="card.label" class="summary-card">
        <div class="summary-icon">
          <el-icon><component :is="card.icon" /></el-icon>
        </div>
        <div>
          <strong>{{ card.value }}{{ card.suffix }}</strong>
          <span>{{ card.label }}</span>
          <em>{{ card.hint }}</em>
        </div>
      </article>
    </section>

    <section class="profile-bento-grid profile-section profile-animate">
      <article class="report-card radar-card live-pulse">
        <div class="card-head">
          <div>
            <p class="eyebrow">Radar</p>
            <h2>学习特征雷达</h2>
          </div>
          <span class="score-pill">{{ report.summary?.profileScore || 0 }}</span>
        </div>

        <div class="radar-layout">
          <div ref="radarChartRef" class="profile-chart radar-chart" style="width: 100%; height: 360px;"></div>
        </div>

        <div class="radar-summary-grid">
          <article v-for="item in radarSummaryCards" :key="item.label" :class="['radar-summary-card', item.tone]">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <p>{{ item.text }}</p>
          </article>
        </div>
      </article>

      <article class="report-card insight-card live-pulse">
        <div class="card-head">
          <div>
            <p class="eyebrow">Insight</p>
            <h2>画像洞察</h2>
          </div>
          <el-tag round effect="light">{{ weakestDimensions.length }} 个重点</el-tag>
        </div>

        <div class="insight-scroll">
        <div class="insight-list">
          <article v-for="item in insights" :key="`${item.type}-${item.title}`" :class="['insight-item', item.type]">
            <div class="insight-item-head">
              <span class="insight-type">{{ item.type }}</span>
              <em>{{ item.priority || '诊断' }}</em>
            </div>
            <h3>{{ item.title }}</h3>
            <p>{{ sanitizeStudentText(item.description) }}</p>
          </article>
        </div>

        <div class="action-panel">
          <h3>下一步建议</h3>
          <div v-for="item in weakestDimensions" :key="item.name" class="action-row">
            <span>{{ item.name }}</span>
            <p>{{ item.action }}</p>
          </div>
        </div>
        </div>
      </article>

      <article class="report-card trend-card live-pulse">
        <div class="card-head">
          <div>
            <p class="eyebrow">Trend</p>
            <h2>学习趋势分析</h2>
          </div>
          <el-icon><TrendCharts /></el-icon>
        </div>
        <div class="trend-panel">
          <div ref="trendChartRef" class="profile-chart trend-chart" style="width: 100%; height: 260px;"></div>
          <aside class="trend-diagnosis-card">
            <span :class="['trend-status', trendSummary.trend === '下降' ? 'down' : trendSummary.trend === '上升' ? 'up' : 'flat']">
              当前趋势：{{ trendSummary.trend }}
            </span>
            <dl>
              <div>
                <dt>最高分</dt>
                <dd>{{ trendSummary.highest }}</dd>
              </div>
              <div>
                <dt>最近分</dt>
                <dd>{{ trendSummary.latest }}</dd>
              </div>
            </dl>
            <p>{{ trendSummary.reason }}</p>
            <strong>{{ trendSummary.advice }}</strong>
          </aside>
        </div>
      </article>

      <article class="report-card diagnosis-card live-pulse">
        <div class="card-head">
          <div>
            <p class="eyebrow">Diagnosis</p>
            <h2>评估诊断</h2>
          </div>
          <el-icon><DataAnalysis /></el-icon>
        </div>

        <div class="diagnosis-toolbar" role="tablist" aria-label="评估诊断视图">
          <button type="button" :class="{ active: diagnosticTab === 'dimension' }" @click="diagnosticTab = 'dimension'">
            画像维度
          </button>
          <button type="button" :class="{ active: diagnosticTab === 'scoring' }" @click="diagnosticTab = 'scoring'">
            评分项诊断
          </button>
        </div>

        <div v-if="diagnosticTab === 'dimension'" class="dimension-diagnosis-grid">
          <div class="distribution-list">
            <div v-for="item in radarDimensions" :key="item.name" class="distribution-row">
              <div>
                <span>{{ item.name }}</span>
                <em>{{ levelText(item.level, item.score) }}</em>
              </div>
              <div class="distribution-track">
                <span :style="{ width: `${item.score}%`, background: item.color }" />
              </div>
              <strong>{{ item.score }}</strong>
            </div>
          </div>
          <div ref="dimensionRadarChartRef" class="profile-chart dimension-mini-radar" style="width: 100%; height: 280px;"></div>
        </div>

        <div v-else class="scoring-diagnosis">
          <p class="scoring-summary">{{ activeScoringAnalysis?.summary }}</p>
          <div class="source-tabs" role="tablist" aria-label="评分来源筛选">
            <button
              v-for="source in [
                { label: '全部', value: 'all' },
                { label: '试卷', value: 'exam' },
                { label: '练习题', value: 'practice' },
              ]"
              :key="source.value"
              type="button"
              :class="{ active: scoringSourceFilter === source.value }"
              @click="scoringSourceFilter = source.value"
            >
              {{ source.label }}
            </button>
          </div>

          <div v-if="scoringItems.length" class="scoring-grid">
            <div ref="scoringBarChartRef" class="profile-chart scoring-bar-chart" style="width: 100%; height: 420px;"></div>
            <div ref="scoringRadarChartRef" class="profile-chart scoring-radar" style="width: 100%; height: 280px;"></div>
          </div>
          <el-empty v-else description="暂无评分项明细" />
        </div>
        <div class="diagnosis-summary">
          <p><span>优势维度</span>{{ diagnosisSummary.best }}体现较好</p>
          <p><span>薄弱维度</span>{{ diagnosisSummary.weak }}需要补强</p>
          <p><span>建议</span>{{ diagnosisSummary.advice }}</p>
        </div>
      </article>
    </section>

    <section class="report-card records-card profile-section profile-animate live-pulse">
      <div class="profile-evidence-grid">
        <div class="records-main">
      <div class="card-head">
        <div>
          <p class="eyebrow">Evidence</p>
          <h2>学习轨迹与画像依据</h2>
          <p class="section-subtitle">系统根据你的作答记录、练习记录和小C对话，实时更新学习画像和下一步学习建议。</p>
        </div>
        <div :class="['profile-sync-status', profileSyncStatus.tone]">
          <strong>{{ profileSyncStatus.label }}</strong>
          <span>最近更新：{{ profileSyncStatus.updatedAt }}</span>
          <em>{{ profileSyncStatus.text }}</em>
        </div>
        <div class="record-tabs" role="tablist" aria-label="记录筛选">
          <button
            v-for="tab in recordTabs"
            :key="tab.value"
            type="button"
            :class="{ active: recordFilter === tab.value }"
            @click="recordFilter = tab.value"
          >
            {{ tab.label }}
          </button>
        </div>
      </div>

      <div class="record-list" :class="{ expanded: evidenceExpanded }">
        <div
          v-for="record in visibleEvidenceRecords"
          :key="`${record.type}-${record.refId || record.title}-${record.time}`"
          class="evidence-row"
        >
          <button type="button" class="record-row-main" @click="openRecord(record)">
            <span class="record-type" :class="record.type">{{ recordTypeText(record.type) }}</span>
            <span class="record-main">
              <span class="record-title-line">
                <strong>{{ record.title }}</strong>
                <i>{{ record.profileStatus }}{{ record.impactEstimated ? '·影响估算' : '' }}</i>
              </span>
              <em>{{ record.summary }}</em>
              <span class="record-impact-tags">
                <b v-for="impact in record.impactChanges" :key="`${record.title}-${impact.name}`">
                  {{ impact.name }} {{ impact.delta > 0 ? `+${impact.delta}` : impact.delta }}
                </b>
              </span>
            </span>
            <span class="record-meta">
              <strong v-if="record.scoreLabel">{{ record.scoreLabel }}</strong>
              <em>{{ record.time }}</em>
            </span>
          </button>
          <div class="record-row-actions">
            <el-button text type="primary" @click.stop="openRecord(record)">{{ record.primaryAction }}</el-button>
            <el-button text @click.stop="openRecord(record)">{{ record.secondaryAction }}</el-button>
            <el-button text class="record-delete-action" :icon="Delete" @click.stop="deleteRecord(record)">删除</el-button>
          </div>
        </div>
        <el-empty v-if="!evidenceRecords.length" description="暂无画像证据" />
      </div>
      <div v-if="evidenceRecords.length > 5" class="evidence-more">
        <el-button text type="primary" @click="evidenceExpanded = !evidenceExpanded">
          {{ evidenceExpanded ? '收起记录' : `查看更多 ${evidenceRecords.length - 5} 条` }}
        </el-button>
      </div>
        </div>

        <aside class="records-side">
          <article class="records-side-card">
            <div class="records-side-head">
              <div>
                <h3>画像更新依据</h3>
                <span>{{ currentRecordTabLabel }}</span>
              </div>
              <div class="profile-change-tags">
                <el-tag size="small" type="warning" effect="light">{{ weakestDimensions[0]?.name || '知识基础' }}待补强</el-tag>
                <el-tag size="small" type="success" effect="light">{{ strongestDimension?.name || '协作表达' }}稳定</el-tag>
                <el-tag size="small" effect="light">理解迁移加强</el-tag>
              </div>
            </div>
            <div class="record-stat-grid">
              <div class="record-stat-chip">
                <el-icon><Document /></el-icon>
                <strong>{{ recordStats.all }}</strong>
                <span>总记录</span>
              </div>
              <div class="record-stat-chip">
                <el-icon><DataAnalysis /></el-icon>
                <strong>{{ recordStats.exam }}</strong>
                <span>试卷</span>
              </div>
              <div class="record-stat-chip">
                <el-icon><TrendCharts /></el-icon>
                <strong>{{ recordStats.practice }}</strong>
                <span>练习</span>
              </div>
              <div class="record-stat-chip">
                <el-icon><ChatDotRound /></el-icon>
                <strong>{{ recordStats.assistant }}</strong>
                <span>小C</span>
              </div>
            </div>
          </article>

          <article class="records-side-card">
            <div class="records-side-head compact">
              <h3>最近动态</h3>
              <span>按时间更新</span>
            </div>
            <el-timeline v-if="recentTimelineRecords.length" class="records-timeline">
              <el-timeline-item
                v-for="record in recentTimelineRecords"
                :key="`${record.type}-${record.refId || record.title}-${record.time}`"
                :timestamp="record.time"
                placement="top"
                :type="record.type === 'assistant' ? 'primary' : 'success'"
              >
                <button type="button" class="timeline-record" @click="openRecord(record)">
                  <strong>{{ record.title }}</strong>
                  <span>{{ record.summary }}</span>
                  <em v-if="record.scoreLabel">{{ record.scoreLabel }}</em>
                </button>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else :image-size="56" description="暂无最近动态" />
          </article>

          <article class="records-side-card">
            <h3>联动建议</h3>
            <div class="action-row compact">
              <span>下一步</span>
              <p>{{ evidenceAdvice }}</p>
            </div>
          </article>
        </aside>
      </div>
    </section>
    </div>

    <el-dialog
      v-model="detailOpen"
      class="profile-record-drawer"
      :title="activeRecord?.type === 'assistant' ? '小C对话详情' : (activeRecord?.title || '记录详情')"
      width="620px"
      append-to-body
      destroy-on-close
    >
      <div v-if="activeRecord" class="record-detail">
        <div class="record-detail-head">
          <span class="record-type" :class="activeRecord.type">{{ recordTypeText(activeRecord.type) }}</span>
          <div>
            <h3>{{ activeRecord.title }}</h3>
            <p v-if="activeRecord.type !== 'assistant'">{{ activeRecord.summary }}</p>
          </div>
        </div>
        <div v-if="activeRecord.type === 'assistant'" class="assistant-qa-detail">
          <section class="qa-block question">
            <span>学生提问</span>
            <p>{{ activeRecord.question || activeAssistantQa.question }}</p>
          </section>
          <section class="qa-block answer">
            <span>小C回复摘要</span>
            <p>{{ activeRecord.answerSummary || activeAssistantQa.answer }}</p>
          </section>
          <section class="qa-block advice">
            <span>学习建议</span>
            <p>{{ activeRecord.advice || '建议结合最近作答继续完成专项练习。' }}</p>
          </section>
          <section class="qa-block knowledge">
            <span>关联知识点</span>
            <div class="detail-tag-list">
              <b v-for="point in activeRecord.relatedKnowledge" :key="point">{{ point }}</b>
            </div>
          </section>
        </div>
        <pre v-else class="record-detail-text">{{ activeRecord.detail || activeRecord.summary }}</pre>
        <dl>
          <div>
            <dt>时间</dt>
            <dd>{{ activeRecord.time || '未记录' }}</dd>
          </div>
          <div v-if="activeRecord.score !== null && activeRecord.score !== undefined">
            <dt>得分</dt>
            <dd>{{ activeRecord.scoreLabel || activeRecord.score }}</dd>
          </div>
          <div>
            <dt>画像影响</dt>
            <dd>
              <span class="detail-impact-list">
                <b v-for="impact in activeRecord.impactChanges" :key="impact.name">
                  {{ impact.name }} {{ impact.delta > 0 ? `+${impact.delta}` : impact.delta }}
                </b>
              </span>
            </dd>
          </div>
        </dl>
        <el-button
          v-if="activeRecord.type !== 'assistant' && activeRecord.refId"
          type="primary"
          @click="goRecord(activeRecord)"
        >
          查看作答结果
        </el-button>
        <el-button text type="danger" :icon="Delete" @click="deleteRecord(activeRecord)">删除这条记录</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<style src="@/styles/knowledge-profile-view.css" scoped></style>
