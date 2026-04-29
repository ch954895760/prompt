<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { useToastStore } from '@/stores/toast'
import MainLayout from '@/components/MainLayout.vue'
import DeleteConfirmDialog from '@/components/DeleteConfirmDialog.vue'
import AvatarUpload from '@/components/AvatarUpload.vue'
import { getSettings, updateSettings } from '@/api/setting'

import { updateUserAvatar, changePassword } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import type { UserSetting, AiProvider, AiProviderCreateRequest, AiProviderUpdateRequest } from '@/types'
import { getAiProviders, createAiProvider, updateAiProvider, deleteAiProvider, setDefaultAiProvider } from '@/api/aiProvider'
import { User, Palette, Eye, EyeOff, Sun, Moon, Plus, Edit2, Trash2, Check, X, Bot, Lock, KeyRound } from 'lucide-vue-next'
import ThemeTransition from '@/components/ThemeTransition.vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const userStore = useUserStore()
const toastStore = useToastStore()
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
const systemProviders = computed(() => aiProviders.value.filter(p => p.isSystem))
const userProviders = computed(() => aiProviders.value.filter(p => !p.isSystem))
const showAiProviderModal = ref(false)
const editingProvider = ref<AiProvider | null>(null)
const deletingProvider = ref<AiProvider | null>(null)
const showDeleteDialog = ref(false)
const aiProviderForm = ref({
  name: '',
  provider: 'kimi',
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
  { value: 'custom', label: '自定义', icon: '⚙️', models: [] },
  { value: 'kimi', label: 'Kimi', icon: '�', models: ['kimi-k2.5', 'kimi-k2.6'] },
  { value: 'minimax', label: 'MiniMax', icon: '🎭', models: ['MiniMax-M2.5', 'MiniMax-M2.7'] },
  { value: 'deepseek', label: 'DeepSeek', icon: '🐋', models: ['deepseek-v4-pro', 'deepseek-v4-flash'] },
  { value: 'google', label: 'Google Gemini', icon: '🔮', models: ['gemini-1.5-pro', 'gemini-1.5-flash', 'gemini-pro'] },
  { value: 'anthropic', label: 'Anthropic Claude', icon: '�', models: ['claude-3-opus-20240229', 'claude-3-sonnet-20240229', 'claude-3-haiku-20240307', 'claude-3-5-sonnet-20240620'] },
  { value: 'openai', label: 'OpenAI', icon: '⚡', models: ['gpt-4', 'gpt-4-turbo', 'gpt-4o', 'gpt-4o-mini', 'gpt-3.5-turbo'] },
]

const currentProvider = computed(() => {
  return providerOptions.find(p => p.value === aiProviderForm.value.provider)
})

const defaultBaseUrls = {
  openai: 'https://api.openai.com/v1',
  anthropic: 'https://api.anthropic.com',
  google: 'https://generativelanguage.googleapis.com',
  deepseek: 'https://api.deepseek.com',
  minimax: 'https://api.minimaxi.com/v1',
  kimi: 'https://api.moonshot.cn/v1',
  custom: '',
} as const

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
    toastStore.success(t('settings.avatarSuccess'))
  } catch (e: any) {
    toastStore.error(e.message || t('settings.avatarFailed'))
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
    toastStore.success(t('settings.saveSuccess'))
  } catch (e: any) {
    toastStore.error(e.message || t('settings.saveFailed'))
  } finally {
    loading.value = false
  }
}

// AI Provider Functions
function openAddAiProvider() {
  editingProvider.value = null
  aiProviderForm.value = {
    name: '',
    provider: 'kimi',
    apiBaseUrl: defaultBaseUrls.kimi,
    apiKey: '',
    model: 'kimi-k2.5',
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
  aiProviderForm.value.apiBaseUrl = (defaultBaseUrls as Record<string, string>)[provider] || ''
  const models = providerOptions.find(p => p.value === provider)?.models || []
  aiProviderForm.value.model = models[0] || ''
}

async function handleSaveAiProvider() {
  if (!aiProviderForm.value.name.trim()) {
    toastStore.warning(t('settings.providerNameRequired'))
    return
  }
  if (!aiProviderForm.value.apiBaseUrl.trim()) {
    toastStore.warning(t('settings.apiBaseUrlRequired'))
    return
  }
  if (!aiProviderForm.value.model.trim()) {
    toastStore.warning(t('settings.modelRequired'))
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
      toastStore.success(t('settings.providerUpdateSuccess'))
    } else {
      if (!aiProviderForm.value.apiKey) {
        toastStore.warning(t('settings.apiKeyRequired'))
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
      toastStore.success(t('settings.providerCreateSuccess'))
    }
    showAiProviderModal.value = false
    await loadAiProviders()
  } catch (e: any) {
    toastStore.error(e.message || t('settings.providerSaveFailed'))
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
    toastStore.success(t('settings.providerDeleteSuccess'))
    await loadAiProviders()
  } catch (e: any) {
    toastStore.error(e.message || t('settings.providerDeleteFailed'))
  } finally {
    deletingProvider.value = null
    showDeleteDialog.value = false
  }
}

async function handleSetDefault(provider: AiProvider) {
  if (provider.isDefault) return
  try {
    await setDefaultAiProvider(provider.id)
    toastStore.success(t('settings.setDefaultSuccess'))
    await loadAiProviders()
  } catch (e: any) {
    toastStore.error(e.message || t('settings.setDefaultFailed'))
  }
}

function getProviderLabel(providerValue: string) {
  return providerOptions.find(p => p.value === providerValue)?.label || providerValue
}

function getProviderIcon(providerValue: string) {
  return providerOptions.find(p => p.value === providerValue)?.icon || '🤖'
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
    toastStore.warning(t('settings.currentPasswordRequired'))
    return
  }
  if (!passwordForm.value.newPassword) {
    toastStore.warning(t('settings.newPasswordRequired'))
    return
  }
  if (passwordForm.value.newPassword.length < 6) {
    toastStore.warning(t('settings.passwordMinLength'))
    return
  }
  if (!passwordForm.value.confirmPassword) {
    toastStore.warning(t('settings.confirmPasswordRequired'))
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    toastStore.warning(t('settings.passwordMismatch'))
    return
  }

  passwordLoading.value = true
  try {
    await changePassword({
      currentPassword: passwordForm.value.currentPassword,
      newPassword: passwordForm.value.newPassword,
      confirmPassword: passwordForm.value.confirmPassword
    })
    toastStore.success(t('settings.passwordChangeSuccess'))
    closePasswordModal()
  } catch (e: any) {
    toastStore.error(e.message || t('settings.passwordChangeFailed'))
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
        <h2 class="text-2xl font-bold mb-1" style="color: var(--text-primary)">{{ t('nav.settings') }}</h2>
        <p class="text-sm" style="color: var(--text-secondary)">{{ t('settings.manageDescription') }}</p>
      </div>

      <div class="max-w-3xl space-y-6">
        <!-- Profile -->
        <div class="rounded-2xl p-6" style="background: var(--bg-secondary); border: 1px solid var(--border-color);">
          <h3 class="font-semibold mb-5 flex items-center gap-2" style="color: var(--text-primary)">
            <User class="w-4 h-4" style="color: var(--accent)" />
            {{ t('settings.profile') }}
          </h3>
          <div class="flex items-center gap-4 mb-5">
            <AvatarUpload v-model="avatarUrl" :username="form.username" @success="handleAvatarSuccess" />
          </div>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-5">
            <div>
              <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">{{ t('settings.username') }}</label>
              <input v-model="form.username" type="text" disabled
                class="w-full px-4 py-2.5 rounded-xl text-sm"
                style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-muted);"
              >
            </div>
            <div>
              <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">{{ t('settings.email') }}</label>
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
              {{ t('settings.changePassword') }}
            </button>
          </div>
        </div>

        <!-- AI Provider Management -->
        <div class="rounded-2xl p-6" style="background: var(--bg-secondary); border: 1px solid var(--border-color);">
          <div class="flex items-center justify-between mb-5">
            <h3 class="font-semibold flex items-center gap-2" style="color: var(--text-primary)">
              <Bot class="w-4 h-4" style="color: var(--accent)" />
              {{ t('settings.apiProvider') }}
            </h3>
            <button @click="openAddAiProvider"
              class="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-lg transition-colors"
              style="background: var(--accent); color: white;"
            >
              <Plus class="w-3.5 h-3.5" />
              {{ t('settings.addProvider') }}
            </button>
          </div>

          <!-- System AI Providers -->
          <div v-if="systemProviders.length > 0" class="mb-6">
            <h4 class="text-xs font-medium mb-3 uppercase tracking-wide" style="color: var(--text-secondary)">
              {{ t('settings.systemModels') }}
            </h4>
            <div class="space-y-2">
              <div v-for="provider in systemProviders" :key="provider.id"
                class="flex items-center gap-3 p-3 rounded-xl transition-all"
                :class="provider.isDefault ? 'ring-1' : ''"
                style="background: var(--bg-primary); border: 1px solid var(--border-color);"
                :style="provider.isDefault ? 'ring-color: var(--accent)' : ''"
              >
                <div class="w-9 h-9 rounded-lg flex items-center justify-center text-base"
                  style="background: var(--bg-secondary);"
                >
                  {{ getProviderIcon(provider.provider) }}
                </div>
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2">
                    <span class="font-medium text-sm truncate" style="color: var(--text-primary)">{{ provider.name }}</span>
                    <span class="text-[10px] px-1.5 py-0.5 rounded-full"
                      style="background: var(--bg-tertiary); color: var(--text-secondary);"
                    >
                      {{ t('settings.system') }}
                    </span>
                    <span v-if="provider.isDefault"
                      class="text-[10px] px-1.5 py-0.5 rounded-full"
                      style="background: var(--accent); color: white;"
                    >
                      {{ t('settings.default') }}
                    </span>
                  </div>
                  <div class="text-xs mt-0.5" style="color: var(--text-muted)">
                    {{ getProviderLabel(provider.provider) }} · {{ provider.model }}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- User AI Providers -->
          <div>
            <h4 v-if="systemProviders.length > 0" class="text-xs font-medium mb-3 uppercase tracking-wide" style="color: var(--text-secondary)">
              {{ t('settings.myModels') }}
            </h4>
            <div class="space-y-3">
              <div v-if="aiProviders.length === 0" class="text-center py-8 rounded-xl" style="background: var(--bg-primary); border: 1px dashed var(--border-color);">
                <Bot class="w-10 h-10 mx-auto mb-2" style="color: var(--text-muted)" />
                <p class="text-sm" style="color: var(--text-muted)">{{ t('settings.noAiProviders') }}</p>
                <p class="text-xs mt-1" style="color: var(--text-muted)">{{ t('settings.addFirstModel') }}</p>
              </div>

              <div v-for="provider in userProviders" :key="provider.id"
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
                      {{ t('settings.default') }}
                    </span>
                  </div>
                  <div class="text-xs mt-0.5" style="color: var(--text-muted)">
                    {{ getProviderLabel(provider.provider) }} · {{ provider.model }}
                  </div>
                </div>
                <div class="flex items-center gap-1">
                  <button v-if="!provider.isDefault" @click="handleSetDefault(provider)"
                    class="p-2 rounded-lg transition-colors hover:bg-[var(--bg-tertiary)]"
                    style="color: var(--text-muted);"
                    :title="t('settings.setDefault')"
                  >
                    <Check class="w-4 h-4" />
                  </button>
                  <button @click="openEditAiProvider(provider)"
                    class="p-2 rounded-lg transition-colors hover:bg-[var(--bg-tertiary)]"
                    style="color: var(--text-muted);"
                    :title="t('common.edit')"
                  >
                    <Edit2 class="w-4 h-4" />
                  </button>
                  <button @click="handleDeleteAiProvider(provider)"
                    class="p-2 rounded-lg transition-colors hover:bg-red-50 dark:hover:bg-red-900/20"
                    style="color: var(--text-muted);"
                    :title="t('common.delete')"
                  >
                    <Trash2 class="w-4 h-4 hover:text-red-500" />
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>


        <!-- Appearance -->
        <div class="rounded-2xl p-6" style="background: var(--bg-secondary); border: 1px solid var(--border-color);">
          <h3 class="font-semibold mb-5 flex items-center gap-2" style="color: var(--text-primary)">
            <Palette class="w-4 h-4" style="color: var(--accent)" />
            {{ t('settings.appearance') }}
          </h3>
          <div class="flex items-center justify-between">
            <div>
              <div class="text-sm font-medium mb-0.5" style="color: var(--text-primary)">{{ t('settings.darkMode') }}</div>
              <div class="text-xs" style="color: var(--text-muted)">{{ t('settings.darkModeDesc') }}</div>
            </div>
            <ThemeTransition :current-theme="form.theme" @toggle="toggleTheme">
              <template #default="{ trigger }">
                <button @click="trigger"
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
              </template>
            </ThemeTransition>
          </div>
        </div>

        <button @click="handleSave" :disabled="loading"
          class="w-full py-3 bg-[#ea580c] hover:bg-[#c2410c] text-white font-medium rounded-xl transition-all shadow-lg shadow-[#ea580c]/20 disabled:opacity-50"
        >
          {{ loading ? t('common.saving') : t('settings.saveSettings') }}
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
            {{ editingProvider ? t('settings.editProvider') : t('settings.addProvider') }}
          </h3>
          <button @click="showAiProviderModal = false"
            class="p-2 rounded-lg transition-colors hover:bg-[var(--bg-tertiary)]"
            style="color: var(--text-muted);"
          >
            <X class="w-5 h-5" />
          </button>
        </div>

        <div class="space-y-4">
          <div>
            <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">{{ t('settings.providerName') }}</label>
            <input v-model="aiProviderForm.name" type="text"
              class="w-full px-4 py-2.5 rounded-xl text-sm transition-all"
              style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
              :placeholder="t('settings.providerNamePlaceholder')"
            >
          </div>

          <div>
            <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">{{ t('settings.providerType') }}</label>
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
            <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">{{ t('settings.apiBaseUrl') }}</label>
            <input v-model="aiProviderForm.apiBaseUrl" type="text"
              class="w-full px-4 py-2.5 rounded-xl text-sm transition-all"
              style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
              placeholder="https://api.openai.com/v1"
            >
          </div>

          <div>
            <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">
              {{ t('settings.apiKey') }}
              <span v-if="editingProvider" class="text-[10px] ml-1" style="color: var(--text-muted)">({{ t('settings.leaveEmpty') }})</span>
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
            <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">{{ t('settings.model') }}</label>
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
                :placeholder="t('settings.modelPlaceholder')"
              >
            </div>
          </div>

          <div class="flex items-center gap-3">
            <input type="checkbox" id="isDefault" v-model="aiProviderForm.isDefault"
              class="w-4 h-4 rounded"
              style="accent-color: var(--accent);"
            >
            <label for="isDefault" class="text-sm" style="color: var(--text-primary)">{{ t('settings.setAsDefault') }}</label>
          </div>
        </div>

        <div class="flex gap-3 mt-6">
          <button @click="showAiProviderModal = false"
            class="flex-1 py-2.5 font-medium rounded-xl border transition-all"
            style="border-color: var(--border-color); color: var(--text-secondary);"
          >
            {{ t('common.cancel') }}
          </button>
          <button @click="handleSaveAiProvider" :disabled="aiProviderLoading"
            class="flex-1 py-2.5 font-medium rounded-xl transition-all"
            style="background: var(--accent); color: white;"
          >
            {{ aiProviderLoading ? t('common.saving') : t('common.save') }}
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
            {{ t('settings.changePassword') }}
          </h3>
          <button @click="closePasswordModal"
            class="p-2 rounded-lg transition-colors hover:bg-[var(--bg-tertiary)]"
            style="color: var(--text-muted);"
          >
            <X class="w-5 h-5" />
          </button>
        </div>

        <div class="space-y-4">
          <div>
            <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">{{ t('settings.currentPassword') }}</label>
            <div class="relative">
              <input v-model="passwordForm.currentPassword" :type="showCurrentPassword ? 'text' : 'password'"
                class="w-full px-4 py-2.5 rounded-xl text-sm pr-10 transition-all"
                style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
                :placeholder="t('settings.currentPasswordPlaceholder')"
                autocomplete="current-password"
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
            <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">{{ t('settings.newPassword') }}</label>
            <div class="relative">
              <input v-model="passwordForm.newPassword" :type="showNewPassword ? 'text' : 'password'"
                class="w-full px-4 py-2.5 rounded-xl text-sm pr-10 transition-all"
                style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
                :placeholder="t('settings.newPasswordPlaceholder')"
                autocomplete="new-password"
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
            <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">{{ t('settings.confirmPassword') }}</label>
            <div class="relative">
              <input v-model="passwordForm.confirmPassword" :type="showConfirmPassword ? 'text' : 'password'"
                class="w-full px-4 py-2.5 rounded-xl text-sm pr-10 transition-all"
                style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
                :placeholder="t('settings.confirmPasswordPlaceholder')"
                autocomplete="new-password"
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
            {{ t('common.cancel') }}
          </button>
          <button @click="handleChangePassword" :disabled="passwordLoading"
            class="flex-1 py-2.5 font-medium rounded-xl transition-all"
            style="background: var(--accent); color: white;"
          >
            {{ passwordLoading ? t('common.changing') : t('settings.confirmChange') }}
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
  </MainLayout>
</template>

<style scoped>
.animate-fade-in {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
</style>
