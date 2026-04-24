<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import { getSettings, updateSettings } from '@/api/setting'
import { exportPromptsJson, exportPromptsMarkdown, importPrompts } from '@/api/prompt'
import { useUserStore } from '@/stores/user'
import type { UserSetting } from '@/types'
import { User, Cpu, Palette, Database, Download, Upload, Eye, EyeOff, Sun, Moon } from 'lucide-vue-next'

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

async function loadData() {
  try {
    const s = await getSettings()
    setting.value = s
    // 如果服务器有设置主题，且本地没有手动设置过，则同步服务器主题
    if (s.theme && !localStorage.getItem('theme')) {
      userStore.setTheme(s.theme)
    }
    form.value.theme = userStore.theme
    form.value.defaultModel = s.defaultModel || ''
    form.value.apiBaseUrl = s.apiBaseUrl || ''
    form.value.model = s.model || ''
    if (userStore.user) {
      form.value.username = userStore.user.username
      form.value.email = userStore.user.email
    }
  } catch (e) {
    console.error('[DEBUG] Failed to load settings:', e)
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
      apiKey: form.value.apiKey,
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

onMounted(loadData)
</script>

<template>
  <MainLayout>
    <div class="animate-fade-in">
      <div class="mb-6">
        <h2 class="text-2xl font-bold mb-1" style="color: var(--text-primary)">设置</h2>
        <p class="text-sm" style="color: var(--text-secondary)">管理你的账户与应用偏好</p>
      </div>

      <div class="max-w-2xl space-y-6">
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

        <!-- AI Integration -->
        <div class="rounded-2xl p-6" style="background: var(--bg-secondary); border: 1px solid var(--border-color);">
          <h3 class="font-semibold mb-5 flex items-center gap-2" style="color: var(--text-primary)">
            <Cpu class="w-4 h-4" style="color: var(--accent)" />
            AI 集成
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
                @focus="($event.target as HTMLElement).style.borderColor = 'var(--accent)'"
                @blur="($event.target as HTMLElement).style.borderColor = 'var(--border-color)'"
              >
            </div>
            <div>
              <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">API Key</label>
              <div class="relative">
                <input v-model="form.apiKey" :type="showApiKey ? 'text' : 'password'"
                  class="w-full px-4 py-2.5 rounded-xl text-sm pr-10 transition-all"
                  style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary); font-family: monospace;"
                  placeholder="sk-xxxxxxxxxxxxxxxx"
                  @focus="($event.target as HTMLElement).style.borderColor = 'var(--accent)'"
                  @blur="($event.target as HTMLElement).style.borderColor = 'var(--border-color)'"
                >
                <button @click="showApiKey = !showApiKey"
                  class="absolute right-3 top-1/2 -translate-y-1/2"
                  style="color: var(--text-muted);"
                >
                  <Eye v-if="!showApiKey" class="w-4 h-4" />
                  <EyeOff v-else class="w-4 h-4" />
                </button>
              </div>
              <p class="text-[10px] mt-1.5" style="color: var(--text-muted)">你的 API Key 将加密存储，仅用于代理请求</p>
            </div>
            <div>
              <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">Model</label>
              <input v-model="form.model" type="text"
                class="w-full px-4 py-2.5 rounded-xl text-sm transition-all"
                style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
                placeholder="gpt-4, claude-3-5-sonnet 等"
                @focus="($event.target as HTMLElement).style.borderColor = 'var(--accent)'"
                @blur="($event.target as HTMLElement).style.borderColor = 'var(--border-color)'"
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
