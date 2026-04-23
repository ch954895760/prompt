<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import MainLayout from '@/components/MainLayout.vue'
import { getPromptList, usePrompt } from '@/api/prompt'
import { getCategoryList } from '@/api/category'
import type { Prompt, Category } from '@/types'
import { FileText, FolderOpen, Zap, Copy, ChevronRight } from 'lucide-vue-next'

const router = useRouter()
const userStore = useUserStore()

const prompts = ref<Prompt[]>([])
const categories = ref<Category[]>([])
const loading = ref(false)

const stats = computed(() => ({
  total: prompts.value.length,
  categories: categories.value.length,
  weeklyUsage: prompts.value.reduce((sum, p) => sum + (p.usageCount || 0), 0),
}))

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

async function copyPrompt(content: string, title: string, id: number) {
  await navigator.clipboard.writeText(content)
  await usePrompt(id)
  showToast(`"${title}" 已复制`)
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

function navigate(path: string) {
  router.push(path)
}

onMounted(loadData)
</script>

<template>
  <MainLayout>
    <div class="animate-fade-in">
      <div class="mb-8">
        <h2 class="text-2xl font-bold mb-1" style="color: var(--text-primary)">
          早上好, {{ userStore.user?.username || '用户' }}
        </h2>
        <p class="text-sm" style="color: var(--text-secondary)">今天准备创作什么?</p>
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
          <div class="text-xs" style="color: var(--text-muted)">提示词总数</div>
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
          <div class="text-xs" style="color: var(--text-muted)">分类数量</div>
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
          <div class="text-xs" style="color: var(--text-muted)">累计使用次数</div>
        </div>
      </div>

      <!-- Recent prompts -->
      <div class="rounded-2xl p-6" style="background: var(--bg-secondary); border: 1px solid var(--border-color);">
        <div class="flex items-center justify-between mb-5">
          <h3 class="font-semibold" style="color: var(--text-primary)">最近编辑</h3>
          <a href="#" @click.prevent="navigate('/prompts')" class="text-xs font-medium hover:underline" style="color: var(--accent)">查看全部</a>
        </div>
        <div v-if="loading" class="py-8 text-center text-sm" style="color: var(--text-muted)">加载中...</div>
        <div v-else-if="recentPrompts.length === 0" class="py-8 text-center text-sm" style="color: var(--text-muted)">暂无提示词</div>
        <div v-else class="space-y-3">
          <div v-for="p in recentPrompts" :key="p.id"
            class="flex items-center gap-4 p-3 rounded-xl transition-colors cursor-pointer group"
            style="border: 1px solid transparent;"
            @mouseenter="($event.currentTarget as HTMLElement).style.borderColor = 'var(--border-color)'"
            @mouseleave="($event.currentTarget as HTMLElement).style.borderColor = 'transparent'"
            @click="navigate(`/editor/${p.id}`)"
          >
            <div class="w-2 h-2 rounded-full flex-shrink-0" :style="{ background: p.categoryName ? '#ea580c' : '#d6d3d1' }"></div>
            <div class="flex-1 min-w-0">
              <div class="text-sm font-medium truncate transition-colors group-hover:text-[#ea580c] dark:group-hover:text-[#fb923c]" style="color: var(--text-primary)">{{ p.title }}</div>
              <div class="text-xs truncate" style="color: var(--text-muted)">{{ p.categoryName || '未分类' }} · {{ p.updatedAt }}</div>
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
    </div>

    <!-- Toast -->
    <div v-if="toastVisible"
      class="fixed bottom-6 right-6 px-5 py-3 rounded-xl flex items-center gap-2.5 z-50 transition-all"
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
