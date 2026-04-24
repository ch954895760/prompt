<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import DeleteConfirmDialog from '@/components/DeleteConfirmDialog.vue'
import AvatarUpload from '@/components/AvatarUpload.vue'
import { getSettings, updateSettings } from '@/api/setting'

import { updateUserAvatar, changePassword } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import type { UserSetting, AiProvider, AiProviderCreateRequest, AiProviderUpdateRequest } from '@/types'
import { getAiProviders, createAiProvider, updateAiProvider, deleteAiProvider, setDefaultAiProvider } from '@/api/aiProvider'
import { User, Palette, Eye, EyeOff, Sun, Moon, Plus, Edit2, Trash2, Check, X, Bot, Lock, KeyRound } from 'lucide-vue-next'

const userStore = useUserStore()
const setting = ref<UserSetting | null>(null)
const loading = ref(false)

const showApiKey = ref(false)
const avatarUrl = ref('')
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
const deletingProvider = ref<AiProvider | null>(null)
const showDeleteDialog = ref(false)
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

// Password Change
const showPasswordModal = ref(false)
const passwordLoading = ref(false)
const showCurrentPassword = ref(false)
const showNewPassword = ref(false)
const showConfirmPassword = ref(false)
const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

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
      avatarUrl.value = userStore.user.avatar || ''
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

async function handleAvatarSuccess(url: string) {
  avatarUrl.value = url
  try {
    await updateUserAvatar(url)
    if (userStore.user) {
      userStore.user.avatar = url
    }
    showToast('头像更新成功')
  } catch (e: any) {
    showToast(e.message || '头像保存失败')
  }
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

function handleDeleteAiProvider(provider: AiProvider) {
  deletingProvider.value = provider
  showDeleteDialog.value = true
}

async function confirmDeleteAiProvider() {
  if (!deletingProvider.value) return
  try {
    await deleteAiProvider(deletingProvider.value.id)
    showToast('AI配置已删除')
    await loadAiProviders()
  } catch (e: any) {
    showToast(e.message || '删除失败')
  } finally {
    deletingProvider.value = null
    showDeleteDialog.value = false
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

// Password Change Functions
function openPasswordModal() {
  passwordForm.value = {
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  }
  showCurrentPassword.value = false
  showNewPassword.value = false
  showConfirmPassword.value = false
  showPasswordModal.value = true
}

function closePasswordModal() {
  showPasswordModal.value = false
  passwordForm.value = {
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  }
}

async function handleChangePassword() {
  // 表单验证
  if (!passwordForm.value.currentPassword) {
    showToast('请输入当前密码')
    return
  }
  if (!passwordForm.value.newPassword) {
    showToast('请输入新密码')
    return
  }
  if (passwordForm.value.newPassword.length < 6) {
    showToast('新密码长度不能少于6位')
    return
  }
  if (!passwordForm.value.confirmPassword) {
    showToast('请确认新密码')
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    showToast('两次输入的新密码不一致')
    return
  }

  passwordLoading.value = true
  try {
    await changePassword({
      currentPassword: passwordForm.value.currentPassword,
      newPassword: passwordForm.value.newPassword,
      confirmPassword: passwordForm.value.confirmPassword
    })
    showToast('密码修改成功')
    closePasswordModal()
  } catch (e: any) {
    showToast(e.message || '密码修改失败')
  } finally {
    passwordLoading.value = false
  }
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
            <AvatarUpload v-model="avatarUrl" :username="form.username" @success="handleAvatarSuccess" />
          </div>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-5">
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
          <div class="pt-4 border-t" style="border-color: var(--border-color);">
            <button @click="openPasswordModal"
              class="flex items-center gap-2 px-4 py-2.5 text-sm font-medium rounded-xl border transition-all"
              style="border-color: var(--border-color); color: var(--text-secondary);"
              @mouseenter="($event.currentTarget as HTMLElement).style.background = 'var(--bg-tertiary)'"
              @mouseleave="($event.currentTarget as HTMLElement).style.background = 'transparent'"
            >
              <KeyRound class="w-4 h-4" />
              修改密码
            </button>
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

    <!-- Password Change Modal -->
    <div v-if="showPasswordModal"
      class="fixed inset-0 z-50 flex items-center justify-center p-4"
      @click.self="closePasswordModal"
    >
      <!-- Backdrop -->
      <div class="absolute inset-0 bg-black/40 backdrop-blur-sm transition-opacity"></div>

      <div class="relative w-full max-w-md rounded-2xl p-6 animate-fade-in"
        style="background: var(--bg-secondary); border: 1px solid var(--border-color); box-shadow: 0 24px 80px rgba(0,0,0,0.2);"
      >
        <div class="flex items-center justify-between mb-5">
          <h3 class="font-semibold text-lg flex items-center gap-2" style="color: var(--text-primary)">
            <Lock class="w-5 h-5" style="color: var(--accent)" />
            修改密码
          </h3>
          <button @click="closePasswordModal"
            class="p-2 rounded-lg transition-colors hover:bg-surface-100 dark:hover:bg-surface-800"
            style="color: var(--text-muted);"
          >
            <X class="w-5 h-5" />
          </button>
        </div>

        <div class="space-y-4">
          <div>
            <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">当前密码</label>
            <div class="relative">
              <input v-model="passwordForm.currentPassword" :type="showCurrentPassword ? 'text' : 'password'"
                class="w-full px-4 py-2.5 rounded-xl text-sm pr-10 transition-all"
                style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
                placeholder="请输入当前密码"
              >
              <button @click="showCurrentPassword = !showCurrentPassword"
                class="absolute right-3 top-1/2 -translate-y-1/2"
                style="color: var(--text-muted);"
              >
                <Eye v-if="!showCurrentPassword" class="w-4 h-4" />
                <EyeOff v-else class="w-4 h-4" />
              </button>
            </div>
          </div>

          <div>
            <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">新密码</label>
            <div class="relative">
              <input v-model="passwordForm.newPassword" :type="showNewPassword ? 'text' : 'password'"
                class="w-full px-4 py-2.5 rounded-xl text-sm pr-10 transition-all"
                style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
                placeholder="请输入新密码（至少6位）"
              >
              <button @click="showNewPassword = !showNewPassword"
                class="absolute right-3 top-1/2 -translate-y-1/2"
                style="color: var(--text-muted);"
              >
                <Eye v-if="!showNewPassword" class="w-4 h-4" />
                <EyeOff v-else class="w-4 h-4" />
              </button>
            </div>
          </div>

          <div>
            <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">确认新密码</label>
            <div class="relative">
              <input v-model="passwordForm.confirmPassword" :type="showConfirmPassword ? 'text' : 'password'"
                class="w-full px-4 py-2.5 rounded-xl text-sm pr-10 transition-all"
                style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
                placeholder="请再次输入新密码"
              >
              <button @click="showConfirmPassword = !showConfirmPassword"
                class="absolute right-3 top-1/2 -translate-y-1/2"
                style="color: var(--text-muted);"
              >
                <Eye v-if="!showConfirmPassword" class="w-4 h-4" />
                <EyeOff v-else class="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>

        <div class="flex gap-3 mt-6">
          <button @click="closePasswordModal"
            class="flex-1 py-2.5 font-medium rounded-xl border transition-all"
            style="border-color: var(--border-color); color: var(--text-secondary);"
          >
            取消
          </button>
          <button @click="handleChangePassword" :disabled="passwordLoading"
            class="flex-1 py-2.5 font-medium rounded-xl transition-all"
            style="background: var(--accent); color: white;"
          >
            {{ passwordLoading ? '修改中...' : '确认修改' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Delete Confirm Dialog -->
    <DeleteConfirmDialog
      v-model="showDeleteDialog"
      :item-name="deletingProvider?.name"
      @confirm="confirmDeleteAiProvider"
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

.animate-fade-in {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
</style>
