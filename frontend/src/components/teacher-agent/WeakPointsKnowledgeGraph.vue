<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { gsap } from 'gsap'

const props = defineProps({
  // 学生薄弱知识点列表
  weakPoints: {
    type: Array,
    required: true,
  },
  // 知识图谱数据（中心节点、分组、知识点）
  center: {
    type: Object,
    required: true,
  },
  groups: {
    type: Array,
    required: true,
  },
  points: {
    type: Array,
    required: true,
  },
})

const emit = defineEmits(['select-point'])

const svgRef = ref(null)
const viewportRef = ref(null)
const selectedItem = ref(null)
const hoverId = ref('')
const selectedId = ref('')
const transform = ref({ x: 0, y: 0, scale: 1 })
const isDragging = ref(false)
const dragStart = ref({ x: 0, y: 0 })
const dragFrame = ref(null)
const reduceMotion = ref(false)

// 识别薄弱知识点 ID
const weakPointIds = computed(() => {
  return new Set(props.weakPoints.map(wp => wp.knowledgePointId || wp.id).filter(Boolean))
})

// 筛选出薄弱的知识点节点
const weakPointNodes = computed(() => {
  return props.points.filter(point => weakPointIds.value.has(point.id))
})

// 根据选中的 ID 显示详情
const currentDetail = computed(() => {
  if (!selectedId.value) return null
  const found = props.points.find(p => p.id === selectedId.value)
  return found || props.groups.find(g => g.id === selectedId.value) || props.center
})

// 节点渲染层级权重（值越大越靠顶层）
// 选中/悬停的球 → 最顶层；薄弱点 → 次顶层（标签常显不被遮挡）；普通点 → 底层
function pointZWeight(point) {
  if (hoverId.value === point.id || selectedId.value === point.id) return 3
  if (weakPointIds.value.has(point.id)) return 2
  return 1
}

// 排序后的知识点节点：选中/悬停/薄弱的球始终绘制在最上层，保证文字不被遮挡
const orderedPoints = computed(() => {
  return [...props.points].sort((a, b) => pointZWeight(a) - pointZWeight(b))
})

// 排序后的分组节点：悬停/选中的分组置顶
const orderedGroups = computed(() => {
  return [...props.groups].sort((a, b) => {
    const za = hoverId.value === a.id || selectedId.value === a.id ? 1 : 0
    const zb = hoverId.value === b.id || selectedId.value === b.id ? 1 : 0
    return za - zb
  })
})

// 线条样式
function lineClass(groupId, pointId) {
  const classes = []
  if (weakPointIds.value.has(pointId)) {
    classes.push('highlighted')
  } else if (hoverId.value === pointId) {
    classes.push('muted')
  }
  return classes.filter(Boolean).join(' ')
}

// 缩放控制
function zoomIn() {
  transform.value.scale = Math.min(transform.value.scale + 0.2, 3)
}

function zoomOut() {
  transform.value.scale = Math.max(transform.value.scale - 0.2, 0.4)
}

function fitView() {
  gsap.to(transform.value, {
    x: 0,
    y: 0,
    scale: 1,
    duration: 0.5,
    ease: 'power2.out',
  })
}

function resetView() {
  transform.value = { x: 0, y: 0, scale: 1 }
}

// 拖拽控制：仅在背景处按下后拖动才移动图谱，松开立即停止
// pointerDown = 是否已按下（非节点处）；suppressClick = 拖动结束后短暂抑制点击
const pointerDown = ref(false)
let suppressClick = false

function onWheel(e) {
  e.preventDefault()
  const delta = e.deltaY > 0 ? -0.1 : 0.1
  transform.value.scale = Math.max(0.4, Math.min(3, transform.value.scale + delta))
}

function onPointerDown(e) {
  if (e.button !== 0) return
  // 仅在非节点处（背景/空白区域）按下才启动拖拽平移；
  // 节点区域保持悬停与点击交互，不参与拖动
  if (e.target.closest?.('.center-node, .group-node, .point-node')) return
  pointerDown.value = true
  isDragging.value = false
  dragStart.value = { x: e.clientX, y: e.clientY }
  document.addEventListener('pointermove', onPointerMove)
  document.addEventListener('pointerup', onPointerUp)
}

// 移动只发生在“已按下”之后；未按下时（鼠标悬停滑过）不做任何平移
function onPointerMove(e) {
  if (!pointerDown.value || !dragStart.value) return
  const dx = e.clientX - dragStart.value.x
  const dy = e.clientY - dragStart.value.y
  if (!isDragging.value && Math.abs(dx) + Math.abs(dy) > 3) {
    isDragging.value = true
  }
  if (!isDragging.value) return
  transform.value.x += dx
  transform.value.y += dy
  dragStart.value = { x: e.clientX, y: e.clientY }
}

// 松开后立即停止移动（不再跟随）
function onPointerUp() {
  pointerDown.value = false
  suppressClick = isDragging.value
  isDragging.value = false
  document.removeEventListener('pointermove', onPointerMove)
  document.removeEventListener('pointerup', onPointerUp)
  // 短暂抑制紧随其后的 click，避免“拖动后松手”误触发节点选中
  setTimeout(() => { suppressClick = false }, 60)
}

// 选中节点（拖动后松手不触发选中）
function handlePointClick(point) {
  if (suppressClick || isDragging.value) return
  selectedId.value = point.id
  emit('select-point', point)
}

// 初始化动画
function animateIntro() {
  if (reduceMotion.value) return

  gsap.fromTo(viewportRef.value,
    { opacity: 0, scale: 0.8 },
    { opacity: 1, scale: 1, duration: 0.8, ease: 'power2.out' }
  )
}

let mediaContext = null

onMounted(() => {
  mediaContext = gsap.matchMedia()
  mediaContext.add('(prefers-reduced-motion: reduce)', ({ conditions }) => {
    reduceMotion.value = Boolean(conditions.reduceMotion)
  })

  if (!reduceMotion.value) {
    setTimeout(animateIntro, 100)
  }
})

onUnmounted(() => {
  mediaContext?.revert()
  if (dragFrame.value) window.cancelAnimationFrame(dragFrame.value)
})
</script>

<template>
  <div class="weak-points-graph-canvas"
       @wheel.prevent="onWheel"
       @pointerdown="onPointerDown"
       @pointerup="onPointerUp"
       @pointercancel="onPointerUp"
  >
    <svg ref="svgRef" viewBox="0 0 1600 1000" role="img" aria-label="学科知识图谱">
      <defs>
        <!-- 薄弱知识点高亮渐变（柔和玫瑰红） -->
        <radialGradient id="weakGlow" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="#fecdd3" />
          <stop offset="45%" stop-color="#f43f5e" />
          <stop offset="100%" stop-color="#be123c" />
        </radialGradient>

        <!-- 普通知识点 / 分组渐变（靛蓝） -->
        <radialGradient id="nodeGlow" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="#c7d2fe" />
          <stop offset="55%" stop-color="#6366f1" />
          <stop offset="100%" stop-color="#4338ca" />
        </radialGradient>

        <!-- 中心节点渐变（深靛蓝） -->
        <radialGradient id="centerGlow" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="#e0e7ff" />
          <stop offset="45%" stop-color="#4f46e5" />
          <stop offset="100%" stop-color="#312e81" />
        </radialGradient>

        <!-- 背景中心光晕（浅靛蓝） -->
        <radialGradient id="ambientGlow" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="rgba(99, 102, 241, 0.18)" />
          <stop offset="45%" stop-color="rgba(99, 102, 241, 0.08)" />
          <stop offset="100%" stop-color="rgba(99, 102, 241, 0)" />
        </radialGradient>

        <!-- 柔和发光 -->
        <filter id="softGlow" x="-80%" y="-80%" width="260%" height="260%">
          <feGaussianBlur stdDeviation="5" result="blur" />
          <feMerge>
            <feMergeNode in="blur" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>

        <!-- 强发光（薄弱点） -->
        <filter id="strongGlow" x="-90%" y="-90%" width="280%" height="280%">
          <feGaussianBlur stdDeviation="7" result="blur" />
          <feMerge>
            <feMergeNode in="blur" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>
      </defs>

      <!-- 背景光晕 + 轨道网格 -->
      <g class="kg-ambient">
        <circle class="ambient-glow" cx="800" cy="500" r="380" />
        <circle class="orbit orbit-outer" cx="800" cy="500" r="450" />
        <circle class="orbit" cx="800" cy="500" r="350" />
        <circle class="orbit" cx="800" cy="500" r="250" />
        <g class="grid-lines">
          <line v-for="i in 12" :key="i"
            :x1="800 + 450 * Math.cos(i * 30 * Math.PI / 180)"
            :y1="500 + 450 * Math.sin(i * 30 * Math.PI / 180)"
            :x2="800 + 500 * Math.cos(i * 30 * Math.PI / 180)"
            :y2="500 + 500 * Math.sin(i * 30 * Math.PI / 180)"
          />
        </g>
      </g>

      <!-- 视图变换组 -->
      <g ref="viewportRef"
         :style="{ transform: `translate(${transform.x}px, ${transform.y}px) scale(${transform.scale})`, transformOrigin: '800px 500px' }"
      >
        <!-- 连线 -->
        <g class="links">
          <!-- 中心到分组的连线 -->
          <line v-for="group in groups" :key="`center-${group.id}`"
            class="graph-link group-link"
            :class="lineClass('center', group.id)"
            :x1="800"
            :y1="500"
            :x2="group.x"
            :y2="group.y"
          />

          <!-- 分组到知识点的连线 -->
          <line v-for="point in points" :key="`point-${point.id}`"
            class="graph-link point-link"
            :class="lineClass(point.groupId, point.id)"
            :data-group-id="point.groupId"
            :x1="(groups.find(g => g.id === point.groupId) || {}).x || 0"
            :y1="(groups.find(g => g.id === point.groupId) || {}).y || 0"
            :x2="point.x"
            :y2="point.y"
          />
        </g>

        <!-- 中心节点 -->
        <g class="center-node"
           tabindex="0"
           @click="selectedItem = center"
           @mouseenter="hoverId = 'center'"
           @mouseleave="hoverId = ''"
        >
          <circle cx="800" cy="500" r="104" class="center-aura" />
          <circle cx="800" cy="500" r="82" class="center-glow" />
          <circle cx="800" cy="500" r="62" class="center-core" />
          <text x="800" y="494" class="center-title">{{ center.name }}</text>
          <text x="800" y="522" class="center-sub">学科知识图谱</text>
        </g>

        <!-- 分组节点（悬停/选中的置顶） -->
        <g v-for="group in orderedGroups" :key="`group-${group.id}`"
           class="group-node"
           :class="{ 'is-active': hoverId === group.id || selectedId === group.id }"
           :data-group-id="group.id"
           @mouseenter="hoverId = group.id"
           @mouseleave="hoverId = ''"
        >
          <circle :cx="group.x" :cy="group.y" r="52" class="group-halo" />
          <circle :cx="group.x" :cy="group.y" r="38" class="group-core" />
          <text :x="group.x" :y="group.y" class="group-label">{{ group.name }}</text>
        </g>

        <!-- 知识点节点（选中/悬停/薄弱的球置顶，避免文字被遮挡） -->
        <g v-for="point in orderedPoints" :key="`point-${point.id}`"
           class="point-node"
           :data-point-id="point.id"
           :class="{ 'is-weak': weakPointIds.has(point.id), 'is-active': hoverId === point.id || selectedId === point.id }"
           @click="handlePointClick(point)"
           @mouseenter="hoverId = point.id"
           @mouseleave="hoverId = ''"
        >
          <!-- 薄弱知识点：红色高亮节点 -->
          <circle v-if="weakPointIds.has(point.id)"
            :cx="point.x"
            :cy="point.y"
            r="23"
            fill="url(#weakGlow)"
            class="weak-core"
          />
          <!-- 普通知识点 -->
          <circle v-else
            :cx="point.x"
            :cy="point.y"
            r="12"
            fill="url(#nodeGlow)"
            class="point-core"
          />

          <!-- 知识点标签：薄弱点常显，普通点悬停时显示 -->
          <text v-if="!reduceMotion && (weakPointIds.has(point.id) || hoverId === point.id)"
            :x="point.x"
            :y="point.y"
            class="point-label"
          >{{ point.name.slice(0, 8) }}</text>
        </g>
      </g>
    </svg>

    <!-- 控制按钮 -->
    <div class="canvas-controls">
      <button @click="zoomIn" title="放大">+</button>
      <button @click="fitView" title="适应视图">⤢</button>
      <button @click="zoomOut" title="缩小">−</button>
      <button @click="resetView" title="复位">⟳</button>
    </div>

    <!-- 统计信息 -->
    <div class="canvas-stats">
      <span>分组 <strong>{{ groups.length }}</strong></span>
      <span>知识点 <strong>{{ points.length }}</strong></span>
      <span v-if="weakPointIds.size > 0" class="weak-stat">
        高亮 <strong>{{ weakPointIds.size }}</strong>
      </span>
    </div>

    <!-- 左下角固定：交互提示条 -->
    <div class="kg-tipbar">
      <div class="tip-main">拖动 / 滚轮缩放，点击知识点查看</div>
    </div>
  </div>
</template>

<style scoped>
.weak-points-graph-canvas {
  position: relative;
  width: 100%;
  height: 600px;
  background:
    radial-gradient(circle at 50% 30%, rgba(99, 102, 241, 0.08) 0%, transparent 50%),
    radial-gradient(circle at 82% 78%, rgba(59, 130, 246, 0.06) 0%, transparent 45%),
    linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.05);
  cursor: grab;
  touch-action: none;
  user-select: none;
  -webkit-user-select: none;
}

.weak-points-graph-canvas:active {
  cursor: grabbing;
}

svg {
  width: 100%;
  height: 100%;
  display: block;
  user-select: none;
  -webkit-user-select: none;
}

/* ===== 背景 ===== */
.kg-ambient {
  pointer-events: none;
}

.ambient-glow {
  fill: url(#ambientGlow);
  animation: ambientBreath 6s ease-in-out infinite;
}

@keyframes ambientBreath {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

.orbit {
  fill: none;
  stroke: rgba(99, 102, 241, 0.12);
  stroke-width: 1;
  stroke-dasharray: 6 14;
}

.orbit-outer {
  stroke: rgba(99, 102, 241, 0.16);
}

.grid-lines line {
  stroke: rgba(148, 163, 184, 0.1);
  stroke-width: 1;
}

/* ===== 连线 ===== */
.graph-link {
  transition: stroke 0.25s ease, opacity 0.25s ease, stroke-width 0.25s ease;
}

.group-link {
  stroke: rgba(99, 102, 241, 0.35);
  stroke-width: 1.6;
}

.point-link {
  stroke: rgba(148, 163, 184, 0.28);
  stroke-width: 1;
}

.graph-link.muted {
  opacity: 0.35;
}

/* 薄弱点连线：柔和玫瑰红高亮 */
.graph-link.highlighted {
  stroke: #f43f5e !important;
  stroke-width: 2.2 !important;
  filter: drop-shadow(0 0 5px rgba(244, 63, 94, 0.45));
  animation: pulseLink 1.6s ease-in-out infinite;
}

@keyframes pulseLink {
  0%, 100% { opacity: 0.9; }
  50% { opacity: 0.55; }
}

/* ===== 中心节点 ===== */
.center-node {
  outline: none;
  cursor: pointer;
}

.center-aura {
  fill: rgba(99, 102, 241, 0.1);
  animation: centerPulse 4s ease-in-out infinite;
}

@keyframes centerPulse {
  0%, 100% { opacity: 0.7; transform: scale(1); transform-origin: 800px 500px; }
  50% { opacity: 1; transform: scale(1.05); transform-origin: 800px 500px; }
}

.center-glow {
  fill: rgba(99, 102, 241, 0.18);
}

.center-core {
  fill: url(#centerGlow);
  stroke: rgba(199, 210, 254, 0.9);
  stroke-width: 1.6;
  filter: url(#softGlow);
  transition: stroke 0.2s ease, stroke-width 0.2s ease;
}

.center-node:hover .center-core,
.center-node:focus-visible .center-core {
  stroke: #ffffff;
  stroke-width: 2.2;
  filter: drop-shadow(0 0 14px rgba(79, 70, 229, 0.55));
}

.center-title {
  fill: #ffffff;
  text-anchor: middle;
  font-size: 21px;
  font-weight: 900;
  pointer-events: none;
  paint-order: stroke;
  stroke: rgba(49, 46, 129, 0.55);
  stroke-width: 3px;
  stroke-linejoin: round;
}

.center-sub {
  fill: #c7d2fe;
  text-anchor: middle;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 3px;
  pointer-events: none;
}

/* ===== 分组节点 ===== */
.group-node {
  outline: none;
  cursor: pointer;
}

.group-halo {
  fill: rgba(99, 102, 241, 0.22);
  opacity: 0.6;
}

.group-core {
  fill: url(#nodeGlow);
  stroke: rgba(199, 210, 254, 0.9);
  stroke-width: 1.3;
  filter: url(#softGlow);
  transition: stroke 0.18s ease, stroke-width 0.18s ease;
}

.group-node:hover .group-core,
.group-node:focus-visible .group-core {
  stroke: #ffffff;
  stroke-width: 2;
  filter: drop-shadow(0 0 10px rgba(79, 70, 229, 0.5));
}

/* 选中/悬停的分组：靛蓝高亮 */
.group-node.is-active .group-core {
  stroke: #4f46e5;
  stroke-width: 2.6;
  filter: drop-shadow(0 0 14px rgba(79, 70, 229, 0.6));
}

.group-node.is-active .group-halo {
  fill: rgba(79, 70, 229, 0.25);
  opacity: 1;
}

.group-label {
  fill: #334155;
  text-anchor: middle;
  dominant-baseline: middle;
  font-size: 14px;
  font-weight: 800;
  pointer-events: none;
  paint-order: stroke;
  stroke: rgba(255, 255, 255, 0.9);
  stroke-width: 3px;
  stroke-linejoin: round;
}

/* ===== 知识点节点 ===== */
.point-node {
  outline: none;
  cursor: pointer;
}

.point-core {
  fill: url(#nodeGlow);
  stroke: rgba(199, 210, 254, 0.8);
  stroke-width: 1;
  transition: r 0.2s ease, stroke 0.2s ease, stroke-width 0.2s ease;
}

.point-node:hover .point-core,
.point-node:focus-visible .point-core {
  r: 18;
  stroke: #ffffff;
  stroke-width: 1.6;
  filter: drop-shadow(0 0 8px rgba(79, 70, 229, 0.45));
}

.point-label {
  fill: #1e293b;
  text-anchor: middle;
  dominant-baseline: middle;
  font-size: 11px;
  font-weight: 800;
  pointer-events: none;
  paint-order: stroke;
  stroke: rgba(255, 255, 255, 0.92);
  stroke-width: 3px;
  stroke-linejoin: round;
}

/* 薄弱知识点：柔和玫瑰红高亮 */
.weak-core {
  stroke: #fecdd3;
  stroke-width: 1.6;
  filter: url(#strongGlow);
  animation: weakPulse 1.6s ease-in-out infinite;
}

@keyframes weakPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.75; }
}

.point-node.is-weak .point-label {
  fill: #881337;
  font-size: 12px;
}

.point-node.is-weak:hover .weak-core,
.point-node.is-weak:focus-visible .weak-core {
  r: 28;
  stroke: #ffffff;
  stroke-width: 2.2;
}

/* 选中/悬停的球：靛蓝高亮 */
.point-node.is-active .point-core {
  r: 20;
  stroke: #4f46e5;
  stroke-width: 2.2;
  filter: drop-shadow(0 0 10px rgba(79, 70, 229, 0.55));
}

.point-node.is-active .weak-core {
  r: 30;
  stroke: #ffffff;
  stroke-width: 2.4;
  filter: drop-shadow(0 0 16px rgba(244, 63, 94, 0.7));
}

.point-node.is-active .point-label {
  font-size: 13px;
  fill: #312e81;
}

/* ===== 控制按钮 ===== */
.canvas-controls {
  position: absolute;
  bottom: 18px;
  right: 18px;
  display: flex;
  gap: 8px;
  padding: 8px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.08);
}

.canvas-controls button {
  width: 34px;
  height: 34px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  color: #475569;
  border-radius: 8px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 700;
  transition: all 0.2s ease;
}

.canvas-controls button:hover {
  background: #eef2ff;
  border-color: #c7d2fe;
  color: #4f46e5;
  transform: scale(1.06);
}

.canvas-controls button:active {
  transform: scale(0.95);
}

/* ===== 统计信息 ===== */
.canvas-stats {
  position: absolute;
  top: 18px;
  left: 18px;
  display: flex;
  gap: 18px;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.08);
  color: #64748b;
  font-size: 13px;
}

.canvas-stats strong {
  color: #4f46e5;
  font-size: 15px;
}

.canvas-stats .weak-stat strong {
  color: #e11d48;
}

/* ===== 左下角固定：交互提示条 ===== */
.kg-tipbar {
  position: absolute;
  left: 18px;
  bottom: 18px;
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 10px 16px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.95) 0%, rgba(99, 102, 241, 0.92) 100%);
  border: 1px solid rgba(199, 210, 254, 0.5);
  box-shadow: 0 6px 18px rgba(79, 70, 229, 0.28);
  color: #ffffff;
  font-size: 12px;
  line-height: 1.5;
  pointer-events: none;
  user-select: none;
  -webkit-user-select: none;
}

.kg-tipbar .tip-main {
  white-space: nowrap;
  letter-spacing: 0.2px;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .canvas-controls {
    bottom: 10px;
    right: 10px;
  }

  .canvas-stats {
    top: 10px;
    left: 10px;
    font-size: 11px;
  }

  .kg-tipbar {
    left: 10px;
    bottom: 10px;
    font-size: 11px;
    padding: 8px 12px;
  }
}
</style>
