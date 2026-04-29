<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { debounce } from '@/utils/debounce'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getLocale } from '@/i18n'
import { useLanguageTransition } from '@/composables/useLanguageTransition'
import {
  Sparkles, LayoutDashboard, FileText, PenTool,
  FolderTree, Tag, Settings, Menu, X, LogOut,
  Search, Sun, Moon, Bell, ChevronLeft, ChevronRight,
  Languages
} from 'lucide-vue-next'
import DeleteConfirmDialog from '@/components/DeleteConfirmDialog.vue'
import ThemeTransition from '@/components/ThemeTransition.vue'

const { t } = useI18n()

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const sidebarOpen = ref(false)
const sidebarCollapsed = ref(false)
const searchQuery = ref('')

// 组件挂载时从 localStorage 读取折叠状态，并从 URL 读取搜索关键词
onMounted(() => {
  const stored = localStorage.getItem('sidebarCollapsed')
  if (stored !== null) {
    sidebarCollapsed.value = stored === 'true'
  }
  // 从 URL 参数初始化搜索框内容
  if (route.query.q) {
    searchQuery.value = route.query.q as string
  }
})

// 监听折叠状态变化，保存到 localStorage
watch(sidebarCollapsed, (newValue) => {
  localStorage.setItem('sidebarCollapsed', String(newValue))
})

function toggleTheme() {
  userStore.toggleTheme()
}

function checkTheme() {
  document.documentElement.classList.toggle('dark', userStore.theme === 'dark')
}
checkTheme()

function toggleSidebar() {
  sidebarOpen.value = !sidebarOpen.value
}

function toggleSidebarCollapse() {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

function navigate(path: string) {
  router.push(path)
  if (window.innerWidth < 1024) {
    sidebarOpen.value = false
  }
}

const logoutDialogVisible = ref(false)

function handleLogout() {
  logoutDialogVisible.value = true
}

function confirmLogout() {
  userStore.logout()
  router.push('/login')
}

const handleSearch = debounce(() => {
  if (searchQuery.value.trim()) {
    router.push({ path: '/prompts', query: { q: searchQuery.value } })
  } else {
    // 如果搜索框为空，清除查询参数
    router.push({ path: '/prompts' })
  }
}, 300)

// 监听搜索输入变化，实时查询
watch(searchQuery, () => {
  handleSearch()
})

const currentLocale = ref(getLocale())
const { toggleLocaleWithTransition } = useLanguageTransition()

async function handleToggleLocale() {
  const newLocale = await toggleLocaleWithTransition()
  currentLocale.value = newLocale
}

const currentRoute = computed(() => route.path)
const navItems = computed(() => [
  { path: '/dashboard', label: t('nav.dashboard'), icon: LayoutDashboard },
  { path: '/prompts', label: t('nav.promptLibrary'), icon: FileText },
  { path: '/editor', label: t('nav.newPrompt'), icon: PenTool },
])
const manageItems = computed(() => [
  { path: '/categories', label: t('nav.categoryManage'), icon: FolderTree },
  { path: '/tags', label: t('nav.tagManage'), icon: Tag },
  { path: '/settings', label: t('nav.settings'), icon: Settings },
])

watch(() => route.query.q, (q) => {
  if (q) searchQuery.value = q as string
})
</script>

<template>
  <div class="min-h-screen flex">
    <!-- Mobile sidebar overlay -->
    <div class="fixed inset-0 bg-black/30 z-35 lg:hidden transition-opacity"
      :class="sidebarOpen ? 'opacity-100 pointer-events-auto' : 'opacity-0 pointer-events-none'"
      @click="toggleSidebar"
    ></div>

    <!-- Sidebar -->
    <aside class="sidebar flex-shrink-0 flex flex-col h-screen sticky top-0 z-40 transition-all duration-300 lg:translate-x-0"
      :class="[
        sidebarOpen ? 'translate-x-0' : '-translate-x-full',
        sidebarCollapsed ? 'w-16' : 'w-64'
      ]"
      style="background: var(--bg-secondary); border-right: 1px solid var(--border-color);"
    >
      <!-- Logo -->
      <div class="p-5 flex items-center gap-3" :class="sidebarCollapsed ? 'justify-center' : ''">
        <div class="w-9 h-9 rounded-xl bg-[#ea580c] text-white flex items-center justify-center shadow-md shadow-[#ea580c]/20 flex-shrink-0">
          <Sparkles class="w-5 h-5" />
        </div>
        <span v-if="!sidebarCollapsed" class="font-bold text-lg tracking-tight truncate" style="color: var(--text-primary)">Prompt Vault</span>
        <button v-if="!sidebarCollapsed" class="lg:hidden ml-auto" @click="toggleSidebar">
          <X class="w-5 h-5" style="color: var(--text-secondary)" />
        </button>
      </div>

      <!-- Collapse Toggle Button (Desktop) -->
      <button
        @click="toggleSidebarCollapse"
        class="hidden lg:flex items-center justify-center py-2 mx-3 mb-2 rounded-lg transition-colors hover:bg-[var(--bg-tertiary)]"
        style="color: var(--text-muted)"
        :title="sidebarCollapsed ? t('sidebar.expand') : t('sidebar.collapse')"
      >
        <ChevronLeft v-if="!sidebarCollapsed" class="w-4 h-4" />
        <ChevronRight v-else class="w-4 h-4" />
      </button>

      <!-- Navigation -->
      <nav class="flex-1 px-3 space-y-1 overflow-y-auto">
        <div v-if="!sidebarCollapsed" class="text-[10px] font-semibold uppercase tracking-wider px-3 py-2" style="color: var(--text-muted)">{{ t('nav.workspace') }}</div>
        <a v-for="item in navItems" :key="item.path" href="#"
          class="nav-item flex items-center text-sm font-medium"
          :class="[
            currentRoute === item.path ? 'active' : '',
            sidebarCollapsed ? 'justify-center px-2 py-3' : 'gap-3 px-3 py-2.5'
          ]"
          :style="currentRoute === item.path ? '' : 'color: var(--text-secondary)'"
          @click.prevent="navigate(item.path)"
          :title="sidebarCollapsed ? item.label : ''"
        >
          <component :is="item.icon" class="w-4.5 h-4.5 flex-shrink-0" />
          <span v-if="!sidebarCollapsed" class="truncate">{{ item.label }}</span>
        </a>

        <div v-if="!sidebarCollapsed" class="text-[10px] font-semibold uppercase tracking-wider px-3 py-2 mt-4" style="color: var(--text-muted)">{{ t('nav.management') }}</div>
        <a v-for="item in manageItems" :key="item.path" href="#"
          class="nav-item flex items-center text-sm font-medium"
          :class="[
            currentRoute === item.path ? 'active' : '',
            sidebarCollapsed ? 'justify-center px-2 py-3' : 'gap-3 px-3 py-2.5'
          ]"
          :style="currentRoute === item.path ? '' : 'color: var(--text-secondary)'"
          @click.prevent="navigate(item.path)"
          :title="sidebarCollapsed ? item.label : ''"
        >
          <component :is="item.icon" class="w-4.5 h-4.5 flex-shrink-0" />
          <span v-if="!sidebarCollapsed" class="truncate">{{ item.label }}</span>
        </a>
      </nav>

      <!-- User section -->
      <div v-if="!sidebarCollapsed" class="p-4 mx-3 mb-3 rounded-xl" style="background: var(--bg-tertiary)">
        <div class="flex items-center gap-3 mb-3">
          <img
            v-if="userStore.user?.avatar"
            :src="`/api${userStore.user.avatar}`"
            class="w-9 h-9 rounded-full object-cover"
            alt="avatar"
          />
          <div
            v-else
            class="w-9 h-9 rounded-full bg-gradient-to-br from-[#fb923c] to-[#ea580c] text-white flex items-center justify-center text-sm font-bold"
          >
            {{ userStore.user?.username?.charAt(0)?.toUpperCase() || 'U' }}
          </div>
          <div class="flex-1 min-w-0">
            <div class="text-sm font-medium truncate" style="color: var(--text-primary)">{{ userStore.user?.username || t('user.username') }}</div>
            <div class="text-xs truncate" style="color: var(--text-muted)">{{ t('user.plan') }}</div>
          </div>
        </div>
        <button @click="handleLogout"
          class="w-full flex items-center justify-center gap-2 py-2 text-xs font-medium rounded-lg transition-colors hover:bg-white/50 dark:hover:bg-black/20"
          style="color: var(--text-secondary)"
        >
          <LogOut class="w-3.5 h-3.5" />
          {{ t('user.logout') }}
        </button>
      </div>

      <!-- Collapsed User Avatar Only -->
      <div v-else class="px-3 mb-3 flex justify-center">
        <button
          @click="handleLogout"
          class="w-9 h-9 rounded-full flex items-center justify-center transition-colors hover:opacity-80"
          style="background: var(--bg-tertiary); color: var(--text-secondary)"
          :title="t('user.logout')"
        >
          <LogOut class="w-4 h-4" />
        </button>
      </div>
    </aside>

    <!-- Main content area -->
    <main class="flex-1 flex flex-col min-h-screen overflow-hidden">
      <!-- Top header -->
      <header class="h-16 flex items-center gap-4 px-6 sticky top-0 z-30"
        style="background: var(--bg-primary); border-bottom: 1px solid var(--border-color);"
      >
        <button class="lg:hidden" @click="toggleSidebar">
          <Menu class="w-5 h-5" style="color: var(--text-secondary)" />
        </button>

        <!-- Search -->
        <div class="flex-1 max-w-xl relative">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4" style="color: var(--text-muted)" />
          <input v-model="searchQuery" type="text"
            class="w-full pl-10 pr-4 py-2 rounded-xl text-sm transition-all"
            style="background: var(--bg-secondary); border: 1px solid var(--border-color); color: var(--text-primary);"
            :placeholder="t('common.search')"
            @focus="($event.target as HTMLElement).style.borderColor = 'var(--accent)'"
            @blur="($event.target as HTMLElement).style.borderColor = 'var(--border-color)'"
          >
        </div>

        <div class="flex items-center gap-2 ml-auto">
          <ThemeTransition :current-theme="userStore.theme" @toggle="toggleTheme">
            <template #default="{ trigger }">
              <button @click="trigger"
                class="w-9 h-9 rounded-xl flex items-center justify-center transition-colors"
                style="color: var(--text-secondary)"
                :title="t('theme.toggle')"
                @mouseenter="($event.currentTarget as HTMLElement).style.background = 'var(--bg-tertiary)'"
                @mouseleave="($event.currentTarget as HTMLElement).style.background = 'transparent'"
              >
                <Sun v-if="userStore.theme === 'dark'" class="w-4 h-4" />
                <Moon v-else class="w-4 h-4" />
              </button>
            </template>
          </ThemeTransition>
          <!-- Language Switch Button -->
          <button @click="handleToggleLocale"
            class="w-9 h-9 rounded-xl flex items-center justify-center transition-colors"
            style="color: var(--text-secondary)"
            :title="t('language.toggle')"
            @mouseenter="($event.currentTarget as HTMLElement).style.background = 'var(--bg-tertiary)'"
            @mouseleave="($event.currentTarget as HTMLElement).style.background = 'transparent'"
          >
            <Languages class="w-4 h-4" />
          </button>
          <!-- <button class="w-9 h-9 rounded-xl flex items-center justify-center transition-colors relative"
            style="color: var(--text-secondary)"
            @mouseenter="($event.currentTarget as HTMLElement).style.background = 'var(--bg-tertiary)'"
            @mouseleave="($event.currentTarget as HTMLElement).style.background = 'transparent'"
          >
            <Bell class="w-4 h-4" />
            <span class="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-[#f97316]"></span>
          </button> -->
        </div>
      </header>

      <!-- Scrollable content -->
      <div class="flex-1 overflow-y-auto p-6 lg:p-8">
        <slot />
      </div>
    </main>
    <!-- Logout Confirm Dialog -->
    <DeleteConfirmDialog
      v-model="logoutDialogVisible"
      :title="t('user.logout')"
      :description="t('user.logoutConfirm')"
      :confirm-text="t('user.logout')"
      @confirm="confirmLogout"
    />
  </div>
</template>

<style scoped>
.nav-item {
  transition: all 0.15s ease;
  border-radius: 8px;
  position: relative;
}
.nav-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 0;
  background: var(--accent);
  border-radius: 0 2px 2px 0;
  transition: height 0.2s ease;
}
.nav-item.active {
  background: var(--accent-soft);
  color: var(--accent);
}
.nav-item.active::before { height: 60%; }
.nav-item:hover:not(.active) {
  background: var(--bg-tertiary);
}
</style>
