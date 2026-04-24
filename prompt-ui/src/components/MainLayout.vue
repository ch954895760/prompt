<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  Sparkles, LayoutDashboard, FileText, PenTool,
  FolderTree, Tag, Settings, Menu, X, LogOut,
  Search, Sun, Moon, Bell
} from 'lucide-vue-next'
import DeleteConfirmDialog from '@/components/DeleteConfirmDialog.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const sidebarOpen = ref(false)
const searchQuery = ref('')

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

function handleSearch() {
  if (searchQuery.value.trim()) {
    router.push({ path: '/prompts', query: { q: searchQuery.value } })
  }
}

const currentRoute = computed(() => route.path)
const navItems = [
  { path: '/dashboard', label: '控制台', icon: LayoutDashboard },
  { path: '/prompts', label: '提示词库', icon: FileText },
  { path: '/editor', label: '新建提示词', icon: PenTool },
]
const manageItems = [
  { path: '/categories', label: '分类管理', icon: FolderTree },
  { path: '/tags', label: '标签管理', icon: Tag },
  { path: '/settings', label: '设置', icon: Settings },
]

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
    <aside class="sidebar w-64 flex-shrink-0 flex flex-col h-screen sticky top-0 z-40 transition-transform duration-300 lg:translate-x-0"
      :class="sidebarOpen ? 'translate-x-0' : '-translate-x-full'"
      style="background: var(--bg-secondary); border-right: 1px solid var(--border-color);"
    >
      <!-- Logo -->
      <div class="p-5 flex items-center gap-3">
        <div class="w-9 h-9 rounded-xl bg-[#ea580c] text-white flex items-center justify-center shadow-md shadow-[#ea580c]/20">
          <Sparkles class="w-5 h-5" />
        </div>
        <span class="font-bold text-lg tracking-tight" style="color: var(--text-primary)">Prompt Vault</span>
        <button class="lg:hidden ml-auto" @click="toggleSidebar">
          <X class="w-5 h-5" style="color: var(--text-secondary)" />
        </button>
      </div>

      <!-- Navigation -->
      <nav class="flex-1 px-3 space-y-1 overflow-y-auto">
        <div class="text-[10px] font-semibold uppercase tracking-wider px-3 py-2" style="color: var(--text-muted)">工作区</div>
        <a v-for="item in navItems" :key="item.path" href="#"
          class="nav-item flex items-center gap-3 px-3 py-2.5 text-sm font-medium"
          :class="currentRoute === item.path ? 'active' : ''"
          :style="currentRoute === item.path ? '' : 'color: var(--text-secondary)'"
          @click.prevent="navigate(item.path)"
        >
          <component :is="item.icon" class="w-4.5 h-4.5" />
          {{ item.label }}
        </a>

        <div class="text-[10px] font-semibold uppercase tracking-wider px-3 py-2 mt-4" style="color: var(--text-muted)">管理</div>
        <a v-for="item in manageItems" :key="item.path" href="#"
          class="nav-item flex items-center gap-3 px-3 py-2.5 text-sm font-medium"
          :class="currentRoute === item.path ? 'active' : ''"
          :style="currentRoute === item.path ? '' : 'color: var(--text-secondary)'"
          @click.prevent="navigate(item.path)"
        >
          <component :is="item.icon" class="w-4.5 h-4.5" />
          {{ item.label }}
        </a>
      </nav>

      <!-- User section -->
      <div class="p-4 mx-3 mb-3 rounded-xl" style="background: var(--bg-tertiary)">
        <div class="flex items-center gap-3 mb-3">
          <div class="w-9 h-9 rounded-full bg-gradient-to-br from-[#fb923c] to-[#ea580c] text-white flex items-center justify-center text-sm font-bold">
            {{ userStore.user?.username?.charAt(0)?.toUpperCase() || 'U' }}
          </div>
          <div class="flex-1 min-w-0">
            <div class="text-sm font-medium truncate" style="color: var(--text-primary)">{{ userStore.user?.username || '用户' }}</div>
            <div class="text-xs truncate" style="color: var(--text-muted)">Pro 计划</div>
          </div>
        </div>
        <button @click="handleLogout"
          class="w-full flex items-center justify-center gap-2 py-2 text-xs font-medium rounded-lg transition-colors hover:bg-white/50 dark:hover:bg-black/20"
          style="color: var(--text-secondary)"
        >
          <LogOut class="w-3.5 h-3.5" />
          退出登录
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
            placeholder="搜索提示词、标签、内容..."
            @keyup.enter="handleSearch"
            @focus="($event.target as HTMLElement).style.borderColor = 'var(--accent)'"
            @blur="($event.target as HTMLElement).style.borderColor = 'var(--border-color)'"
          >
        </div>

        <div class="flex items-center gap-2">
          <button @click="toggleTheme"
            class="w-9 h-9 rounded-xl flex items-center justify-center transition-colors hover:bg-surface-200 dark:hover:bg-surface-800"
            style="color: var(--text-secondary)"
          >
            <Sun v-if="userStore.theme === 'dark'" class="w-4 h-4" />
            <Moon v-else class="w-4 h-4" />
          </button>
          <button class="w-9 h-9 rounded-xl flex items-center justify-center transition-colors hover:bg-surface-200 dark:hover:bg-surface-800 relative"
            style="color: var(--text-secondary)"
          >
            <Bell class="w-4 h-4" />
            <span class="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-[#f97316]"></span>
          </button>
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
      title="确认退出"
      description="确定要退出登录吗？"
      confirm-text="退出登录"
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
