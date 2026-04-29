<script setup lang="ts">
import { ref, onMounted, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useToastStore } from '@/stores/toast'
import { useI18n } from 'vue-i18n'
import MainLayout from '@/components/MainLayout.vue'
import { getPromptList, usePrompt, getRecentlyUsedPrompts } from '@/api/prompt'
import { getCategoryList } from '@/api/category'
import type { Prompt, Category } from '@/types'
import { FileText, FolderOpen, Zap, Copy, ChevronRight, Clock } from 'lucide-vue-next'

const router = useRouter()
const userStore = useUserStore()
const toastStore = useToastStore()
const { t, locale } = useI18n()

const prompts = ref<Prompt[]>([])
const categories = ref<Category[]>([])
const recentlyUsedPrompts = ref<Prompt[]>([])
const loading = ref(false)
const loadingRecentlyUsed = ref(false)
const currentTime = ref(new Date())
let timeTimer: ReturnType<typeof setInterval> | null = null

const stats = computed(() => ({
  total: prompts.value.length,
  categories: categories.value.length,
  weeklyUsage: prompts.value.reduce((sum, p) => sum + (p.usageCount || 0), 0),
}))

// 根据当前时间返回问候语
const greeting = computed(() => {
  const hour = currentTime.value.getHours()
  if (hour < 6) return t('dashboard.greeting.night')
  if (hour < 9) return t('dashboard.greeting.earlyMorning')
  if (hour < 12) return t('dashboard.greeting.morning')
  if (hour < 14) return t('dashboard.greeting.noon')
  if (hour < 18) return t('dashboard.greeting.afternoon')
  return t('dashboard.greeting.evening')
})

// 格式化当前时间
const formattedTime = computed(() => {
  const date = currentTime.value
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${hours}:${minutes}:${seconds}`
})

// 格式化当前日期
const formattedDate = computed(() => {
  const date = currentTime.value
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const weekDays = locale.value === 'zh-CN'
    ? ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
    : ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']
  const weekDay = weekDays[date.getDay()]
  return locale.value === 'zh-CN'
    ? `${year}年${month}月${day}日 ${weekDay}`
    : `${weekDay}, ${year}-${month}-${day}`
})

const recentPrompts = computed(() => prompts.value.slice(0, 4))

async function loadData() {
  loading.value = true
  try {
    const [pList, cList] = await Promise.all([
      getPromptList(),
      getCategoryList(),
    ])
    prompts.value = pList
    categories.value = cList
  } catch (e) {
    console.error('[DEBUG] Failed to load dashboard data:', e)
  } finally {
    loading.value = false
  }
}

async function loadRecentlyUsed() {
  loadingRecentlyUsed.value = true
  try {
    recentlyUsedPrompts.value = await getRecentlyUsedPrompts(5)
  } catch (e) {
    console.error('[DEBUG] Failed to load recently used prompts:', e)
  } finally {
    loadingRecentlyUsed.value = false
  }
}

async function copyPrompt(content: string, title: string, id: number) {
  try {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      await navigator.clipboard.writeText(content)
    } else {
      const textarea = document.createElement('textarea')
      textarea.value = content
      textarea.style.position = 'fixed'
      textarea.style.left = '-9999px'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
    }
    await usePrompt(id)
    toastStore.success(t('common.copied') + `: "${title}"`)
  } catch (e) {
    toastStore.error(t('common.copyFailed'))
  }
}

function navigate(path: string) {
  router.push(path)
}

async function copyRecentlyUsed(content: string, title: string, id: number) {
  try {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      await navigator.clipboard.writeText(content)
    } else {
      const textarea = document.createElement('textarea')
      textarea.value = content
      textarea.style.position = 'fixed'
      textarea.style.left = '-9999px'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
    }
    await usePrompt(id, t('dashboard.fromRecentCopy'))
    toastStore.success(t('common.copied') + `: "${title}"`)
  } catch (e) {
    toastStore.error(t('common.copyFailed'))
  }
}

onMounted(() => {
  loadData()
  loadRecentlyUsed()
  // 启动时间定时器，每秒更新
  timeTimer = setInterval(() => {
    currentTime.value = new Date()
  }, 1000)
})

onUnmounted(() => {
  // 清除时间定时器
  if (timeTimer) {
    clearInterval(timeTimer)
    timeTimer = null
  }
})
</script>

<template>
  <MainLayout>
    <div class="animate-fade-in">
      <div class="mb-8 flex items-start justify-between">
        <div>
          <h2 class="text-2xl font-bold mb-1" style="color: var(--text-primary)">
            {{ greeting }}, {{ userStore.user?.username || t('user.user') }}
          </h2>
          <p class="text-sm" style="color: var(--text-secondary)">{{ t('dashboard.whatToCreate') }}</p>
        </div>
        <div class="text-right">
          <div class="text-3xl font-mono font-bold" style="color: var(--accent)">{{ formattedTime }}</div>
          <div class="text-xs mt-1" style="color: var(--text-muted)">{{ formattedDate }}</div>
        </div>
      </div>

      <!-- Stats cards -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-5 mb-8">
        <div class="rounded-2xl p-5 relative overflow-hidden transition-all hover:-translate-y-0.5"
          style="background: var(--bg-secondary); border: 1px solid var(--border-color);"
          @mouseenter="($event.currentTarget as HTMLElement).style.borderColor = 'var(--accent)'"
          @mouseleave="($event.currentTarget as HTMLElement).style.borderColor = 'var(--border-color)'"
        >
          <div class="absolute top-0 right-0 w-24 h-24 bg-gradient-to-br from-[#fb923c]/10 to-transparent rounded-bl-full"></div>
          <div class="flex items-start justify-between mb-3">
            <div class="w-10 h-10 rounded-xl flex items-center justify-center" style="background: var(--accent-soft)">
              <FileText class="w-5 h-5" style="color: var(--accent)" />
            </div>
            <span class="text-[10px] font-medium px-2 py-1 rounded-full bg-emerald-50 text-emerald-600 dark:bg-emerald-900/30 dark:text-emerald-400">+12%</span>
          </div>
          <div class="text-2xl font-bold mb-0.5" style="color: var(--text-primary)">{{ stats.total }}</div>
          <div class="text-xs" style="color: var(--text-muted)">{{ t('dashboard.totalPrompts') }}</div>
        </div>

        <div class="rounded-2xl p-5 relative overflow-hidden transition-all hover:-translate-y-0.5"
          style="background: var(--bg-secondary); border: 1px solid var(--border-color);"
          @mouseenter="($event.currentTarget as HTMLElement).style.borderColor = '#3b82f6'"
          @mouseleave="($event.currentTarget as HTMLElement).style.borderColor = 'var(--border-color)'"
        >
          <div class="absolute top-0 right-0 w-24 h-24 bg-gradient-to-br from-blue-400/10 to-transparent rounded-bl-full"></div>
          <div class="flex items-start justify-between mb-3">
            <div class="w-10 h-10 rounded-xl bg-blue-50 dark:bg-blue-900/30 flex items-center justify-center">
              <FolderOpen class="w-5 h-5 text-blue-600 dark:text-blue-400" />
            </div>
            <span class="text-[10px] font-medium px-2 py-1 rounded-full bg-emerald-50 text-emerald-600 dark:bg-emerald-900/30 dark:text-emerald-400">+3</span>
          </div>
          <div class="text-2xl font-bold mb-0.5" style="color: var(--text-primary)">{{ stats.categories }}</div>
          <div class="text-xs" style="color: var(--text-muted)">{{ t('dashboard.categoryCount') }}</div>
        </div>

        <div class="rounded-2xl p-5 relative overflow-hidden transition-all hover:-translate-y-0.5"
          style="background: var(--bg-secondary); border: 1px solid var(--border-color);"
          @mouseenter="($event.currentTarget as HTMLElement).style.borderColor = '#8b5cf6'"
          @mouseleave="($event.currentTarget as HTMLElement).style.borderColor = 'var(--border-color)'"
        >
          <div class="absolute top-0 right-0 w-24 h-24 bg-gradient-to-br from-purple-400/10 to-transparent rounded-bl-full"></div>
          <div class="flex items-start justify-between mb-3">
            <div class="w-10 h-10 rounded-xl bg-purple-50 dark:bg-purple-900/30 flex items-center justify-center">
              <Zap class="w-5 h-5 text-purple-600 dark:text-purple-400" />
            </div>
            <span class="text-[10px] font-medium px-2 py-1 rounded-full bg-emerald-50 text-emerald-600 dark:bg-emerald-900/30 dark:text-emerald-400">+28%</span>
          </div>
          <div class="text-2xl font-bold mb-0.5" style="color: var(--text-primary)">{{ stats.weeklyUsage }}</div>
          <div class="text-xs" style="color: var(--text-muted)">{{ t('dashboard.totalUsage') }}</div>
        </div>
      </div>

      <!-- Two column layout -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- Recently edited -->
        <div class="rounded-2xl p-6" style="background: var(--bg-secondary); border: 1px solid var(--border-color);">
          <div class="flex items-center justify-between mb-5">
            <h3 class="font-semibold" style="color: var(--text-primary)">{{ t('dashboard.recentlyEdited') }}</h3>
            <a href="#" @click.prevent="navigate('/prompts')" class="text-xs font-medium hover:underline" style="color: var(--accent)">{{ t('dashboard.viewAll') }}</a>
          </div>
          <div v-if="loading" class="py-8 text-center text-sm" style="color: var(--text-muted)">{{ t('common.loading') }}</div>
          <div v-else-if="recentPrompts.length === 0" class="py-8 text-center text-sm" style="color: var(--text-muted)">{{ t('dashboard.noPrompts') }}</div>
          <div v-else class="space-y-3">
            <div v-for="p in recentPrompts" :key="p.id"
              class="flex items-center gap-4 p-3 rounded-xl transition-colors cursor-pointer group"
              style="border: 1px solid transparent;"
              @mouseenter="($event.currentTarget as HTMLElement).style.borderColor = 'var(--border-color)'"
              @mouseleave="($event.currentTarget as HTMLElement).style.borderColor = 'transparent'"
              @click="navigate(`/editor/${p.id}`)"
            >
              <div class="w-2 h-2 rounded-full flex-shrink-0" :style="{ background: p.categoryColor || '#d6d3d1' }"></div>
              <div class="flex-1 min-w-0">
                <div class="text-sm font-medium truncate transition-colors group-hover:opacity-80" :style="{ color: p.categoryColor || 'var(--text-primary)' }">{{ p.title }}</div>
                <div class="text-xs truncate" style="color: var(--text-muted)">{{ p.categoryName || t('prompt.uncategorized') }} · {{ p.updatedAt }}</div>
              </div>
              <button @click.stop="copyPrompt(p.content, p.title, p.id)"
                class="opacity-0 group-hover:opacity-100 transition-opacity p-2 rounded-lg"
                style="color: var(--text-muted);"
                @mouseenter="($event.currentTarget as HTMLElement).style.background = 'var(--bg-tertiary)'"
                @mouseleave="($event.currentTarget as HTMLElement).style.background = 'transparent'"
              >
                <Copy class="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        </div>

        <!-- Recently used -->
        <div class="rounded-2xl p-6" style="background: var(--bg-secondary); border: 1px solid var(--border-color);">
          <div class="flex items-center justify-between mb-5">
            <h3 class="font-semibold flex items-center gap-2" style="color: var(--text-primary)">
              <Clock class="w-4 h-4" style="color: var(--accent)" />
              {{ t('dashboard.recentlyUsed') }}
            </h3>
            <a href="#" @click.prevent="navigate('/prompts')" class="text-xs font-medium hover:underline" style="color: var(--accent)">{{ t('dashboard.viewAll') }}</a>
          </div>
          <div v-if="loadingRecentlyUsed" class="py-8 text-center text-sm" style="color: var(--text-muted)">{{ t('common.loading') }}</div>
          <div v-else-if="recentlyUsedPrompts.length === 0" class="py-8 text-center text-sm" style="color: var(--text-muted)">{{ t('dashboard.noUsage') }}</div>
          <div v-else class="space-y-3">
            <div v-for="p in recentlyUsedPrompts" :key="p.id"
              class="flex items-center gap-4 p-3 rounded-xl transition-colors cursor-pointer group"
              style="border: 1px solid transparent;"
              @mouseenter="($event.currentTarget as HTMLElement).style.borderColor = 'var(--border-color)'"
              @mouseleave="($event.currentTarget as HTMLElement).style.borderColor = 'transparent'"
              @click="navigate(`/editor/${p.id}`)"
            >
              <div class="w-2 h-2 rounded-full flex-shrink-0" :style="{ background: p.categoryColor || '#d6d3d1' }"></div>
              <div class="flex-1 min-w-0">
                <div class="text-sm font-medium truncate transition-colors group-hover:opacity-80" :style="{ color: p.categoryColor || 'var(--text-primary)' }">{{ p.title }}</div>
                <div class="text-xs truncate" style="color: var(--text-muted)">{{ p.categoryName || t('prompt.uncategorized') }} · {{ t('prompt.usedCount', { count: p.usageCount || 0 }) }}</div>
              </div>
              <button @click.stop="copyRecentlyUsed(p.content, p.title, p.id)"
                class="opacity-0 group-hover:opacity-100 transition-opacity p-2 rounded-lg"
                style="color: var(--text-muted);"
                @mouseenter="($event.currentTarget as HTMLElement).style.background = 'var(--bg-tertiary)'"
                @mouseleave="($event.currentTarget as HTMLElement).style.background = 'transparent'"
              >
                <Copy class="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Toast -->
  </MainLayout>
</template>
