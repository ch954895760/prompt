<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Sparkles, X, Check, AlertCircle, Lightbulb, Layers, Eye, FileText, Star, Columns, RefreshCw, ChevronDown, ChevronUp, Clock, Cpu, Hash } from 'lucide-vue-next'
import type { PromptOptimizeResponse, OptimizeSuggestion } from '@/types'
import { optimizePrompt } from '@/api/promptOptimizer'

const props = defineProps<{
  modelValue: boolean
  currentPrompt: string
  providerId?: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'apply': [optimizedPrompt: string]
}>()

const loading = ref(false)
const error = ref('')
const optimizeResult = ref<PromptOptimizeResponse | null>(null)
const showComparison = ref(false)
const expandedSuggestions = ref<Set<number>>(new Set())

const scoreColor = computed(() => {
  const score = optimizeResult.value?.score || 0
  if (score >= 8) return '#22c55e'
  if (score >= 5) return '#f59e0b'
  return '#ef4444'
})

const scoreBgColor = computed(() => {
  const score = optimizeResult.value?.score || 0
  if (score >= 8) return 'rgba(34, 197, 94, 0.1)'
  if (score >= 5) return 'rgba(245, 158, 11, 0.1)'
  return 'rgba(239, 68, 68, 0.1)'
})

const scoreLabel = computed(() => {
  const score = optimizeResult.value?.score || 0
  if (score >= 9) return '优秀'
  if (score >= 8) return '良好'
  if (score >= 6) return '一般'
  if (score >= 4) return '需改进'
  return '较差'
})

const scoreProgressWidth = computed(() => {
  return `${(optimizeResult.value?.score || 0) * 10}%`
})

const hasOptimizedContent = computed(() => {
  return !!optimizeResult.value?.optimizedPrompt?.trim()
})

const formatOptimizationTime = (ms?: number): string => {
  if (ms === undefined || ms === null) return '-';
  if (ms < 1000) return `${ms}ms`;
  return `${(ms / 1000).toFixed(2)}s`;
}

const formatTokens = (tokens?: number): string => {
  if (tokens === undefined || tokens === null) return '-';
  if (tokens >= 1000) return `${(tokens / 1000).toFixed(1)}k`;
  return `${tokens}`;
}

function getSuggestionIcon(type: string) {
  switch (type) {
    case 'structure': return Layers
    case 'clarity': return Eye
    case 'example': return FileText
    default: return Lightbulb
  }
}

function getSuggestionTypeLabel(type: string) {
  switch (type) {
    case 'structure': return '结构优化'
    case 'clarity': return '清晰度改进'
    case 'example': return '示例补充'
    default: return '优化建议'
  }
}

function getPriorityColor(priority: string) {
  switch (priority) {
    case 'high': return '#ef4444'
    case 'medium': return '#f59e0b'
    case 'low': return '#3b82f6'
    default: return '#6b7280'
  }
}

function getPriorityBgColor(priority: string) {
  switch (priority) {
    case 'high': return 'rgba(239, 68, 68, 0.1)'
    case 'medium': return 'rgba(245, 158, 11, 0.1)'
    case 'low': return 'rgba(59, 130, 246, 0.1)'
    default: return 'rgba(107, 114, 128, 0.1)'
  }
}

function getPriorityLabel(priority: string) {
  switch (priority) {
    case 'high': return '高优先级'
    case 'medium': return '中优先级'
    case 'low': return '低优先级'
    default: return '一般'
  }
}

function toggleSuggestion(index: number) {
  if (expandedSuggestions.value.has(index)) {
    expandedSuggestions.value.delete(index)
  } else {
    expandedSuggestions.value.add(index)
  }
}

async function handleOptimize(forceRefresh = false) {
  if (!props.currentPrompt.trim()) {
    error.value = '请先输入提示词内容'
    return
  }

  loading.value = true
  error.value = ''
  optimizeResult.value = null
  expandedSuggestions.value.clear()

  try {
    optimizeResult.value = await optimizePrompt({
      promptContent: props.currentPrompt,
      providerId: props.providerId ?? undefined,
      forceRefresh: forceRefresh
    })
  } catch (e: any) {
    error.value = e.message || '优化失败，请重试'
  } finally {
    loading.value = false
  }
}

function applyOptimization() {
  if (optimizeResult.value?.optimizedPrompt) {
    emit('apply', optimizeResult.value.optimizedPrompt)
    close()
  }
}

function close() {
  emit('update:modelValue', false)
  optimizeResult.value = null
  expandedSuggestions.value.clear()
  showComparison.value = false
  error.value = ''
}

function onMaskClick(e: MouseEvent) {
  if (e.target === e.currentTarget) close()
}

watch(() => props.modelValue, (newVal) => {
  if (newVal && props.currentPrompt.trim() && !optimizeResult.value) {
    handleOptimize(false) // 打开面板时使用缓存
  }
})
</script>

<template>
  <Teleport to="body">
    <Transition name="optimizer">
      <div v-if="modelValue" class="fixed inset-0 z-[100] flex items-center justify-center" @click="onMaskClick">
        <div class="absolute inset-0 bg-black/50 backdrop-blur-sm transition-opacity optimizer-backdrop"></div>

        <div class="relative w-full max-w-[700px] mx-4 rounded-2xl flex flex-col optimizer-panel"
          style="background: var(--bg-primary); border: 1px solid var(--border-color); box-shadow: 0 24px 80px rgba(0,0,0,0.25); max-height: 85vh;"
        >
          <!-- Header -->
          <div class="flex items-center justify-between px-6 py-4 shrink-0"
            style="border-bottom: 1px solid var(--border-color);"
          >
            <div class="flex items-center gap-3">
              <div class="w-9 h-9 rounded-xl flex items-center justify-center"
                style="background: linear-gradient(135deg, var(--accent) 0%, #c2410c 100%);"
              >
                <Sparkles class="w-5 h-5 text-white" />
              </div>
              <div>
                <h3 class="text-base font-semibold" style="color: var(--text-primary)">提示词优化助手</h3>
                <p class="text-xs" style="color: var(--text-muted)">AI 辅助分析和优化提示词</p>
              </div>
            </div>
            <div class="flex items-center gap-2">
              <button v-if="optimizeResult" @click="handleOptimize(true)"
                class="p-2 rounded-lg transition-colors"
                style="color: var(--text-muted);"
                :disabled="loading"
                @mouseenter="($event.currentTarget as HTMLElement).style.background = 'var(--bg-tertiary)'"
                @mouseleave="($event.currentTarget as HTMLElement).style.background = 'transparent'"
                title="重新分析"
              >
                <RefreshCw class="w-4 h-4" :class="{ 'animate-spin': loading }" />
              </button>
              <button @click="close"
                class="p-2 rounded-lg transition-colors"
                style="color: var(--text-muted);"
                @mouseenter="($event.currentTarget as HTMLElement).style.background = 'var(--bg-tertiary)'"
                @mouseleave="($event.currentTarget as HTMLElement).style.background = 'transparent'"
              >
                <X class="w-5 h-5" />
              </button>
            </div>
          </div>

          <!-- Content -->
          <div class="flex-1 overflow-y-auto p-6">
            <!-- Loading State -->
            <div v-if="loading && !optimizeResult" class="flex flex-col items-center justify-center py-12">
              <div class="w-16 h-16 rounded-2xl flex items-center justify-center mb-4"
                style="background: linear-gradient(135deg, var(--accent-soft) 0%, rgba(234, 88, 12, 0.2) 100%);"
              >
                <Sparkles class="w-8 h-8 animate-pulse" style="color: var(--accent);" />
              </div>
              <p class="text-sm font-medium mb-1" style="color: var(--text-primary)">正在分析提示词...</p>
              <p class="text-xs" style="color: var(--text-muted)">AI 正在评估质量和生成优化建议</p>
            </div>

            <!-- Error State -->
            <div v-else-if="error" class="flex flex-col items-center justify-center py-12">
              <div class="w-16 h-16 rounded-2xl flex items-center justify-center mb-4"
                style="background: rgba(239, 68, 68, 0.1);"
              >
                <AlertCircle class="w-8 h-8" style="color: #ef4444;" />
              </div>
              <p class="text-sm font-medium mb-1" style="color: var(--text-primary)">分析失败</p>
              <p class="text-xs text-center max-w-[300px]" style="color: var(--text-muted)">{{ error }}</p>
              <button @click="handleOptimize(true)"
                class="mt-4 px-4 py-2 rounded-lg text-sm font-medium transition-colors"
                style="background: var(--accent); color: white;"
              >
                重试
              </button>
            </div>

            <!-- Result State -->
            <div v-else-if="optimizeResult" class="space-y-5">
              <!-- Score Card -->
              <div class="rounded-xl p-5"
                :style="{ background: scoreBgColor, border: `1px solid ${scoreColor}30` }"
              >
                <div class="flex items-center justify-between mb-3">
                  <div class="flex items-center gap-2">
                    <Star class="w-5 h-5" :style="{ color: scoreColor }" />
                    <span class="text-sm font-medium" style="color: var(--text-primary)">质量评分</span>
                  </div>
                  <span v-if="optimizeResult.fromCache" class="text-[10px] px-2 py-0.5 rounded-full"
                    style="background: var(--bg-secondary); color: var(--text-muted);"
                  >
                    来自缓存
                  </span>
                </div>
                <div class="flex items-end gap-2 mb-2">
                  <span class="text-4xl font-bold" :style="{ color: scoreColor }">{{ optimizeResult.score }}</span>
                  <span class="text-lg mb-1" :style="{ color: scoreColor }">/10</span>
                  <span class="text-sm ml-2 mb-1.5 px-2 py-0.5 rounded-md font-medium"
                    :style="{ background: scoreColor + '20', color: scoreColor }"
                  >
                    {{ scoreLabel }}
                  </span>
                </div>
                <div class="h-2 rounded-full overflow-hidden mb-3"
                  style="background: var(--bg-secondary);"
                >
                  <div class="h-full rounded-full transition-all duration-500"
                    :style="{ width: scoreProgressWidth, background: scoreColor }"
                  ></div>
                </div>
                <p class="text-sm" style="color: var(--text-secondary)">{{ optimizeResult.analysis }}</p>

                <!-- 统计信息 -->
                <div class="flex items-center gap-4 mt-4 pt-4" style="border-top: 1px solid var(--border-color);">
                  <div class="flex items-center gap-1.5">
                    <Clock class="w-3.5 h-3.5" style="color: var(--text-muted);" />
                    <span class="text-xs" style="color: var(--text-muted);">耗时</span>
                    <span class="text-xs font-medium" style="color: var(--text-primary);">
                      {{ formatOptimizationTime(optimizeResult.optimizationTime) }}
                    </span>
                  </div>
                  <div class="flex items-center gap-1.5">
                    <Cpu class="w-3.5 h-3.5" style="color: var(--text-muted);" />
                    <span class="text-xs" style="color: var(--text-muted);">模型</span>
                    <span class="text-xs font-medium truncate max-w-[120px]" style="color: var(--text-primary);">
                      {{ optimizeResult.modelUsed || '-' }}
                    </span>
                  </div>
                  <div class="flex items-center gap-1.5">
                    <Hash class="w-3.5 h-3.5" style="color: var(--text-muted);" />
                    <span class="text-xs" style="color: var(--text-muted);">Token</span>
                    <span class="text-xs font-medium" style="color: var(--text-primary);">
                      {{ formatTokens(optimizeResult.tokensConsumed) }}
                    </span>
                  </div>
                </div>
              </div>

              <!-- Suggestions -->
              <div>
                <h4 class="text-sm font-medium mb-3 flex items-center gap-2" style="color: var(--text-primary)">
                  <Lightbulb class="w-4 h-4" style="color: var(--accent);" />
                  优化建议
                </h4>
                <div class="space-y-3">
                  <div v-for="(suggestion, index) in optimizeResult.suggestions" :key="index"
                    class="rounded-xl overflow-hidden transition-all"
                    style="background: var(--bg-secondary); border: 1px solid var(--border-color);"
                  >
                    <div class="p-4 cursor-pointer"
                      @click="toggleSuggestion(index)"
                    >
                      <div class="flex items-start gap-3">
                        <div class="w-8 h-8 rounded-lg flex items-center justify-center shrink-0"
                          :style="{ background: getPriorityBgColor(suggestion.priority) }"
                        >
                          <component :is="getSuggestionIcon(suggestion.type)" class="w-4 h-4"
                            :style="{ color: getPriorityColor(suggestion.priority) }"
                          />
                        </div>
                        <div class="flex-1 min-w-0">
                          <div class="flex items-center gap-2 mb-1">
                            <span class="text-sm font-medium truncate" style="color: var(--text-primary)">
                              {{ suggestion.title }}
                            </span>
                            <span class="text-[10px] px-1.5 py-0.5 rounded shrink-0"
                              :style="{ background: getPriorityBgColor(suggestion.priority), color: getPriorityColor(suggestion.priority) }"
                            >
                              {{ getPriorityLabel(suggestion.priority) }}
                            </span>
                          </div>
                          <p class="text-xs truncate" style="color: var(--text-muted)">
                            {{ getSuggestionTypeLabel(suggestion.type) }}
                          </p>
                        </div>
                        <component :is="expandedSuggestions.has(index) ? ChevronUp : ChevronDown"
                          class="w-4 h-4 shrink-0 transition-transform"
                          style="color: var(--text-muted);"
                        />
                      </div>
                    </div>
                    <div v-if="expandedSuggestions.has(index)"
                      class="px-4 pb-4"
                      style="border-top: 1px solid var(--border-color);"
                    >
                      <p class="text-sm pt-3" style="color: var(--text-secondary)">
                        {{ suggestion.description }}
                      </p>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Comparison -->
              <div v-if="hasOptimizedContent">
                <div class="flex items-center justify-between mb-3">
                  <h4 class="text-sm font-medium flex items-center gap-2" style="color: var(--text-primary)">
                    <Columns class="w-4 h-4" style="color: var(--accent);" />
                    优化对比
                  </h4>
                  <button @click="showComparison = !showComparison"
                    class="text-xs flex items-center gap-1 transition-colors"
                    style="color: var(--accent);"
                  >
                    {{ showComparison ? '收起' : '展开' }}
                    <component :is="showComparison ? ChevronUp : ChevronDown" class="w-3 h-3" />
                  </button>
                </div>
                <div v-if="showComparison" class="grid grid-cols-2 gap-3">
                  <div class="rounded-xl p-4"
                    style="background: var(--bg-secondary); border: 1px solid var(--border-color);"
                  >
                    <div class="flex items-center gap-2 mb-2">
                      <span class="text-[10px] px-2 py-0.5 rounded font-medium"
                        style="background: var(--bg-tertiary); color: var(--text-muted);"
                      >优化前</span>
                    </div>
                    <pre class="text-xs whitespace-pre-wrap break-words" style="color: var(--text-secondary); max-height: 200px; overflow-y: auto;">{{ optimizeResult.originalPrompt || '无内容' }}</pre>
                  </div>
                  <div class="rounded-xl p-4"
                    style="background: rgba(34, 197, 94, 0.05); border: 1px solid rgba(34, 197, 94, 0.3);"
                  >
                    <div class="flex items-center gap-2 mb-2">
                      <span class="text-[10px] px-2 py-0.5 rounded font-medium"
                        style="background: rgba(34, 197, 94, 0.15); color: #22c55e;"
                      >优化后</span>
                    </div>
                    <pre class="text-xs whitespace-pre-wrap break-words" style="color: var(--text-primary); max-height: 200px; overflow-y: auto;">{{ optimizeResult.optimizedPrompt }}</pre>
                  </div>
                </div>
              </div>
            </div>

            <!-- Empty State -->
            <div v-else class="flex flex-col items-center justify-center py-12">
              <div class="w-16 h-16 rounded-2xl flex items-center justify-center mb-4"
                style="background: var(--bg-secondary);"
              >
                <Sparkles class="w-8 h-8" style="color: var(--text-muted);" />
              </div>
              <p class="text-sm font-medium mb-1" style="color: var(--text-primary)">开始优化</p>
              <p class="text-xs text-center max-w-[300px]" style="color: var(--text-muted)">
                点击下方按钮，AI 将分析您的提示词并提供优化建议
              </p>
              <button @click="handleOptimize(false)"
                class="mt-4 px-5 py-2.5 rounded-xl text-sm font-medium transition-all flex items-center gap-2"
                style="background: var(--accent); color: white;"
              >
                <Sparkles class="w-4 h-4" />
                开始分析
              </button>
            </div>
          </div>

          <!-- Footer -->
          <div v-if="optimizeResult && hasOptimizedContent" class="px-6 py-4 shrink-0 flex items-center justify-end gap-3"
            style="border-top: 1px solid var(--border-color);"
          >
            <button @click="close"
              class="px-4 py-2.5 rounded-xl text-sm font-medium transition-colors"
              style="color: var(--text-secondary);"
              @mouseenter="($event.currentTarget as HTMLElement).style.background = 'var(--bg-tertiary)'"
              @mouseleave="($event.currentTarget as HTMLElement).style.background = 'transparent'"
            >
              取消
            </button>
            <button @click="applyOptimization"
              class="px-5 py-2.5 rounded-xl text-sm font-medium transition-all flex items-center gap-2"
              style="background: var(--accent); color: white;"
            >
              <Check class="w-4 h-4" />
              应用优化
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.optimizer-enter-active,
.optimizer-leave-active {
  transition: all 0.25s ease;
}

.optimizer-enter-from,
.optimizer-leave-to {
  opacity: 0;
}

.optimizer-enter-from .optimizer-panel,
.optimizer-leave-to .optimizer-panel {
  opacity: 0;
  transform: scale(0.96) translateY(8px);
}

.optimizer-panel {
  transition: all 0.25s ease;
}

/* Custom scrollbar */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: var(--border-color);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: var(--text-muted);
}
</style>
