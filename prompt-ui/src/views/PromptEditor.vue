<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useToastStore } from '@/stores/toast'
import { useI18n } from 'vue-i18n'
import MainLayout from '@/components/MainLayout.vue'
import { createPrompt, updatePrompt, getPrompt } from '@/api/prompt'
import { getCategoryTree } from '@/api/category'
import { getTags, createTag } from '@/api/tag'
import type { Category, Tag, Prompt } from '@/types'
import { Save, Play, Copy, Trash2, X, History, RotateCcw, Square, Sparkles, Eye, Star } from 'lucide-vue-next'
import DeleteConfirmDialog from '@/components/DeleteConfirmDialog.vue'
import VariableInput from '@/components/VariableInput.vue'
import AiTestDialog from '@/components/AiTestDialog.vue'
import CategoryTreeSelect from '@/components/CategoryTreeSelect.vue'
import PromptOptimizer from '@/components/PromptOptimizer.vue'
import { getPromptHistory, rollbackPrompt, updatePromptScore } from '@/api/prompt'
import { aiTestStream } from '@/api/setting'
import { getAiProviders, getDefaultAiProvider } from '@/api/aiProvider'
import type { AiProvider } from '@/types'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'

marked.use({
  renderer: {
    code({ text, lang }) {
      const language = hljs.getLanguage(lang || '') ? lang : 'plaintext'
      const highlighted = hljs.highlight(text, { language: language || 'plaintext' }).value
      return `<pre style="margin: 0.75em 0; border-radius: 8px; overflow-x: auto; background: var(--bg-tertiary); border: 1px solid var(--border-color);"><code class="hljs language-${language}" style="font-family: 'JetBrains Mono', Menlo, monospace; font-size: 0.85em; line-height: 1.6; padding: 12px; display: block;">${highlighted}</code></pre>`
    },
    codespan({ text }) {
      return `<code style="font-family: 'JetBrains Mono', Menlo, monospace; font-size: 0.85em; background: var(--bg-tertiary); padding: 2px 6px; border-radius: 4px; color: var(--accent);">${text}</code>`
    }
  }
})

const route = useRoute()
const router = useRouter()
const toastStore = useToastStore()
const { t } = useI18n()
const isEdit = computed(() => !!route.params.id)
const promptId = computed(() => Number(route.params.id))

const title = ref('')
const content = ref('')
const description = ref('')
const categoryId = ref<number | null>(null)
const selectedTagIds = ref<number[]>([])
const newTagInput = ref('')
const tagInputFocused = ref(false)
const showTagDropdown = ref(false)
const tagDropdownRef = ref<HTMLElement | null>(null)
const tagHighlightIndex = ref(-1) // 当前高亮的标签索引

const categories = ref<Category[]>([])
const categoryTree = ref<Category[]>([])
const tags = ref<Tag[]>([])
const showCategoryDropdown = ref(false)
const categoryDropdownRef = ref<HTMLElement | null>(null)
const variableValues = ref<Record<string, string>>({})
const aiResult = ref('')
const showAiResult = ref(false)
const aiLoading = ref(false)
const loading = ref(false)
const aiAbort = ref<(() => void) | null>(null)
const aiContentRef = ref<HTMLDivElement | null>(null)
const aiProviders = ref<AiProvider[]>([])
const selectedAiProvider = ref<number | null>(null)
const showAiTestDialog = ref(false)
const showOptimizer = ref(false)
const currentPromptScore = ref<number | undefined>(undefined)

function scrollAiToBottom() {
  nextTick(() => {
    if (aiContentRef.value) {
      aiContentRef.value.scrollTop = aiContentRef.value.scrollHeight
    }
  })
}

const renderedAiResult = computed(() => {
  if (!aiResult.value) return ''
  return marked(aiResult.value) as string
})

const processedPrompt = computed(() => {
  return content.value.replace(/\{\{(\w+)\}\}/g, (match: string, varName: string) => {
    return variableValues.value[varName] || match
  })
})

const extractedVars = computed(() => {
  const vars = [...content.value.matchAll(/\{\{(\w+)\}\}/g)].map(m => m[1])
  return [...new Set(vars.filter((v): v is string => !!v))]
})

// 检测变量对应的语言
function getVariableLanguage(varName: string): string {
  const lower = varName.toLowerCase()
  const langMap: Record<string, string> = {
    'javascript': 'javascript',
    'typescript': 'typescript',
    'python': 'python',
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
    'go': 'go',
    'rust': 'rust',
    'php': 'php',
    'ruby': 'ruby',
    'swift': 'swift',
    'kotlin': 'kotlin',
    'vue': 'xml',
    'react': 'javascript',
  }

  // 严格匹配：变量名等于语言名，或以语言名+下划线开头，或以_+语言名结尾
  for (const [key, lang] of Object.entries(langMap)) {
    // 完全匹配
    if (lower === key) return lang
    // 以语言名开头，后面跟着下划线
    if (lower.startsWith(key + '_')) return lang
    // 以_语言名结尾
    if (lower.endsWith('_' + key)) return lang
    // 包含_语言名_
    if (lower.includes('_' + key + '_')) return lang
  }

  // 短代码别名（需要更严格的匹配）
  const shortLangMap: Record<string, string> = {
    '_js': 'javascript',
    'js_': 'javascript',
    '_ts': 'typescript',
    'ts_': 'typescript',
    '_py': 'python',
    'py_': 'python',
    '_sh': 'bash',
    'sh_': 'bash',
    '_rs': 'rust',
    'rs_': 'rust',
    '_cs': 'csharp',
    'cs_': 'csharp',
    '_rb': 'ruby',
    'rb_': 'ruby',
    '_kt': 'kotlin',
    'kt_': 'kotlin',
    '_jsx': 'javascript',
    'jsx_': 'javascript',
    '_tsx': 'typescript',
    'tsx_': 'typescript',
  }

  for (const [key, lang] of Object.entries(shortLangMap)) {
    if (lower.includes(key)) return lang
  }

  return ''
}

// HTML 转义函数
function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
}

const previewContent = computed(() => {
  let preview = content.value
  for (const [key, value] of Object.entries(variableValues.value)) {
    const regex = new RegExp(`\\{\\{${key}\\}\\}`, 'g')

    let formattedValue: string
    if (!value) {
      formattedValue = `{{${key}}}`
    } else if (key.toLowerCase().includes('code') || getVariableLanguage(key)) {
      // 代码变量：使用代码块高亮
      const lang = getVariableLanguage(key) || 'plaintext'
      try {
        const highlighted = hljs.highlight(value, { language: lang }).value
        formattedValue = `<pre style="margin: 0.5em 0; border-radius: 8px; overflow-x: auto; background: var(--bg-tertiary); border: 1px solid var(--border-color);"><code class="hljs language-${lang}" style="font-family: 'JetBrains Mono', Menlo, monospace; font-size: 0.85em; line-height: 1.6; padding: 12px; display: block;">${highlighted}</code></pre>`
      } catch (e) {
        formattedValue = `<pre style="margin: 0.5em 0; border-radius: 8px; overflow-x: auto; background: var(--bg-tertiary); border: 1px solid var(--border-color); padding: 12px;"><code style="font-family: 'JetBrains Mono', Menlo, monospace; font-size: 0.85em; line-height: 1.6; color: var(--text-primary);">${escapeHtml(value)}</code></pre>`
      }
    } else {
      // 文本类型变量：直接追加替换，不做特殊处理，但需要转义 HTML
      formattedValue = escapeHtml(value)
    }
    preview = preview.replace(regex, formattedValue)
  }
  // Highlight remaining vars
  preview = preview.replace(/\{\{(\w+)\}\}/g, '<span style="color: var(--accent); font-family: monospace; font-size: 0.85em; background: var(--accent-soft); padding: 1px 4px; border-radius: 4px;">{{$1}}</span>')
  return preview
})

watch(extractedVars, (vars) => {
  const newValues: Record<string, string> = {}
  for (const v of vars) {
    newValues[v] = variableValues.value[v] || ''
  }
  variableValues.value = newValues
})

async function loadData() {
  const [cTree, tList, providers] = await Promise.all([
    getCategoryTree(),
    getTags(),
    getAiProviders(),
  ])
  categoryTree.value = cTree
  // 扁平化分类列表用于显示
  categories.value = flattenCategories(cTree)
  tags.value = tList
  aiProviders.value = providers
  // Set default provider
  const defaultProvider = providers.find(p => p.isDefault)
  if (defaultProvider) {
    selectedAiProvider.value = defaultProvider.id
  } else if (providers.length > 0) {
    selectedAiProvider.value = providers[0]!.id
  }
}

// 将树形分类扁平化为列表
function flattenCategories(cats: Category[]): Category[] {
  const result: Category[] = []
  for (const cat of cats) {
    result.push(cat)
    if (cat.children && cat.children.length > 0) {
      result.push(...flattenCategories(cat.children))
    }
  }
  return result
}

// 获取分类的完整路径名称
function getCategoryPathName(catId: number | null): string {
  if (!catId) return t('prompt.uncategorized')
  const cat = categories.value.find(c => c.id === catId)
  if (!cat) return t('prompt.uncategorized')

  const path: string[] = [cat.name]
  let current = cat
  while (current.parentId) {
    const parent = categories.value.find(c => c.id === current.parentId)
    if (parent) {
      path.unshift(parent.name)
      current = parent
    } else {
      break
    }
  }
  return path.join(' / ')
}

// 选择分类
function selectCategory(id: number | null) {
  categoryId.value = id
  showCategoryDropdown.value = false
}

// 点击外部关闭下拉框
function handleClickOutside(event: MouseEvent) {
  if (categoryDropdownRef.value && !categoryDropdownRef.value.contains(event.target as Node)) {
    showCategoryDropdown.value = false
  }
}

async function loadPrompt() {
  if (!isEdit.value) return
  try {
    const p = await getPrompt(promptId.value)
    title.value = p.title
    content.value = p.content
    description.value = p.description || ''
    categoryId.value = p.categoryId || null
    selectedTagIds.value = p.tags?.map(t => t.id) || []
    currentPromptScore.value = p.qualityScore
    if (p.variablesJson) {
      variableValues.value = typeof p.variablesJson === 'string' ? JSON.parse(p.variablesJson) : p.variablesJson
    }
  } catch (e) {
    console.error('[DEBUG] Failed to load prompt:', e)
  }
}

async function handleSave() {
  if (!title.value.trim()) {
    toastStore.warning(t('prompt.titleRequired'))
    return
  }
  if (!content.value.trim()) {
    toastStore.warning(t('prompt.contentRequired'))
    return
  }
  loading.value = true
  try {
    const data: Partial<Prompt> = {
      title: title.value,
      content: content.value,
      description: description.value,
      categoryId: categoryId.value || undefined,
      tagIds: selectedTagIds.value,
      variablesJson: extractedVars.value.length > 0 ? JSON.stringify(variableValues.value) : undefined,
    }
    if (isEdit.value) {
      await updatePrompt(promptId.value, data)
      toastStore.success(t('prompt.updateSuccess'))
    } else {
      await createPrompt(data)
      toastStore.success(t('prompt.createSuccess'))
    }
    setTimeout(() => router.push('/prompts'), 800)
  } catch (e: any) {
    toastStore.error(e.message || t('prompt.saveFailed'))
  } finally {
    loading.value = false
  }
}

function handleCopy() {
  const text = content.value.replace(/\{\{(\w+)\}\}/g, (match, varName) => {
    return variableValues.value[varName] || match
  })

  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(text).then(() => {
      toastStore.success(t('prompt.copySuccess'))
    }).catch(() => {
      toastStore.error(t('common.copyFailed'))
    })
  } else {
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.left = '-9999px'
    document.body.appendChild(textarea)
    textarea.select()
    const success = document.execCommand('copy')
    document.body.removeChild(textarea)
    if (success) {
      toastStore.success(t('prompt.copySuccess'))
    } else {
      toastStore.error(t('common.copyFailed'))
    }
  }
}

function handleTest() {
  showAiTestDialog.value = true
}

function handleStopTest() {
  if (aiAbort.value) {
    aiAbort.value()
    aiAbort.value = null
  }
  aiLoading.value = false
}

async function handleSaveScore(score: number) {
  if (!isEdit.value) {
    toastStore.warning(t('prompt.savePromptFirst'))
    return
  }
  try {
    await updatePromptScore(promptId.value, score)
    currentPromptScore.value = score
    toastStore.success(t('prompt.scoreSaved'))
  } catch (e: any) {
    toastStore.error(e.message || t('prompt.scoreSaveFailed'))
  }
}

function getScoreColor(score: number) {
  if (score >= 8) return '#22c55e'
  if (score >= 5) return '#f59e0b'
  return '#ef4444'
}

function getScoreBgColor(score: number) {
  if (score >= 8) return 'rgba(34, 197, 94, 0.1)'
  if (score >= 5) return 'rgba(245, 158, 11, 0.1)'
  return 'rgba(239, 68, 68, 0.1)'
}

// 过滤后的标签列表（搜索功能）
const filteredTags = computed(() => {
  const input = newTagInput.value.trim().toLowerCase()
  if (!input) return tags.value.filter(t => !selectedTagIds.value.includes(t.id))
  return tags.value.filter(t =>
    !selectedTagIds.value.includes(t.id) &&
    t.name.toLowerCase().includes(input)
  )
})

// 是否有完全匹配的标签
const hasExactMatch = computed(() => {
  const input = newTagInput.value.trim().toLowerCase()
  if (!input) return false
  return tags.value.some(t => t.name.toLowerCase() === input)
})

// 下拉框总选项数（标签 + 可能的"新建"选项）
const totalTagOptions = computed(() => {
  let count = filteredTags.value.length
  if (newTagInput.value.trim() && !hasExactMatch.value) {
    count += 1 // 新建选项
  }
  return count
})

function handleAddTag() {
  const name = newTagInput.value.trim()
  if (!name) return
  const existing = tags.value.find(t => t.name === name)
  if (existing) {
    if (!selectedTagIds.value.includes(existing.id)) {
      selectedTagIds.value.push(existing.id)
    }
  } else {
    createTag({ name }).then((tag) => {
      tags.value.push(tag)
      selectedTagIds.value.push(tag.id)
    })
  }
  newTagInput.value = ''
  tagHighlightIndex.value = -1
  // 保持下拉框打开，方便继续选择
  showTagDropdown.value = true
}

function selectTag(tag: Tag) {
  if (!selectedTagIds.value.includes(tag.id)) {
    selectedTagIds.value.push(tag.id)
  }
  newTagInput.value = ''
  tagHighlightIndex.value = -1
  // 保持下拉框打开，方便继续选择
  showTagDropdown.value = true
}

function handleTagInputFocus() {
  tagInputFocused.value = true
  showTagDropdown.value = true
  tagHighlightIndex.value = -1
}

function handleTagInputBlur() {
  tagInputFocused.value = false
  // 延迟关闭下拉框，以便点击下拉项
  setTimeout(() => {
    showTagDropdown.value = false
    tagHighlightIndex.value = -1
  }, 200)
}

// 处理标签输入框的键盘事件
function handleTagKeydown(event: KeyboardEvent) {
  if (!showTagDropdown.value) return

  const totalOptions = totalTagOptions.value
  if (totalOptions === 0) return

  switch (event.key) {
    case 'ArrowDown':
      event.preventDefault()
      tagHighlightIndex.value = (tagHighlightIndex.value + 1) % totalOptions
      break
    case 'ArrowUp':
      event.preventDefault()
      tagHighlightIndex.value = (tagHighlightIndex.value - 1 + totalOptions) % totalOptions
      break
    case 'Enter':
      event.preventDefault()
      if (tagHighlightIndex.value >= 0 && tagHighlightIndex.value < filteredTags.value.length) {
        // 选择高亮的标签
        const selectedTag = filteredTags.value[tagHighlightIndex.value]
        if (selectedTag) {
          selectTag(selectedTag)
        }
      } else if (tagHighlightIndex.value >= filteredTags.value.length && newTagInput.value.trim() && !hasExactMatch.value) {
        // 新建标签
        handleAddTag()
      } else if (tagHighlightIndex.value === -1 && newTagInput.value.trim()) {
        // 没有高亮项时，回车默认创建新标签或选择第一个匹配项
        handleAddTag()
      }
      break
    case 'Escape':
      showTagDropdown.value = false
      tagHighlightIndex.value = -1
      break
    case 'Backspace':
      // 当输入框为空时，删除最后一个已选择的标签
      if (!newTagInput.value && selectedTagIds.value.length > 0) {
        event.preventDefault()
        const lastTagId = selectedTagIds.value[selectedTagIds.value.length - 1]!
        removeTag(lastTagId)
      }
      break
  }
}

function removeTag(tagId: number) {
  selectedTagIds.value = selectedTagIds.value.filter(id => id !== tagId)
}

function clearEditor() {
  title.value = ''
  content.value = ''
  description.value = ''
  categoryId.value = null
  selectedTagIds.value = []
  variableValues.value = {}
  showAiResult.value = false
  aiResult.value = ''
}

const showHistory = ref(false)
const historyList = ref<{ id: number; promptId: number; content: string; version: number; createdAt: string }[]>([])
const historyLoading = ref(false)

async function loadHistory() {
  if (!isEdit.value) return
  historyLoading.value = true
  try {
    historyList.value = await getPromptHistory(promptId.value)
  } catch (e) {
    console.error('[DEBUG] Failed to load history:', e)
  } finally {
    historyLoading.value = false
  }
}

const rollbackDialogVisible = ref(false)
const rollbackVersion = ref<number | null>(null)

function handleRollback(version: number) {
  rollbackVersion.value = version
  rollbackDialogVisible.value = true
}

// 查看历史版本内容
const viewHistoryDialogVisible = ref(false)
const viewingHistory = ref<{ id: number; promptId: number; content: string; version: number; createdAt: string } | null>(null)

function handleViewHistory(history: { id: number; promptId: number; content: string; version: number; createdAt: string }) {
  viewingHistory.value = history
  viewHistoryDialogVisible.value = true
}

async function confirmRollback() {
  if (!rollbackVersion.value) return
  try {
    await rollbackPrompt(promptId.value, rollbackVersion.value)
    toastStore.success(t('prompt.rollbackSuccess'))
    await loadPrompt()
    await loadHistory()
  } catch (e: any) {
    toastStore.error(e.message || t('prompt.rollbackFailed'))
  } finally {
    rollbackVersion.value = null
  }
}

onMounted(() => {
  loadData()
  loadPrompt()
  loadHistory()
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <MainLayout>
    <div class="animate-fade-in">
      <div class="flex items-center justify-between mb-6">
        <div class="flex items-center gap-4">
          <div>
            <h2 class="text-2xl font-bold mb-1" style="color: var(--text-primary)">
              {{ isEdit ? t('prompt.editPrompt') : t('prompt.createPrompt') }}
            </h2>
            <p class="text-sm" style="color: var(--text-secondary)">
              {{ isEdit ? t('prompt.updatePrompt') : t('prompt.createPromptDesc') }}
            </p>
          </div>
          <!-- 评分展示 -->
          <div
            v-if="currentPromptScore !== undefined && currentPromptScore !== null"
            class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium"
            :style="{ background: getScoreBgColor(currentPromptScore), color: getScoreColor(currentPromptScore) }"
          >
            <Star class="w-4 h-4" />
            <span>{{ currentPromptScore }}/10</span>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <button v-if="isEdit" @click="showHistory = !showHistory"
            class="flex items-center gap-2 px-4 py-2.5 text-sm font-medium rounded-xl transition-all border"
            :class="showHistory ? 'bg-[#ea580c]/10 border-[#ea580c]/30 text-[#ea580c]' : 'border-color: var(--border-color); color: var(--text-secondary);'"
            style="border-color: var(--border-color);"
          >
            <History class="w-4 h-4" />
            {{ t('prompt.historyVersion') }}
          </button>
          <button @click="handleSave" :disabled="loading"
            class="flex items-center gap-2 px-4 py-2.5 bg-[#ea580c] hover:bg-[#c2410c] text-white text-sm font-medium rounded-xl transition-all shadow-lg shadow-[#ea580c]/20 disabled:opacity-50"
          >
            <Save class="w-4 h-4" />
            {{ loading ? t('common.saving') : t('common.save') }}
          </button>
        </div>
      </div>

      <div class="grid grid-cols-1 xl:grid-cols-2 gap-6">
        <!-- Editor panel -->
        <div class="space-y-5">
          <div>
            <label class="block text-xs font-medium mb-2" style="color: var(--text-secondary)">{{ t('common.title') }}</label>
            <input v-model="title" type="text"
              class="w-full px-4 py-3 rounded-xl text-base font-medium transition-all"
              style="background: var(--bg-secondary); border: 1px solid var(--border-color); color: var(--text-primary);"
              :placeholder="t('prompt.titlePlaceholder')"
              @focus="($event.target as HTMLElement).style.borderColor = 'var(--accent)'"
              @blur="($event.target as HTMLElement).style.borderColor = 'var(--border-color)'"
            >
          </div>

          <div class="flex gap-4">
            <div class="flex-1 relative" ref="categoryDropdownRef">
              <label class="block text-xs font-medium mb-2" style="color: var(--text-secondary)">{{ t('common.category') }}</label>
              <button
                @click.stop="showCategoryDropdown = !showCategoryDropdown"
                class="w-full px-4 py-2.5 rounded-xl text-sm transition-all text-left flex items-center justify-between"
                style="background: var(--bg-secondary); border: 1px solid var(--border-color); color: var(--text-primary);"
                :style="showCategoryDropdown ? 'border-color: var(--accent)' : ''"
              >
                <span class="truncate">{{ getCategoryPathName(categoryId) }}</span>
                <svg class="w-4 h-4 transition-transform flex-shrink-0" :class="showCategoryDropdown ? 'rotate-180' : ''" style="color: var(--text-muted)" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                </svg>
              </button>

              <!-- 树形分类下拉框 -->
              <div v-if="showCategoryDropdown"
                class="absolute z-50 w-full mt-1 rounded-xl overflow-hidden shadow-lg"
                style="background: var(--bg-secondary); border: 1px solid var(--border-color); max-height: 320px; overflow-y: auto;"
              >
                <!-- 未分类选项 -->
                <div
                  @click="selectCategory(null)"
                  class="px-4 py-2.5 cursor-pointer transition-colors flex items-center gap-2"
                  :style="categoryId === null ? 'background: var(--accent-soft); color: var(--accent);' : 'color: var(--text-primary);'"
                  @mouseenter="($event.currentTarget as HTMLElement).style.background = categoryId === null ? 'var(--accent-soft)' : 'var(--bg-tertiary)'"
                  @mouseleave="($event.currentTarget as HTMLElement).style.background = categoryId === null ? 'var(--accent-soft)' : 'transparent'"
                >
                  <span class="text-sm">未分类</span>
                </div>

                <!-- 树形分类列表 -->
                <CategoryTreeSelect
                  v-for="cat in categoryTree" :key="cat.id"
                  :category="cat"
                  :selected-id="categoryId"
                  @select="selectCategory"
                />
              </div>
            </div>
            <div class="flex-1 relative" ref="tagDropdownRef">
              <label class="block text-xs font-medium mb-2" style="color: var(--text-secondary)">{{ t('common.tag') }}</label>
              <div class="flex flex-wrap items-center gap-1 px-2 py-1.5 rounded-lg text-sm transition-all"
                :style="{ borderColor: tagInputFocused ? 'var(--accent)' : 'var(--border-color)', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)' }"
              >
                <span v-for="tagId in selectedTagIds" :key="tagId"
                  class="inline-flex items-center gap-1 text-xs px-2 py-0.5 rounded-md shrink-0"
                  style="background: var(--accent-soft); color: var(--accent);"
                >
                  {{ tags.find(t => t.id === tagId)?.name || tagId }}
                  <button @click="removeTag(tagId)" class="hover:opacity-70">
                    <X class="w-3 h-3" />
                  </button>
                </span>
                <input v-model="newTagInput" type="text"
                  class="flex-1 min-w-[80px] px-2 py-1 bg-transparent text-sm outline-none"
                  style="color: var(--text-primary);"
                  :placeholder="t('prompt.tagPlaceholder')"
                  @keydown="handleTagKeydown"
                  @focus="handleTagInputFocus"
                  @blur="handleTagInputBlur"
                >
              </div>
              <!-- 标签搜索下拉框 -->
              <div v-if="showTagDropdown && (filteredTags.length > 0 || (newTagInput.trim() && !hasExactMatch))"
                class="absolute z-50 w-full mt-1 rounded-xl overflow-hidden shadow-lg"
                style="background: var(--bg-secondary); border: 1px solid var(--border-color); max-height: 200px; overflow-y: auto;"
              >
                <!-- 已有标签列表 -->
                <div v-for="(tag, index) in filteredTags" :key="tag.id"
                  @click="selectTag(tag)"
                  class="px-4 py-2 cursor-pointer transition-colors text-sm"
                  :style="{
                    color: tagHighlightIndex === index ? 'var(--accent)' : 'var(--text-primary)',
                    background: tagHighlightIndex === index ? 'var(--accent-soft)' : 'transparent'
                  }"
                  @mouseenter="tagHighlightIndex = index"
                >
                  {{ tag.name }}
                </div>
                <!-- 新建标签选项 -->
                <div v-if="newTagInput.trim() && !hasExactMatch"
                  @click="handleAddTag"
                  class="px-4 py-2 cursor-pointer transition-colors text-sm flex items-center gap-2"
                  :style="{
                    color: tagHighlightIndex === filteredTags.length ? 'var(--accent)' : 'var(--accent)',
                    background: tagHighlightIndex === filteredTags.length ? 'var(--accent-soft)' : 'transparent',
                    borderTop: '1px solid var(--border-color)'
                  }"
                  @mouseenter="tagHighlightIndex = filteredTags.length"
                >
                  <span class="text-xs px-1.5 py-0.5 rounded" style="background: var(--accent); color: white;">新建</span>
                  <span>"{{ newTagInput.trim() }}"</span>
                </div>
              </div>
            </div>
          </div>

          <div>
            <div class="flex items-center justify-between mb-2">
              <label class="block text-xs font-medium" style="color: var(--text-secondary)">{{ t('prompt.promptContent') }}</label>
              <div class="flex items-center gap-2">
                <button @click="showOptimizer = true"
                  class="flex items-center gap-1.5 px-2.5 py-1 text-[10px] font-medium rounded-md transition-all hover:opacity-80"
                  style="background: linear-gradient(135deg, var(--accent-soft) 0%, rgba(234, 88, 12, 0.15) 100%); color: var(--accent); border: 1px solid var(--accent);"
                >
                  <Sparkles class="w-3 h-3" />
                  {{ t('prompt.aiOptimize') }}
                </button>
                <span class="text-[10px] px-2 py-1 rounded-md" style="background: var(--bg-tertiary); color: var(--text-muted)">
                  {{ t('prompt.variableTip') }}
                </span>
              </div>
            </div>
            <textarea v-model="content"
              class="w-full px-4 py-4 rounded-xl transition-all"
              style="background: var(--bg-secondary); border: 1px solid var(--border-color); color: var(--text-primary); font-size: 16px; line-height: 1.7; resize: vertical; min-height: 300px;"
              :placeholder="t('prompt.contentPlaceholder')"
              @focus="($event.target as HTMLElement).style.borderColor = 'var(--accent)'"
              @blur="($event.target as HTMLElement).style.borderColor = 'var(--border-color)'"
            >
            </textarea>
          </div>

          <!-- Variable inputs -->
          <div v-if="extractedVars.length > 0">
            <label class="block text-xs font-medium mb-3" style="color: var(--text-secondary)">{{ t('prompt.fillVariables') }}</label>
            <div class="grid grid-cols-1 gap-3">
              <div v-for="v in extractedVars" :key="v">
                <label class="block text-[10px] font-medium uppercase tracking-wider mb-1.5" style="color: var(--text-muted)">{{ v }}</label>
                <VariableInput
                  :name="v"
                  v-model="variableValues[v]"
                  :placeholder="t('prompt.variableInputPlaceholder', { name: v })"
                />
              </div>
            </div>
          </div>

          <div class="flex items-center gap-3 pt-2 flex-wrap">
            <select v-model="selectedAiProvider"
              class="px-3 py-2.5 rounded-xl text-sm transition-all"
              style="background: var(--bg-secondary); border: 1px solid var(--border-color); color: var(--text-primary);"
            >
              <option v-for="provider in aiProviders" :key="provider.id" :value="provider.id">
                {{ provider.name }}
              </option>
            </select>
            <button v-if="!aiLoading" @click="handleTest"
              class="flex items-center gap-1.5 px-4 py-2 border-2 text-sm font-medium rounded-xl transition-all active:scale-[0.98]"
              style="border-color: var(--accent); color: var(--accent);"
            >
              <Play class="w-4 h-4" />
              {{ t('prompt.testRun') }}
            </button>
            <button v-else @click="handleStopTest"
              class="flex items-center gap-1.5 px-4 py-2 border-2 text-sm font-medium rounded-xl transition-all active:scale-[0.98]"
              style="border-color: #dc2626; color: #dc2626;"
            >
              <Square class="w-4 h-4" />
              {{ t('prompt.stopGenerating') }}
            </button>
            <button @click="clearEditor"
              class="px-4 py-2.5 text-sm font-medium rounded-xl transition-colors hover:bg-[var(--bg-tertiary)]"
              style="color: var(--text-secondary);"
            >
              {{ t('common.clear') }}
            </button>
          </div>

          <!-- History panel -->
          <div v-if="showHistory && isEdit" class="rounded-2xl p-5 mt-4" style="background: var(--bg-secondary); border: 1px solid var(--border-color);">
            <h3 class="text-sm font-semibold mb-3 flex items-center gap-2" style="color: var(--text-primary)">
              <History class="w-4 h-4" style="color: var(--accent)" />
              {{ t('prompt.versionHistory') }}
            </h3>
            <div v-if="historyLoading" class="text-xs py-2" style="color: var(--text-muted)">{{ t('common.loading') }}</div>
            <div v-else-if="historyList.length === 0" class="text-xs py-2" style="color: var(--text-muted)">{{ t('prompt.noHistory') }}</div>
            <div v-else class="space-y-2 max-h-[300px] overflow-y-auto">
              <div v-for="h in historyList" :key="h.id"
                class="flex items-center justify-between px-3 py-2.5 rounded-xl text-xs"
                style="background: var(--bg-primary); border: 1px solid var(--border-color);"
              >
                <div class="flex items-center gap-3">
                  <span class="font-mono font-semibold px-2 py-0.5 rounded-md" style="background: var(--accent-soft); color: var(--accent);">v{{ h.version }}</span>
                  <span style="color: var(--text-secondary)">{{ new Date(h.createdAt).toLocaleString() }}</span>
                </div>
                <div class="flex items-center gap-1">
                  <button @click="handleViewHistory(h)"
                    class="flex items-center gap-1 px-2 py-1 rounded-lg transition-colors hover:bg-[var(--bg-tertiary)]"
                    style="color: var(--text-secondary);"
                    :title="t('prompt.viewContent')"
                  >
                    <Eye class="w-3 h-3" />
                    {{ t('common.view') }}
                  </button>
                  <button @click="handleRollback(h.version)"
                    class="flex items-center gap-1 px-2 py-1 rounded-lg transition-colors hover:bg-[var(--bg-tertiary)]"
                    style="color: var(--accent);"
                    :title="t('prompt.rollbackTitle')"
                  >
                    <RotateCcw class="w-3 h-3" />
                    {{ t('prompt.rollback') }}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Preview + AI panel -->
        <div class="flex flex-col gap-4 self-start" style="position: sticky; top: 24px;">
          <!-- Preview -->
          <div>
            <label class="block text-xs font-medium mb-2" style="color: var(--text-secondary)">{{ t('prompt.livePreview') }}</label>
            <div class="rounded-2xl p-6 min-h-[400px] relative transition-all"
              style="background: var(--bg-secondary); border: 1px solid var(--border-color); line-height: 1.8;"
            >
              <div class="absolute top-4 right-4">
                <button @click="handleCopy"
                  class="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-lg transition-all hover:bg-[var(--bg-tertiary)]"
                  style="color: var(--text-muted); border: 1px solid var(--border-color);"
                >
                  <Copy class="w-3.5 h-3.5" />
                  {{ t('common.copy') }}
                </button>
              </div>
              <div class="pt-8 text-base preview-content" style="white-space: pre-wrap; color: var(--text-primary);" v-html="previewContent || `<span style='color: var(--text-muted); font-style: italic;'>${t('prompt.previewPlaceholder')}</span>`">
              </div>
            </div>
          </div>

          <!-- AI Test Result -->
          <div v-if="showAiResult" class="rounded-2xl flex flex-col transition-all"
            style="background: var(--bg-secondary); border: 1px solid var(--border-color); max-height: 420px;"
          >
            <!-- Header -->
            <div class="flex items-center justify-between px-5 py-3 shrink-0"
              style="border-bottom: 1px solid var(--border-color);"
            >
              <div class="flex items-center gap-2">
                <div class="w-6 h-6 rounded-full flex items-center justify-center" style="background: var(--accent-soft);">
                  <svg class="w-3.5 h-3.5" style="color: var(--accent);" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M12 2L2 7l10 5 10-5-10-5z" />
                    <path d="M2 17l10 5 10-5" />
                    <path d="M2 12l10 5 10-5" />
                  </svg>
                </div>
                <span class="text-xs font-medium" style="color: var(--text-primary)">{{ t('prompt.aiResponse') }}</span>
                <span v-if="aiLoading" class="text-[10px] px-2 py-0.5 rounded-full animate-pulse" style="background: var(--accent-soft); color: var(--accent);">{{ t('prompt.generating') }}</span>
              </div>
              <button v-if="aiLoading" @click="handleStopTest"
                class="flex items-center gap-1 px-2 py-1 text-[10px] font-medium rounded-md transition-all"
                style="background: rgba(220, 38, 38, 0.08); color: #dc2626; border: 1px solid rgba(220, 38, 38, 0.25);"
                :title="t('prompt.stopGenerating')"
              >
                <Square class="w-3 h-3" />
                {{ t('common.stop') }}
              </button>
            </div>

            <!-- Content -->
            <div ref="aiContentRef" class="p-5 overflow-y-auto"
              style="min-height: 120px; max-height: 360px;"
            >
              <div v-if="!aiResult && aiLoading" class="flex items-center gap-2 text-sm" style="color: var(--text-muted);">
                <div class="w-4 h-4 border-2 border-[#ea580c] border-t-transparent rounded-full animate-spin"></div>
                {{ t('prompt.waitingForAi') }}
              </div>
              <div v-else-if="!aiResult && !aiLoading" class="text-sm" style="color: var(--text-muted);">{{ t('prompt.clickToTest') }}</div>
              <div v-else class="text-sm leading-relaxed ai-markdown ai-message-bubble" style="color: var(--text-primary);" v-html="renderedAiResult"></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Rollback Confirm Dialog -->
    <DeleteConfirmDialog
      v-model="rollbackDialogVisible"
      :title="t('prompt.confirmRollback')"
      :description="t('prompt.rollbackDescription', { version: rollbackVersion })"
      :confirm-text="t('prompt.rollback')"
      @confirm="confirmRollback"
    />

    <!-- View History Content Dialog -->
    <div v-if="viewHistoryDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4" style="background: rgba(0, 0, 0, 0.5);" @click.self="viewHistoryDialogVisible = false">
      <div class="rounded-2xl w-full max-w-3xl max-h-[80vh] flex flex-col" style="background: var(--bg-secondary); border: 1px solid var(--border-color); box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);">
        <!-- Header -->
        <div class="flex items-center justify-between px-6 py-4" style="border-bottom: 1px solid var(--border-color);">
          <div class="flex items-center gap-3">
            <History class="w-5 h-5" style="color: var(--accent)" />
            <h3 class="text-base font-semibold" style="color: var(--text-primary)">
              {{ t('prompt.versionContent', { version: viewingHistory?.version }) }}
            </h3>
            <span class="text-xs px-2 py-0.5 rounded-md" style="background: var(--bg-tertiary); color: var(--text-muted);">
              {{ viewingHistory ? new Date(viewingHistory.createdAt).toLocaleString() : '' }}
            </span>
          </div>
          <button @click="viewHistoryDialogVisible = false" class="p-1.5 rounded-lg transition-colors hover:bg-[var(--bg-tertiary)]" style="color: var(--text-muted);">
            <X class="w-5 h-5" />
          </button>
        </div>
        <!-- Content -->
        <div class="p-6 overflow-y-auto flex-1">
          <div class="rounded-xl p-4 text-sm" style="background: var(--bg-primary); border: 1px solid var(--border-color); white-space: pre-wrap; color: var(--text-primary); line-height: 1.7; min-height: 200px;">
            {{ viewingHistory?.content }}
          </div>
        </div>
        <!-- Footer -->
        <div class="flex items-center justify-end gap-3 px-6 py-4" style="border-top: 1px solid var(--border-color);">
          <button @click="viewHistoryDialogVisible = false" class="px-4 py-2 text-sm font-medium rounded-xl transition-colors" style="background: var(--bg-tertiary); color: var(--text-secondary);">
            {{ t('common.close') }}
          </button>
          <button v-if="viewingHistory" @click="() => { viewHistoryDialogVisible = false; handleRollback(viewingHistory!.version); }" class="px-4 py-2 text-sm font-medium rounded-xl transition-colors" style="background: var(--accent); color: white;">
            {{ t('prompt.rollbackToVersion') }}
          </button>
        </div>
      </div>
    </div>

    <!-- AI Test Dialog -->
    <AiTestDialog
      v-model="showAiTestDialog"
      :initial-prompt="processedPrompt"
      :provider-id="selectedAiProvider"
    />

    <!-- Prompt Optimizer -->
    <PromptOptimizer
      v-model="showOptimizer"
      :current-prompt="content"
      :provider-id="selectedAiProvider"
      :prompt-id="promptId"
      @apply="(optimized) => content = optimized"
      @save-score="handleSaveScore"
    />

  </MainLayout>
</template>

<style>
.ai-markdown h1 {
  font-size: 1.35em;
  font-weight: 700;
  margin: 0.75em 0 0.4em;
  line-height: 1.35;
  color: var(--text-primary);
}
.ai-markdown h2 {
  font-size: 1.15em;
  font-weight: 600;
  margin: 0.75em 0 0.4em;
  line-height: 1.35;
  color: var(--text-primary);
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 0.3em;
}
.ai-markdown h3 {
  font-size: 1.05em;
  font-weight: 600;
  margin: 0.6em 0 0.3em;
  color: var(--text-primary);
}
.ai-markdown h4 {
  font-size: 1em;
  font-weight: 600;
  margin: 0.5em 0 0.25em;
  color: var(--text-primary);
}
.ai-markdown p {
  margin: 0.5em 0;
  line-height: 1.75;
}
.ai-markdown ul, .ai-markdown ol {
  margin: 0.5em 0;
  padding-left: 1.5em;
}
.ai-markdown li {
  margin: 0.25em 0;
  line-height: 1.7;
}
.ai-markdown blockquote {
  margin: 0.75em 0;
  padding: 0.5em 0.75em;
  border-left: 3px solid var(--accent);
  background: var(--bg-primary);
  border-radius: 0 6px 6px 0;
  color: var(--text-secondary);
}
.ai-markdown blockquote p {
  margin: 0.25em 0;
}
.ai-markdown a {
  color: var(--accent);
  text-decoration: none;
}
.ai-markdown a:hover {
  text-decoration: underline;
}
.ai-markdown table {
  width: 100%;
  border-collapse: collapse;
  margin: 0.75em 0;
  font-size: 0.9em;
}
.ai-markdown th, .ai-markdown td {
  border: 1px solid var(--border-color);
  padding: 6px 10px;
  text-align: left;
}
.ai-markdown th {
  background: var(--bg-tertiary);
  font-weight: 600;
}
.ai-markdown hr {
  border: none;
  border-top: 1px solid var(--border-color);
  margin: 1em 0;
}
.ai-markdown img {
  max-width: 100%;
  border-radius: 8px;
  margin: 0.5em 0;
}

/* 代码块样式 - 适配主题 */
.ai-markdown pre {
  background: var(--bg-tertiary) !important;
  border: 1px solid var(--border-color);
}

.ai-markdown pre code {
  color: var(--text-primary);
}

/* 代码高亮颜色 - 使用 CSS 变量 */
.ai-markdown .hljs {
  color: var(--text-primary);
  background: transparent;
}

.ai-markdown .hljs-keyword,
.ai-markdown .hljs-selector-tag,
.ai-markdown .hljs-subst {
  color: var(--accent);
  font-weight: bold;
}

.ai-markdown .hljs-string,
.ai-markdown .hljs-attr,
.ai-markdown .hljs-attribute {
  color: #22c55e;
}

.ai-markdown .hljs-number,
.ai-markdown .hljs-literal {
  color: #f97316;
}

.ai-markdown .hljs-comment {
  color: var(--text-muted);
  font-style: italic;
}

.ai-markdown .hljs-function,
.ai-markdown .hljs-title {
  color: #3b82f6;
}

.ai-markdown .hljs-params {
  color: var(--text-secondary);
}

.ai-markdown .hljs-tag {
  color: var(--accent);
}

.ai-markdown .hljs-name {
  color: #ef4444;
}

/* 深色模式下的代码高亮调整 */
.dark .ai-markdown .hljs-string,
.dark .ai-markdown .hljs-attr,
.dark .ai-markdown .hljs-attribute {
  color: #4ade80;
}

.dark .ai-markdown .hljs-number,
.dark .ai-markdown .hljs-literal {
  color: #fb923c;
}

.dark .ai-markdown .hljs-function,
.dark .ai-markdown .hljs-title {
  color: #60a5fa;
}

.dark .ai-markdown .hljs-name {
  color: #f87171;
}

/* 实时预览区域代码块样式 */
.preview-content pre {
  background: var(--bg-tertiary) !important;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  overflow-x: auto;
  margin: 0.5em 0;
  padding: 12px;
}

.preview-content pre code {
  font-family: 'JetBrains Mono', Menlo, monospace;
  font-size: 0.85em;
  line-height: 1.6;
  display: block;
  color: var(--text-primary);
  background: transparent !important;
}

.preview-content .hljs {
  color: var(--text-primary);
  background: transparent !important;
}

.preview-content .hljs-keyword,
.preview-content .hljs-selector-tag,
.preview-content .hljs-subst {
  color: var(--accent);
  font-weight: bold;
}

.preview-content .hljs-string,
.preview-content .hljs-attr,
.preview-content .hljs-attribute {
  color: #22c55e;
}

.preview-content .hljs-number,
.preview-content .hljs-literal {
  color: #f97316;
}

.preview-content .hljs-comment {
  color: var(--text-muted);
  font-style: italic;
}

.preview-content .hljs-function,
.preview-content .hljs-title {
  color: #3b82f6;
}

.preview-content .hljs-params {
  color: var(--text-secondary);
}

.preview-content .hljs-tag {
  color: var(--accent);
}

.preview-content .hljs-name {
  color: #ef4444;
}

/* 深色模式下的实时预览代码高亮 */
.dark .preview-content .hljs-string,
.dark .preview-content .hljs-attr,
.dark .preview-content .hljs-attribute {
  color: #4ade80;
}

.dark .preview-content .hljs-number,
.dark .preview-content .hljs-literal {
  color: #fb923c;
}

.dark .preview-content .hljs-function,
.dark .preview-content .hljs-title {
  color: #60a5fa;
}

.dark .preview-content .hljs-name {
  color: #f87171;
}
</style>
