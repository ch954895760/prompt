<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import { getSettings, updateSettings, aiTestStream } from '@/api/setting'
import { exportPromptsJson, exportPromptsMarkdown, importPrompts } from '@/api/prompt'
import { useUserStore } from '@/stores/user'
import type { UserSetting, AiProvider, AiProviderCreateRequest, AiProviderUpdateRequest } from '@/types'
import { getAiProviders, createAiProvider, updateAiProvider, deleteAiProvider, setDefaultAiProvider } from '@/api/aiProvider'
import { User, Cpu, Palette, Database, Download, Upload, Eye, EyeOff, Sun, Moon, Plus, Edit2, Trash2, Check, X, Bot, Sparkles, MessageSquare } from 'lucide-vue-next'

const userStore = useUserStore()
const setting = ref<UserSetting | null>(null)
const loading = ref(false)

const showApiKey = ref(false)
const importFile = ref<HTMLInputElement | null>(null)
const form = ref({
  username: '',
  email: '',
  theme: userStore.theme,
  defaultModel: '',
  apiBaseUrl: '',
  apiKey: '',
  model: '',
})

// AI Provider Management
const aiProviders = ref<AiProvider[]>([])
const showAiProviderModal = ref(false)
const editingProvider = ref<AiProvider | null>(null)
const aiProviderForm = ref({
  name: '',
  provider: 'openai',
  apiBaseUrl: '',
  apiKey: '',
  model: '',
  isDefault: false,
})
const showAiProviderApiKey = ref(false)
const aiProviderLoading = ref(false)

// AI Test
const testContent = ref('')
const testResponse = ref('')
const isTesting = ref(false)
const testAbort = ref<(() => void) | null>(null)
const selectedTestProvider = ref<number | null>(null)

const providerOptions = [
  { value: 'openai', label: 'OpenAI', icon: '⚡', models: ['gpt-4', 'gpt-4-turbo', 'gpt-4o', 'gpt-4o-mini', 'gpt-3.5-turbo'] },
  { value: 'anthropic', label: 'Anthropic Claude', icon: '🌟', models: ['claude-3-opus-20240229', 'claude-3-sonnet-20240229', 'claude-3-haiku-20240307', 'claude-3-5-sonnet-20240620'] },
  { value: 'google', label: 'Google Gemini', icon: '🔮', models: ['gemini-1.5-pro', 'gemini-1.5-flash', 'gemini-pro'] },
  { value: 'deepseek', label: 'DeepSeek', icon: '🐋', models: ['deepseek-chat', 'deepseek-coder'] },
  { value: 'qwen', label: '通义千问', icon: '🌙', models: ['qwen-turbo', 'qwen-plus', 'qwen-max'] },
  { value: 'wenxin', label: '文心一言', icon: '📚', models: ['ernie-bot-4', 'ernie-bot'] },
  { value: 'custom', label: '自定义', icon: '⚙️', models: [] },
]

const currentProvider = computed(() => {
  return providerOptions.find(p => p.value === aiProviderForm.value.provider)
})

const defaultBaseUrls: Record<string, string> = {
  openai: 'https://api.openai.com/v1',
  anthropic: 'https://api.anthropic.com',
  google: 'https://generativelanguage.googleapis.com',
  deepseek: 'https://api.deepseek.com',
  qwen: 'https://dashscope.aliyuncs.com/api/v1',
  wenxin: 'https://aip.baidubce.com',
  custom: '',
}

async function loadData() {
  try {
    const s = await getSettings()
    setting.value = s
    if (s.theme && !localStorage.getItem('theme')) {
      userStore.setTheme(s.theme)
    }
    form.value.theme = userStore.theme
    form.value.defaultModel = s.defaultModel || ''
    form.value.apiBaseUrl = s.apiBaseUrl || ''
    form.value.model = s.model || ''
    form.value.apiKey = s.apiKeyEncrypted || ''
    if (userStore.user) {
      form.value.username = userStore.user.username
      form.value.email = userStore.user.email
    }
  } catch (e) {
    console.error('[DEBUG] Failed to load settings:', e)
  }
}

async function loadAiProviders() {
  try {
    aiProviders.value = await getAiProviders()
  } catch (e) {
    console.error('[DEBUG] Failed to load AI providers:', e)
  }
}

watch(() => userStore.theme, (newTheme) => {
  form.value.theme = newTheme
})

function toggleTheme() {
  userStore.toggleTheme()
  form.value.theme = userStore.theme
}

async function handleSave() {
  loading.value = true
  try {
    await updateSettings({
      theme: form.value.theme,
      defaultModel: form.value.defaultModel,
      apiBaseUrl: form.value.apiBaseUrl,
      apiKeyEncrypted: form.value.apiKey,
      model: form.value.model,
    })
    showToast('设置已保存')
  } catch (e: any) {
    showToast(e.message || '保存失败')
  } finally {
    loading.value = false
  }
}

async function handleExportJson() {
  try {
    const blob = await exportPromptsJson()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `prompts-${new Date().toISOString().slice(0, 10)}.json`
    a.click()
    URL.revokeObjectURL(url)
    showToast('JSON 导出成功')
  } catch (e: any) {
    showToast(e.message || '导出失败')
  }
}

async function handleExportMarkdown() {
  try {
    const blob = await exportPromptsMarkdown()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `prompts-${new Date().toISOString().slice(0, 10)}.md`
    a.click()
    URL.revokeObjectURL(url)
    showToast('Markdown 导出成功')
  } catch (e: any) {
    showToast(e.message || '导出失败')
  }
}

function handleImportClick() {
  importFile.value?.click()
}

async function handleImportFile(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  try {
    const text = await file.text()
    const data = JSON.parse(text)
    if (!Array.isArray(data)) {
      showToast('文件格式错误：应为 JSON 数组')
      return
    }
    const count = await importPrompts(data)
    showToast(`成功导入 ${count} 条提示词`)
  } catch (e: any) {
    showToast(e.message || '导入失败')
  } finally {
    target.value = ''
  }
}

// AI Provider Functions
function openAddAiProvider() {
  editingProvider.value = null
  aiProviderForm.value = {
    name: '',
    provider: 'openai',
    apiBaseUrl: defaultBaseUrls.openai,
    apiKey: '',
    model: 'gpt-4',
    isDefault: aiProviders.value.length === 0,
  }
  showAiProviderApiKey.value = false
  showAiProviderModal.value = true
}

function openEditAiProvider(provider: AiProvider) {
  editingProvider.value = provider
  aiProviderForm.value = {
    name: provider.name,
    provider: provider.provider,
    apiBaseUrl: provider.apiBaseUrl,
    apiKey: '',
    model: provider.model,
    isDefault: provider.isDefault,
  }
  showAiProviderApiKey.value = false
  showAiProviderModal.value = true
}

function onProviderChange() {
  const provider = aiProviderForm.value.provider
  aiProviderForm.value.apiBaseUrl = defaultBaseUrls[provider] || ''
  const models = providerOptions.find(p => p.value === provider)?.models || []
  aiProviderForm.value.model = models[0] || ''
}

async function handleSaveAiProvider() {
  if (!aiProviderForm.value.name.trim()) {
    showToast('请输入配置名称')
    return
  }
  if (!aiProviderForm.value.apiBaseUrl.trim()) {
    showToast('请输入API Base URL')
    return
  }
  if (!aiProviderForm.value.model.trim()) {
    showToast('请输入模型名称')
    return
  }

  aiProviderLoading.value = true
  try {
    if (editingProvider.value) {
      const updateData: AiProviderUpdateRequest = {
        name: aiProviderForm.value.name,
        provider: aiProviderForm.value.provider,
        apiBaseUrl: aiProviderForm.value.apiBaseUrl,
        model: aiProviderForm.value.model,
        isDefault: aiProviderForm.value.isDefault,
      }
      if (aiProviderForm.value.apiKey) {
        updateData.apiKey = aiProviderForm.value.apiKey
      }
      await updateAiProvider(editingProvider.value.id, updateData)
      showToast('AI配置已更新')
    } else {
      if (!aiProviderForm.value.apiKey) {
        showToast('请输入API Key')
        aiProviderLoading.value = false
        return
      }
      const createData: AiProviderCreateRequest = {
        name: aiProviderForm.value.name,
        provider: aiProviderForm.value.provider,
        apiBaseUrl: aiProviderForm.value.apiBaseUrl,
        apiKey: aiProviderForm.value.apiKey,
        model: aiProviderForm.value.model,
        isDefault: aiProviderForm.value.isDefault,
      }
      await createAiProvider(createData)
      showToast('AI配置已添加')
    }
    showAiProviderModal.value = false
    await loadAiProviders()
  } catch (e: any) {
    showToast(e.message || '保存失败')
  } finally {
    aiProviderLoading.value = false
  }
}

async function handleDeleteAiProvider(provider: AiProvider) {
  if (!confirm(`确定要删除 "${provider.name}" 吗？`)) {
    return
  }
  try {
    await deleteAiProvider(provider.id)
    showToast('AI配置已删除')
    await loadAiProviders()
  } catch (e: any) {
    showToast(e.message || '删除失败')
  }
}

async function handleSetDefault(provider: AiProvider) {
  if (provider.isDefault) return
  try {
    await setDefaultAiProvider(provider.id)
    showToast('已设为默认')
    await loadAiProviders()
  } catch (e: any) {
    showToast(e.message || '设置失败')
  }
}

function getProviderLabel(providerValue: string) {
  return providerOptions.find(p => p.value === providerValue)?.label || providerValue
}

function getProviderIcon(providerValue: string) {
  return providerOptions.find(p => p.value === providerValue)?.icon || '🤖'
}

// AI Test Functions
async function handleAiTest() {
  if (!testContent.value.trim()) {
    showToast('请输入测试内容')
    return
  }
  if (isTesting.value) {
    testAbort.value?.()
    return
  }

  isTesting.value = true
  testResponse.value = ''

  const abort = aiTestStream(testContent.value, {
    onChunk: (text) => {
      testResponse.value += text
    },
    onDone: () => {
      isTesting.value = false
      testAbort.value = null
    },
    onError: (error) => {
      showToast(error)
      isTesting.value = false
      testAbort.value = null
    },
  }, selectedTestProvider.value)

  testAbort.value = abort
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
  loadAiProviders()
})
</script>

<template>
  <MainLayout>
    <div class="animate-fade-in">
      <div class="mb-6">
        <h2 class="text-2xl font-bold mb-1" style="color: var(--text-primary)">设置</h2>
        <p class="text-sm" style="color: var(--text-secondary)">管理你的账户与应用偏好</p>
      </div>

      <div class="max-w-3xl space-y-6">
        <!-- Profile -->
        <div class="rounded-2xl p-6" style="background: var(--bg-secondary); border: 1px solid var(--border-color);">
          <h3 class="font-semibold mb-5 flex items-center gap-2" style="color: var(--text-primary)">
            <User class="w-4 h-4" style="color: var(--accent)" />
            个人信息
          </h3>
          <div class="flex items-center gap-4 mb-5">
            <div class="w-16 h-16 rounded-full bg-gradient-to-br from-[#fb923c] to-[#ea580c] text-white flex items-center justify-center text-xl font-bold">
              {{ form.username?.charAt(0)?.toUpperCase() || 'U' }}
            </div>
            <div>
              <button class="text-xs font-medium px-3 py-1.5 rounded-lg border transition-colors hover:bg-surface-100 dark:hover:bg-surface-800"
                style="border-color: var(--border-color); color: var(--text-secondary);"
              >
                更换头像
              </button>
            </div>
          </div>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">用户名</label>
              <input v-model="form.username" type="text" disabled
                class="w-full px-4 py-2.5 rounded-xl text-sm"
                style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-muted);"
              >
            </div>
            <div>
              <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">邮箱</label>
              <input v-model="form.email" type="email" disabled
                class="w-full px-4 py-2.5 rounded-xl text-sm"
                style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-muted);"
              >
            </div>
          </div>
        </div>

        <!-- AI Provider Management -->
        <div class="rounded-2xl p-6" style="background: var(--bg-secondary); border: 1px solid var(--border-color);">
          <div class="flex items-center justify-between mb-5">
            <h3 class="font-semibold flex items-center gap-2" style="color: var(--text-primary)">
              <Bot class="w-4 h-4" style="color: var(--accent)" />
              AI 模型配置
            </h3>
            <button @click="openAddAiProvider"
              class="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-lg transition-colors"
              style="background: var(--accent); color: white;"
            >
              <Plus class="w-3.5 h-3.5" />
              添加配置
            </button>
          </div>

          <!-- AI Provider List -->
          <div class="space-y-3">
            <div v-if="aiProviders.length === 0" class="text-center py-8 rounded-xl" style="background: var(--bg-primary); border: 1px dashed var(--border-color);">
              <Bot class="w-10 h-10 mx-auto mb-2" style="color: var(--text-muted)" />
              <p class="text-sm" style="color: var(--text-muted)">暂无AI配置</p>
              <p class="text-xs mt-1" style="color: var(--text-muted)">点击上方按钮添加你的第一个AI模型</p>
            </div>

            <div v-for="provider in aiProviders" :key="provider.id"
              class="flex items-center gap-3 p-4 rounded-xl transition-all hover:shadow-md"
              :class="provider.isDefault ? 'ring-1' : ''"
              style="background: var(--bg-primary); border: 1px solid var(--border-color);"
              :style="provider.isDefault ? 'ring-color: var(--accent)' : ''"
            >
              <div class="w-10 h-10 rounded-lg flex items-center justify-center text-lg"
                style="background: var(--bg-secondary);"
              >
                {{ getProviderIcon(provider.provider) }}
              </div>
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2">
                  <span class="font-medium text-sm truncate" style="color: var(--text-primary)">{{ provider.name }}</span>
                  <span v-if="provider.isDefault"
                    class="text-[10px] px-1.5 py-0.5 rounded-full"
                    style="background: var(--accent); color: white;"
                  >
                    默认
                  </span>
                </div>
                <div class="text-xs mt-0.5" style="color: var(--text-muted)">
                  {{ getProviderLabel(provider.provider) }} · {{ provider.model }}
                </div>
              </div>
              <div class="flex items-center gap-1">
                <button v-if="!provider.isDefault" @click="handleSetDefault(provider)"
                  class="p-2 rounded-lg transition-colors hover:bg-surface-100 dark:hover:bg-surface-800"
                  style="color: var(--text-muted);"
                  title="设为默认"
                >
                  <Check class="w-4 h-4" />
                </button>
                <button @click="openEditAiProvider(provider)"
                  class="p-2 rounded-lg transition-colors hover:bg-surface-100 dark:hover:bg-surface-800"
                  style="color: var(--text-muted);"
                  title="编辑"
                >
                  <Edit2 class="w-4 h-4" />
                </button>
                <button @click="handleDeleteAiProvider(provider)"
                  class="p-2 rounded-lg transition-colors hover:bg-red-50 dark:hover:bg-red-900/20"
                  style="color: var(--text-muted);"
                  title="删除"
                >
                  <Trash2 class="w-4 h-4 hover:text-red-500" />
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- AI Test -->
        <div class="rounded-2xl p-6" style="background: var(--bg-secondary); border: 1px solid var(--border-color);">
          <h3 class="font-semibold mb-5 flex items-center gap-2" style="color: var(--text-primary)">
            <Sparkles class="w-4 h-4" style="color: var(--accent)" />
            AI 测试
          </h3>
          <div class="space-y-4">
            <div>
              <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">选择模型</label>
              <select v-model="selectedTestProvider"
                class="w-full px-4 py-2.5 rounded-xl text-sm transition-all"
                style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
              >
                <option :value="null">使用默认配置</option>
                <option v-for="provider in aiProviders" :key="provider.id" :value="provider.id">
                  {{ provider.name }}
                </option>
              </select>
            </div>
            <div>
              <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">测试内容</label>
              <textarea v-model="testContent" rows="3"
                class="w-full px-4 py-2.5 rounded-xl text-sm resize-none transition-all"
                style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
                placeholder="输入你想测试的提示词..."
              ></textarea>
            </div>
            <button @click="handleAiTest" :disabled="isTesting && !testAbort"
              class="w-full py-2.5 font-medium rounded-xl transition-all flex items-center justify-center gap-2"
              :style="isTesting ? 'background: var(--bg-tertiary); color: var(--text-muted);' : 'background: var(--accent); color: white;'"
            >
              <span v-if="isTesting" class="w-4 h-4 border-2 border-current border-t-transparent rounded-full animate-spin"></span>
              {{ isTesting ? '停止测试' : '开始测试' }}
            </button>
            <div v-if="testResponse" class="p-4 rounded-xl text-sm leading-relaxed"
              style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
            >
              <div class="flex items-center gap-2 mb-2 pb-2" style="border-bottom: 1px solid var(--border-color);">
                <MessageSquare class="w-4 h-4" style="color: var(--accent);" />
                <span class="text-xs font-medium" style="color: var(--text-secondary);">AI 回复</span>
              </div>
              <div class="whitespace-pre-wrap">{{ testResponse }}</div>
            </div>
          </div>
        </div>

        <!-- Legacy AI Integration (Hidden but kept for backward compatibility) -->
        <div class="rounded-2xl p-6 opacity-60" style="background: var(--bg-secondary); border: 1px solid var(--border-color);">
          <h3 class="font-semibold mb-5 flex items-center gap-2" style="color: var(--text-primary)">
            <Cpu class="w-4 h-4" style="color: var(--accent)" />
            旧版 AI 配置
            <span class="text-[10px] px-1.5 py-0.5 rounded-full ml-2" style="background: var(--bg-tertiary); color: var(--text-muted);">已弃用</span>
          </h3>
          <div class="space-y-4">
            <div>
              <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">默认模型</label>
              <select v-model="form.defaultModel"
                class="w-full px-4 py-2.5 rounded-xl text-sm transition-all"
                style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
              >
                <option value="">请选择</option>
                <option value="openai">OpenAI GPT-4</option>
                <option value="claude">Claude 3.5 Sonnet</option>
                <option value="wenxin">文心一言 4.0</option>
              </select>
            </div>
            <div>
              <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">Base URL</label>
              <input v-model="form.apiBaseUrl" type="text"
                class="w-full px-4 py-2.5 rounded-xl text-sm transition-all"
                style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
                placeholder="https://api.openai.com/v1"
              >
            </div>
            <div>
              <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">API Key</label>
              <div class="relative">
                <input v-model="form.apiKey" :type="showApiKey ? 'text' : 'password'"
                  class="w-full px-4 py-2.5 rounded-xl text-sm pr-10 transition-all"
                  style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary); font-family: monospace;"
                  placeholder="sk-xxxxxxxxxxxxxxxx"
                >
                <button @click="showApiKey = !showApiKey"
                  class="absolute right-3 top-1/2 -translate-y-1/2"
                  style="color: var(--text-muted);"
                >
                  <Eye v-if="!showApiKey" class="w-4 h-4" />
                  <EyeOff v-else class="w-4 h-4" />
                </button>
              </div>
            </div>
            <div>
              <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">Model</label>
              <input v-model="form.model" type="text"
                class="w-full px-4 py-2.5 rounded-xl text-sm transition-all"
                style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
                placeholder="gpt-4, claude-3-5-sonnet 等"
              >
            </div>
          </div>
        </div>

        <!-- Appearance -->
        <div class="rounded-2xl p-6" style="background: var(--bg-secondary); border: 1px solid var(--border-color);">
          <h3 class="font-semibold mb-5 flex items-center gap-2" style="color: var(--text-primary)">
            <Palette class="w-4 h-4" style="color: var(--accent)" />
            外观
          </h3>
          <div class="flex items-center justify-between">
            <div>
              <div class="text-sm font-medium mb-0.5" style="color: var(--text-primary)">深色模式</div>
              <div class="text-xs" style="color: var(--text-muted)">切换浅色/深色主题</div>
            </div>
            <button @click="toggleTheme"
              class="relative w-12 h-7 rounded-full transition-colors"
              :style="{ background: form.theme === 'dark' ? 'var(--accent)' : 'var(--bg-tertiary)' }"
            >
              <span class="absolute top-1 w-5 h-5 rounded-full bg-white shadow-md transition-transform flex items-center justify-center"
                :class="form.theme === 'dark' ? 'translate-x-6' : 'translate-x-1'"
              >
                <Sun v-if="form.theme !== 'dark'" class="w-3 h-3 text-amber-500" />
                <Moon v-else class="w-3 h-3 text-indigo-500" />
              </span>
            </button>
          </div>
        </div>

        <!-- Data -->
        <div class="rounded-2xl p-6" style="background: var(--bg-secondary); border: 1px solid var(--border-color);">
          <h3 class="font-semibold mb-5 flex items-center gap-2" style="color: var(--text-primary)">
            <Database class="w-4 h-4" style="color: var(--accent)" />
            数据管理
          </h3>
          <div class="flex flex-wrap gap-3">
            <button @click="handleExportJson"
              class="flex items-center gap-2 px-4 py-2.5 text-sm font-medium rounded-xl border transition-all hover:bg-surface-100 dark:hover:bg-surface-800"
              style="border-color: var(--border-color); color: var(--text-secondary);"
            >
              <Download class="w-4 h-4" />
              导出 JSON
            </button>
            <button @click="handleExportMarkdown"
              class="flex items-center gap-2 px-4 py-2.5 text-sm font-medium rounded-xl border transition-all hover:bg-surface-100 dark:hover:bg-surface-800"
              style="border-color: var(--border-color); color: var(--text-secondary);"
            >
              <Download class="w-4 h-4" />
              导出 Markdown
            </button>
            <input ref="importFile" type="file" accept=".json" class="hidden" @change="handleImportFile">
            <button @click="handleImportClick"
              class="flex items-center gap-2 px-4 py-2.5 text-sm font-medium rounded-xl border transition-all hover:bg-surface-100 dark:hover:bg-surface-800"
              style="border-color: var(--border-color); color: var(--text-secondary);"
            >
              <Upload class="w-4 h-4" />
              导入数据
            </button>
          </div>
        </div>

        <button @click="handleSave" :disabled="loading"
          class="w-full py-3 bg-[#ea580c] hover:bg-[#c2410c] text-white font-medium rounded-xl transition-all shadow-lg shadow-[#ea580c]/20 disabled:opacity-50"
        >
          {{ loading ? '保存中...' : '保存设置' }}
        </button>
      </div>
    </div>

    <!-- AI Provider Modal -->
    <div v-if="showAiProviderModal"
      class="fixed inset-0 z-50 flex items-center justify-center p-4"
      style="background: rgba(0, 0, 0, 0.5);"
      @click.self="showAiProviderModal = false"
    >
      <div class="w-full max-w-lg rounded-2xl p-6 animate-fade-in"
        style="background: var(--bg-secondary); border: 1px solid var(--border-color);"
      >
        <div class="flex items-center justify-between mb-5">
          <h3 class="font-semibold text-lg" style="color: var(--text-primary)">
            {{ editingProvider ? '编辑AI配置' : '添加AI配置' }}
          </h3>
          <button @click="showAiProviderModal = false"
            class="p-2 rounded-lg transition-colors hover:bg-surface-100 dark:hover:bg-surface-800"
            style="color: var(--text-muted);"
          >
            <X class="w-5 h-5" />
          </button>
        </div>

        <div class="space-y-4">
          <div>
            <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">配置名称</label>
            <input v-model="aiProviderForm.name" type="text"
              class="w-full px-4 py-2.5 rounded-xl text-sm transition-all"
              style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
              placeholder="例如：我的OpenAI"
            >
          </div>

          <div>
            <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">提供商</label>
            <select v-model="aiProviderForm.provider" @change="onProviderChange"
              class="w-full px-4 py-2.5 rounded-xl text-sm transition-all"
              style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
            >
              <option v-for="opt in providerOptions" :key="opt.value" :value="opt.value">
                {{ opt.icon }} {{ opt.label }}
              </option>
            </select>
          </div>

          <div>
            <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">API Base URL</label>
            <input v-model="aiProviderForm.apiBaseUrl" type="text"
              class="w-full px-4 py-2.5 rounded-xl text-sm transition-all"
              style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
              placeholder="https://api.openai.com/v1"
            >
          </div>

          <div>
            <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">
              API Key
              <span v-if="editingProvider" class="text-[10px] ml-1" style="color: var(--text-muted)">(留空则保持不变)</span>
            </label>
            <div class="relative">
              <input v-model="aiProviderForm.apiKey" :type="showAiProviderApiKey ? 'text' : 'password'"
                class="w-full px-4 py-2.5 rounded-xl text-sm pr-10 transition-all"
                style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary); font-family: monospace;"
                placeholder="sk-xxxxxxxxxxxxxxxx"
              >
              <button @click="showAiProviderApiKey = !showAiProviderApiKey"
                class="absolute right-3 top-1/2 -translate-y-1/2"
                style="color: var(--text-muted);"
              >
                <Eye v-if="!showAiProviderApiKey" class="w-4 h-4" />
                <EyeOff v-else class="w-4 h-4" />
              </button>
            </div>
          </div>

          <div>
            <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">模型</label>
            <div class="flex gap-2">
              <select v-if="currentProvider?.models?.length" v-model="aiProviderForm.model"
                class="flex-1 px-4 py-2.5 rounded-xl text-sm transition-all"
                style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
              >
                <option v-for="m in currentProvider.models" :key="m" :value="m">{{ m }}</option>
              </select>
              <input v-model="aiProviderForm.model" type="text"
                class="flex-1 px-4 py-2.5 rounded-xl text-sm transition-all"
                :class="currentProvider?.models?.length ? 'hidden' : ''"
                style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
                placeholder="输入模型名称"
              >
            </div>
          </div>

          <div class="flex items-center gap-3">
            <input type="checkbox" id="isDefault" v-model="aiProviderForm.isDefault"
              class="w-4 h-4 rounded"
              style="accent-color: var(--accent);"
            >
            <label for="isDefault" class="text-sm" style="color: var(--text-primary)">设为默认配置</label>
          </div>
        </div>

        <div class="flex gap-3 mt-6">
          <button @click="showAiProviderModal = false"
            class="flex-1 py-2.5 font-medium rounded-xl border transition-all"
            style="border-color: var(--border-color); color: var(--text-secondary);"
          >
            取消
          </button>
          <button @click="handleSaveAiProvider" :disabled="aiProviderLoading"
            class="flex-1 py-2.5 font-medium rounded-xl transition-all"
            style="background: var(--accent); color: white;"
          >
            {{ aiProviderLoading ? '保存中...' : '保存' }}
          </button>
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

.animate-fade-in {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
</style>
