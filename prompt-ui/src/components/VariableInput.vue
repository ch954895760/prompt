<script setup lang="ts">
import { computed, ref, watch, nextTick } from 'vue'
import { FileText, Code, Type } from 'lucide-vue-next'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'

interface Props {
  name: string
  modelValue?: string
  placeholder?: string
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

type InputType = 'single' | 'textarea' | 'code'

// 检测编程语言
const detectedLanguage = computed(() => {
  const lower = props.name.toLowerCase()
  const langMap: Record<string, string> = {
    'javascript': 'javascript',
    'js': 'javascript',
    'typescript': 'typescript',
    'ts': 'typescript',
    'python': 'python',
    'py': 'python',
    'java': 'java',
    'html': 'xml',
    'css': 'css',
    'scss': 'scss',
    'sass': 'scss',
    'less': 'less',
    'json': 'json',
    'sql': 'sql',
    'bash': 'bash',
    'shell': 'bash',
    'sh': 'bash',
    'go': 'go',
    'rust': 'rust',
    'rs': 'rust',
    'c': 'c',
    'cpp': 'cpp',
    'c++': 'cpp',
    'csharp': 'csharp',
    'cs': 'csharp',
    'php': 'php',
    'ruby': 'ruby',
    'rb': 'ruby',
    'swift': 'swift',
    'kotlin': 'kotlin',
    'kt': 'kotlin',
    'vue': 'xml',
    'react': 'javascript',
    'jsx': 'javascript',
    'tsx': 'typescript',
  }

  for (const [key, lang] of Object.entries(langMap)) {
    if (lower.includes(key)) return lang
  }
  return 'plaintext'
})

const inputType = computed<InputType>(() => {
  const lower = props.name.toLowerCase()
  if (lower.endsWith('_code') || lower.includes('code')) return 'code'
  if (lower.endsWith('_text') || lower.endsWith('_desc') ||
      lower.includes('description') || lower.includes('content') ||
      lower.includes('requirement') || lower.includes('detail')) return 'textarea'
  return 'single'
})

const typeIcon = computed(() => {
  switch (inputType.value) {
    case 'code': return Code
    case 'textarea': return FileText
    default: return Type
  }
})

const typeLabel = computed(() => {
  switch (inputType.value) {
    case 'code': return `代码 (${detectedLanguage.value})`
    case 'textarea': return '多行文本'
    default: return '文本'
  }
})

const inputClasses = computed(() => {
  const baseClasses = 'w-full px-3 py-2 rounded-lg text-sm transition-all focus:outline-none'
  const borderClasses = 'border border-[var(--border-color)] focus:border-[var(--accent)]'
  const bgClasses = 'bg-[var(--bg-secondary)] text-[var(--text-primary)]'

  switch (inputType.value) {
    case 'code':
      return `${baseClasses} ${borderClasses} ${bgClasses} font-mono min-h-[120px] max-h-[300px] resize-y leading-relaxed code-editor`
    case 'textarea':
      return `${baseClasses} ${borderClasses} ${bgClasses} min-h-[80px] max-h-[200px] resize-y leading-relaxed`
    default:
      return `${baseClasses} ${borderClasses} ${bgClasses} h-[40px]`
  }
})

// 代码高亮相关
const codeEditorRef = ref<HTMLTextAreaElement | null>(null)
const codeHighlightRef = ref<HTMLPreElement | null>(null)
const highlightedCode = ref('')

// 高亮代码
function highlightCode(code: string) {
  if (!code) return ''
  try {
    const lang = detectedLanguage.value
    if (lang === 'plaintext') {
      return escapeHtml(code)
    }
    const result = hljs.highlight(code, { language: lang })
    return result.value
  } catch (e) {
    return escapeHtml(code)
  }
}

function escapeHtml(text: string): string {
  const div = document.createElement('div')
  div.textContent = text
  return div.innerHTML
}

// 同步滚动
function syncScroll() {
  if (codeEditorRef.value && codeHighlightRef.value) {
    codeHighlightRef.value.scrollTop = codeEditorRef.value.scrollTop
    codeHighlightRef.value.scrollLeft = codeEditorRef.value.scrollLeft
  }
}

// 更新高亮
watch(() => props.modelValue, (newVal) => {
  highlightedCode.value = highlightCode(newVal || '')
}, { immediate: true })

function handleInput(e: Event) {
  const target = e.target as HTMLInputElement | HTMLTextAreaElement
  emit('update:modelValue', target.value)
}

// 处理 Tab 键
function handleKeydown(e: KeyboardEvent) {
  if (inputType.value === 'code' && e.key === 'Tab') {
    e.preventDefault()
    const target = e.target as HTMLTextAreaElement
    const start = target.selectionStart
    const end = target.selectionEnd
    const value = target.value
    const newValue = value.substring(0, start) + '  ' + value.substring(end)
    emit('update:modelValue', newValue)
    nextTick(() => {
      target.selectionStart = target.selectionEnd = start + 2
    })
  }
}
</script>

<template>
  <div class="variable-input-wrapper">
    <!-- 类型标签 -->
    <div class="flex items-center gap-1.5 mb-1.5">
      <component :is="typeIcon" class="w-3 h-3" style="color: var(--text-muted)" />
      <span class="text-[10px]" style="color: var(--text-muted)">{{ typeLabel }}</span>
    </div>

    <!-- 单行文本输入 -->
    <input
      v-if="inputType === 'single'"
      :value="modelValue"
      type="text"
      :class="inputClasses"
      :placeholder="placeholder"
      @input="handleInput"
    >

    <!-- 多行文本输入 -->
    <textarea
      v-else-if="inputType === 'textarea'"
      :value="modelValue"
      :class="inputClasses"
      :placeholder="placeholder"
      @input="handleInput"
    />

    <!-- 代码编辑器（带语法高亮） -->
    <div
      v-else-if="inputType === 'code'"
      class="code-editor-container"
    >
      <!-- 高亮层 -->
      <pre
        ref="codeHighlightRef"
        class="code-highlight-layer hljs"
        :class="`language-${detectedLanguage}`"
        v-html="highlightedCode + '<br>'"
      />
      <!-- 输入层 -->
      <textarea
        ref="codeEditorRef"
        :value="modelValue"
        :class="inputClasses"
        :placeholder="placeholder || '输入代码...'"
        @input="handleInput"
        @keydown="handleKeydown"
        @scroll="syncScroll"
      />
    </div>
  </div>
</template>

<style scoped>
.variable-input-wrapper {
  width: 100%;
}

/* 代码编辑器容器 */
.code-editor-container {
  position: relative;
  width: 100%;
  min-height: 120px;
  max-height: 300px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--border-color);
  background: var(--bg-secondary);
}

.code-editor-container:focus-within {
  border-color: var(--accent);
}

/* 高亮层 */
.code-highlight-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 12px;
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  tab-size: 2;
  white-space: pre;
  word-wrap: normal;
  overflow: auto;
  pointer-events: none;
  z-index: 1;
  background: transparent !important;
}

/* 输入层 */
.code-editor {
  position: relative;
  z-index: 2;
  width: 100%;
  min-height: 120px;
  max-height: 300px;
  padding: 12px;
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  tab-size: 2;
  white-space: pre;
  word-wrap: normal;
  background: transparent !important;
  border: none !important;
  color: transparent;
  caret-color: var(--text-primary);
  resize: vertical;
}

.code-editor::placeholder {
  color: var(--text-muted);
}

.code-editor:focus {
  outline: none;
  border: none !important;
}

/* 代码编辑器特殊样式 */
textarea[class*="font-mono"] {
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  tab-size: 2;
}

/* 滚动条样式 */
textarea::-webkit-scrollbar,
.code-highlight-layer::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

textarea::-webkit-scrollbar-track,
.code-highlight-layer::-webkit-scrollbar-track {
  background: transparent;
}

textarea::-webkit-scrollbar-thumb,
.code-highlight-layer::-webkit-scrollbar-thumb {
  background: var(--border-color);
  border-radius: 4px;
}

textarea::-webkit-scrollbar-thumb:hover,
.code-highlight-layer::-webkit-scrollbar-thumb:hover {
  background: var(--text-muted);
}

/* 代码高亮样式 - 适配主题 */
.code-highlight-layer.hljs {
  color: var(--text-primary);
  background: transparent !important;
}

.code-highlight-layer .hljs-keyword,
.code-highlight-layer .hljs-selector-tag,
.code-highlight-layer .hljs-subst {
  color: var(--accent);
  font-weight: bold;
}

.code-highlight-layer .hljs-string,
.code-highlight-layer .hljs-attr,
.code-highlight-layer .hljs-attribute {
  color: #22c55e;
}

.code-highlight-layer .hljs-number,
.code-highlight-layer .hljs-literal {
  color: #f97316;
}

.code-highlight-layer .hljs-comment {
  color: var(--text-muted);
  font-style: italic;
}

.code-highlight-layer .hljs-function,
.code-highlight-layer .hljs-title {
  color: #3b82f6;
}

.code-highlight-layer .hljs-params {
  color: var(--text-secondary);
}

.code-highlight-layer .hljs-tag {
  color: var(--accent);
}

.code-highlight-layer .hljs-name {
  color: #ef4444;
}

/* 深色模式下的代码高亮调整 */
.dark .code-highlight-layer .hljs-string,
.dark .code-highlight-layer .hljs-attr,
.dark .code-highlight-layer .hljs-attribute {
  color: #4ade80;
}

.dark .code-highlight-layer .hljs-number,
.dark .code-highlight-layer .hljs-literal {
  color: #fb923c;
}

.dark .code-highlight-layer .hljs-function,
.dark .code-highlight-layer .hljs-title {
  color: #60a5fa;
}

.dark .code-highlight-layer .hljs-name {
  color: #f87171;
}
</style>
