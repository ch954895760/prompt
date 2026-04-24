<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MainLayout from '@/components/MainLayout.vue'
import { createPrompt, updatePrompt, getPrompt } from '@/api/prompt'
import { getCategoryList } from '@/api/category'
import { getTags, createTag } from '@/api/tag'
import type { Category, Tag, Prompt } from '@/types'
import { Save, Play, Copy, Trash2, X, History, RotateCcw, Square } from 'lucide-vue-next'
import DeleteConfirmDialog from '@/components/DeleteConfirmDialog.vue'
import VariableInput from '@/components/VariableInput.vue'
import AiTestDialog from '@/components/AiTestDialog.vue'
import { getPromptHistory, rollbackPrompt } from '@/api/prompt'
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
const isEdit = computed(() => !!route.params.id)
const promptId = computed(() => Number(route.params.id))

const title = ref('')
const content = ref('')
const description = ref('')
const categoryId = ref<number | null>(null)
const selectedTagIds = ref<number[]>([])
const newTagInput = ref('')
const tagInputFocused = ref(false)

const categories = ref<Category[]>([])
const tags = ref<Tag[]>([])
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
  return ''
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
        formattedValue = `<pre style="margin: 0.5em 0; border-radius: 8px; overflow-x: auto; background: var(--bg-tertiary); border: 1px solid var(--border-color); padding: 12px;"><code style="font-family: 'JetBrains Mono', Menlo, monospace; font-size: 0.85em; line-height: 1.6; color: var(--text-primary);">${value.replace(/</g, '&lt;').replace(/>/g, '&gt;')}</code></pre>`
      }
    } else if (value.includes('\n')) {
      // 多行文本：将换行符转换为 <br> 标签
      formattedValue = value.replace(/\n/g, '<br>')
    } else {
      // 单行文本
      formattedValue = value
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
  const [cList, tList, providers] = await Promise.all([
    getCategoryList(),
    getTags(),
    getAiProviders(),
  ])
  categories.value = cList
  tags.value = tList
  aiProviders.value = providers
  // Set default provider
  const defaultProvider = providers.find(p => p.isDefault)
  if (defaultProvider) {
    selectedAiProvider.value = defaultProvider.id
  } else if (providers.length > 0) {
    selectedAiProvider.value = providers[0].id
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
    if (p.variablesJson) {
      variableValues.value = typeof p.variablesJson === 'string' ? JSON.parse(p.variablesJson) : p.variablesJson
    }
  } catch (e) {
    console.error('[DEBUG] Failed to load prompt:', e)
  }
}

async function handleSave() {
  if (!title.value.trim()) {
    showToast('请输入提示词标题')
    return
  }
  if (!content.value.trim()) {
    showToast('请输入提示词内容')
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
      showToast('提示词已更新')
    } else {
      await createPrompt(data)
      showToast('提示词已创建')
    }
    setTimeout(() => router.push('/prompts'), 800)
  } catch (e: any) {
    showToast(e.message || '保存失败')
  } finally {
    loading.value = false
  }
}

function handleCopy() {
  const text = content.value.replace(/\{\{(\w+)\}\}/g, (match, varName) => {
    return variableValues.value[varName] || match
  })
  navigator.clipboard.writeText(text).then(() => {
    showToast('已复制到剪贴板')
  })
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

async function confirmRollback() {
  if (!rollbackVersion.value) return
  try {
    await rollbackPrompt(promptId.value, rollbackVersion.value)
    showToast('已回滚到指定版本')
    await loadPrompt()
    await loadHistory()
  } catch (e: any) {
    showToast(e.message || '回滚失败')
  } finally {
    rollbackVersion.value = null
  }
}

const toastVisible = ref(false)
const toastMessage = ref('')
let toastTimer: ReturnType<typeof setTimeout>

function showToast(message: string) {
  toastMessage.value = message
  toastVisible.value = true
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toastVisible.value = false
  }, 2500)
}

onMounted(() => {
  loadData()
  loadPrompt()
  loadHistory()
})
</script>

<template>
  <MainLayout>
    <div class="animate-fade-in">
      <div class="flex items-center justify-between mb-6">
        <div>
          <h2 class="text-2xl font-bold mb-1" style="color: var(--text-primary)">
            {{ isEdit ? '编辑提示词' : '新建提示词' }}
          </h2>
          <p class="text-sm" style="color: var(--text-secondary)">
            {{ isEdit ? '修改并更新你的提示词模板' : '创建并测试你的提示词模板' }}
          </p>
        </div>
        <div class="flex items-center gap-2">
          <button v-if="isEdit" @click="showHistory = !showHistory"
            class="flex items-center gap-2 px-4 py-2.5 text-sm font-medium rounded-xl transition-all border"
            :class="showHistory ? 'bg-[#ea580c]/10 border-[#ea580c]/30 text-[#ea580c]' : 'border-color: var(--border-color); color: var(--text-secondary);'"
            style="border-color: var(--border-color);"
          >
            <History class="w-4 h-4" />
            历史版本
          </button>
          <button @click="handleSave" :disabled="loading"
            class="flex items-center gap-2 px-4 py-2.5 bg-[#ea580c] hover:bg-[#c2410c] text-white text-sm font-medium rounded-xl transition-all shadow-lg shadow-[#ea580c]/20 disabled:opacity-50"
          >
            <Save class="w-4 h-4" />
            {{ loading ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>

      <div class="grid grid-cols-1 xl:grid-cols-2 gap-6">
        <!-- Editor panel -->
        <div class="space-y-5">
          <div>
            <label class="block text-xs font-medium mb-2" style="color: var(--text-secondary)">标题</label>
            <input v-model="title" type="text"
              class="w-full px-4 py-3 rounded-xl text-base font-medium transition-all"
              style="background: var(--bg-secondary); border: 1px solid var(--border-color); color: var(--text-primary);"
              placeholder="给你的提示词起个名字..."
              @focus="($event.target as HTMLElement).style.borderColor = 'var(--accent)'"
              @blur="($event.target as HTMLElement).style.borderColor = 'var(--border-color)'"
            >
          </div>

          <div class="flex gap-4">
            <div class="flex-1">
              <label class="block text-xs font-medium mb-2" style="color: var(--text-secondary)">分类</label>
              <select v-model="categoryId"
                class="w-full px-4 py-2.5 rounded-xl text-sm transition-all"
                style="background: var(--bg-secondary); border: 1px solid var(--border-color); color: var(--text-primary);"
              >
                <option :value="null">未分类</option>
                <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
              </select>
            </div>
            <div class="flex-1">
              <label class="block text-xs font-medium mb-2" style="color: var(--text-secondary)">标签</label>
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
                  placeholder="输入标签名按回车"
                  @keyup.enter="handleAddTag"
                  @focus="tagInputFocused = true"
                  @blur="tagInputFocused = false"
                >
              </div>
            </div>
          </div>

          <div>
            <div class="flex items-center justify-between mb-2">
              <label class="block text-xs font-medium" style="color: var(--text-secondary)">提示词内容</label>
              <span class="text-[10px] px-2 py-1 rounded-md" style="background: var(--bg-tertiary); color: var(--text-muted)">
                使用 <code v-pre style="color: var(--accent); font-family: monospace; font-size: 0.85em; background: var(--accent-soft); padding: 1px 4px; border-radius: 4px;">{{变量名}}</code> 插入变量
              </span>
            </div>
            <textarea v-model="content"
              class="w-full px-4 py-4 rounded-xl transition-all"
              style="background: var(--bg-secondary); border: 1px solid var(--border-color); color: var(--text-primary); font-family: 'Crimson Pro', Georgia, serif; font-size: 16px; line-height: 1.7; resize: vertical; min-height: 300px;"
              placeholder="在这里输入你的提示词模板..."
              @focus="($event.target as HTMLElement).style.borderColor = 'var(--accent)'"
              @blur="($event.target as HTMLElement).style.borderColor = 'var(--border-color)'"
            >
            </textarea>
          </div>

          <!-- Variable inputs -->
          <div v-if="extractedVars.length > 0">
            <label class="block text-xs font-medium mb-3" style="color: var(--text-secondary)">填写变量值</label>
            <div class="grid grid-cols-1 gap-3">
              <div v-for="v in extractedVars" :key="v">
                <label class="block text-[10px] font-medium uppercase tracking-wider mb-1.5" style="color: var(--text-muted)">{{ v }}</label>
                <VariableInput
                  :name="v"
                  v-model="variableValues[v]"
                  :placeholder="`输入 ${v}...`"
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
              测试运行
            </button>
            <button v-else @click="handleStopTest"
              class="flex items-center gap-1.5 px-4 py-2 border-2 text-sm font-medium rounded-xl transition-all active:scale-[0.98]"
              style="border-color: #dc2626; color: #dc2626;"
            >
              <Square class="w-4 h-4" />
              停止生成
            </button>
            <button @click="clearEditor"
              class="px-4 py-2.5 text-sm font-medium rounded-xl transition-colors hover:bg-surface-200 dark:hover:bg-surface-800"
              style="color: var(--text-secondary);"
            >
              清空
            </button>
          </div>

          <!-- History panel -->
          <div v-if="showHistory && isEdit" class="rounded-2xl p-5 mt-4" style="background: var(--bg-secondary); border: 1px solid var(--border-color);">
            <h3 class="text-sm font-semibold mb-3 flex items-center gap-2" style="color: var(--text-primary)">
              <History class="w-4 h-4" style="color: var(--accent)" />
              版本历史
            </h3>
            <div v-if="historyLoading" class="text-xs py-2" style="color: var(--text-muted)">加载中...</div>
            <div v-else-if="historyList.length === 0" class="text-xs py-2" style="color: var(--text-muted)">暂无历史版本</div>
            <div v-else class="space-y-2 max-h-[300px] overflow-y-auto">
              <div v-for="h in historyList" :key="h.id"
                class="flex items-center justify-between px-3 py-2.5 rounded-xl text-xs"
                style="background: var(--bg-primary); border: 1px solid var(--border-color);"
              >
                <div class="flex items-center gap-3">
                  <span class="font-mono font-semibold px-2 py-0.5 rounded-md" style="background: var(--accent-soft); color: var(--accent);">v{{ h.version }}</span>
                  <span style="color: var(--text-secondary)">{{ new Date(h.createdAt).toLocaleString() }}</span>
                </div>
                <button @click="handleRollback(h.version)"
                  class="flex items-center gap-1 px-2 py-1 rounded-lg transition-colors hover:bg-surface-200 dark:hover:bg-surface-800"
                  style="color: var(--accent);"
                  title="回滚到此版本"
                >
                  <RotateCcw class="w-3 h-3" />
                  回滚
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Preview + AI panel -->
        <div class="flex flex-col gap-4 self-start" style="position: sticky; top: 24px;">
          <!-- Preview -->
          <div>
            <label class="block text-xs font-medium mb-2" style="color: var(--text-secondary)">实时预览</label>
            <div class="rounded-2xl p-6 min-h-[400px] relative transition-all"
              style="background: var(--bg-secondary); border: 1px solid var(--border-color); font-family: 'Crimson Pro', Georgia, serif; line-height: 1.8;"
            >
              <div class="absolute top-4 right-4">
                <button @click="handleCopy"
                  class="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-lg transition-all hover:bg-surface-200 dark:hover:bg-surface-800"
                  style="color: var(--text-muted); border: 1px solid var(--border-color);"
                >
                  <Copy class="w-3.5 h-3.5" />
                  复制
                </button>
              </div>
              <div class="pt-8 text-base preview-content" style="white-space: pre-wrap; color: var(--text-primary);" v-html="previewContent || '<span style=\'color: var(--text-muted); font-style: italic;\'>提示词预览将在这里显示...</span>'">
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
                <span class="text-xs font-medium" style="color: var(--text-primary)">AI 响应</span>
                <span v-if="aiLoading" class="text-[10px] px-2 py-0.5 rounded-full animate-pulse" style="background: var(--accent-soft); color: var(--accent);">生成中...</span>
              </div>
              <button v-if="aiLoading" @click="handleStopTest"
                class="flex items-center gap-1 px-2 py-1 text-[10px] font-medium rounded-md transition-all"
                style="background: rgba(220, 38, 38, 0.08); color: #dc2626; border: 1px solid rgba(220, 38, 38, 0.25);"
                title="停止生成"
              >
                <Square class="w-3 h-3" />
                停止
              </button>
            </div>

            <!-- Content -->
            <div ref="aiContentRef" class="p-5 overflow-y-auto"
              style="min-height: 120px; max-height: 360px;"
            >
              <div v-if="!aiResult && aiLoading" class="flex items-center gap-2 text-sm" style="color: var(--text-muted);">
                <div class="w-4 h-4 border-2 border-[#ea580c] border-t-transparent rounded-full animate-spin"></div>
                正在等待 AI 响应...
              </div>
              <div v-else-if="!aiResult && !aiLoading" class="text-sm" style="color: var(--text-muted);">点击"测试运行"查看 AI 响应</div>
              <div v-else class="text-sm leading-relaxed ai-markdown ai-message-bubble" style="color: var(--text-primary);" v-html="renderedAiResult"></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Rollback Confirm Dialog -->
    <DeleteConfirmDialog
      v-model="rollbackDialogVisible"
      title="确认回滚"
      :description="`确定要回滚到版本 ${rollbackVersion} 吗？当前内容将被替换，此操作不可恢复。`"
      confirm-text="回滚"
      @confirm="confirmRollback"
    />

    <!-- AI Test Dialog -->
    <AiTestDialog
      v-model="showAiTestDialog"
      :initial-prompt="processedPrompt"
      :provider-id="selectedAiProvider"
    />

    <!-- Toast -->
    <div v-if="toastVisible"
      class="fixed bottom-6 right-6 px-5 py-3 rounded-xl flex items-center gap-2.5 z-50"
      style="background: var(--bg-secondary); border: 1px solid var(--border-color); box-shadow: 0 12px 40px rgba(0,0,0,0.15); animation: slideUp 0.3s ease;"
    >
      <div class="w-5 h-5 rounded-full bg-emerald-500/10 flex items-center justify-center">
        <div class="w-2 h-2 rounded-full bg-emerald-500"></div>
      </div>
      <span class="text-sm" style="color: var(--text-primary)">{{ toastMessage }}</span>
    </div>
  </MainLayout>
</template>

<style scoped>
@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>

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
