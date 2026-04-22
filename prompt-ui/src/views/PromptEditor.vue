<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MainLayout from '@/components/MainLayout.vue'
import { createPrompt, updatePrompt, getPrompt } from '@/api/prompt'
import { getCategoryList } from '@/api/category'
import { getTags, createTag } from '@/api/tag'
import type { Category, Tag, Prompt } from '@/types'
import { Save, Play, Copy, Trash2, X, History, RotateCcw } from 'lucide-vue-next'
import { getPromptHistory, rollbackPrompt } from '@/api/prompt'

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

const categories = ref<Category[]>([])
const tags = ref<Tag[]>([])
const variableValues = ref<Record<string, string>>({})
const aiResult = ref('')
const showAiResult = ref(false)
const loading = ref(false)

const extractedVars = computed(() => {
  const vars = [...content.value.matchAll(/\{\{(\w+)\}\}/g)].map(m => m[1])
  return [...new Set(vars)]
})

const previewContent = computed(() => {
  let preview = content.value
  for (const [key, value] of Object.entries(variableValues.value)) {
    const regex = new RegExp(`\\{\\{${key}\\}\\}`, 'g')
    preview = preview.replace(regex, value || `{{${key}}}`)
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
  const [cList, tList] = await Promise.all([
    getCategoryList(),
    getTags(),
  ])
  categories.value = cList
  tags.value = tList
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
  showAiResult.value = true
  aiResult.value = 'AI 测试功能需要配置 base_url 和 api_key。请在设置中配置后使用。'
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

async function handleRollback(version: number) {
  if (!confirm(`确定要回滚到版本 ${version} 吗？`)) return
  try {
    await rollbackPrompt(promptId.value, version)
    showToast('已回滚到指定版本')
    await loadPrompt()
    await loadHistory()
  } catch (e: any) {
    showToast(e.message || '回滚失败')
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
              <div class="flex flex-wrap gap-1 mb-2">
                <span v-for="tagId in selectedTagIds" :key="tagId"
                  class="inline-flex items-center gap-1 text-xs px-2 py-1 rounded-md"
                  style="background: var(--accent-soft); color: var(--accent);"
                >
                  {{ tags.find(t => t.id === tagId)?.name || tagId }}
                  <button @click="removeTag(tagId)" class="hover:opacity-70">
                    <X class="w-3 h-3" />
                  </button>
                </span>
              </div>
              <div class="flex gap-2">
                <input v-model="newTagInput" type="text"
                  class="flex-1 px-3 py-2 rounded-lg text-sm transition-all"
                  style="background: var(--bg-secondary); border: 1px solid var(--border-color); color: var(--text-primary);"
                  placeholder="输入标签名按回车"
                  @keyup.enter="handleAddTag"
                  @focus="($event.target as HTMLElement).style.borderColor = 'var(--accent)'"
                  @blur="($event.target as HTMLElement).style.borderColor = 'var(--border-color)'"
                >
              </div>
            </div>
          </div>

          <div>
            <div class="flex items-center justify-between mb-2">
              <label class="block text-xs font-medium" style="color: var(--text-secondary)">提示词内容</label>
              <span class="text-[10px] px-2 py-1 rounded-md" style="background: var(--bg-tertiary); color: var(--text-muted)">
                使用 <code style="color: var(--accent); font-family: monospace; font-size: 0.85em; background: var(--accent-soft); padding: 1px 4px; border-radius: 4px;">{{变量名}}</code> 插入变量
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
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div v-for="v in extractedVars" :key="v">
                <label class="block text-[10px] font-medium uppercase tracking-wider mb-1.5" style="color: var(--text-muted)">{{ v }}</label>
                <input v-model="variableValues[v]" type="text"
                  class="w-full px-3 py-2 rounded-lg text-sm transition-all"
                  style="background: var(--bg-secondary); border: 1px solid var(--border-color); color: var(--text-primary);"
                  :placeholder="`输入 ${v}...`"
                  @focus="($event.target as HTMLElement).style.borderColor = 'var(--accent)'"
                  @blur="($event.target as HTMLElement).style.borderColor = 'var(--border-color)'"
                >
              </div>
            </div>
          </div>

          <div class="flex items-center gap-3 pt-2">
            <button @click="handleTest"
              class="flex items-center gap-2 px-5 py-2.5 border-2 font-medium rounded-xl transition-all active:scale-[0.98]"
              style="border-color: var(--accent); color: var(--accent);"
            >
              <Play class="w-4 h-4" />
              测试运行
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

        <!-- Preview panel -->
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
            <div class="pt-8 text-base" style="white-space: pre-wrap; color: var(--text-primary);" v-html="previewContent || '<span style=\'color: var(--text-muted); font-style: italic;\'>提示词预览将在这里显示...</span>'">
            </div>
          </div>

          <!-- AI Test Result -->
          <div v-if="showAiResult" class="mt-5">
            <div class="flex items-center gap-2 mb-2">
              <div class="w-2 h-2 rounded-full bg-[#f97316] animate-pulse"></div>
              <label class="text-xs font-medium" style="color: var(--accent)">AI 响应</label>
            </div>
            <div class="rounded-2xl p-5 min-h-[120px]" style="background: var(--accent-soft);">
              <div class="text-sm leading-relaxed" style="color: var(--text-primary);">{{ aiResult }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

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
