<script setup lang="ts">
import { useToastStore, type ToastType } from '@/stores/toast'
import { CheckCircle, XCircle, AlertTriangle, Info } from 'lucide-vue-next'

const toastStore = useToastStore()

const iconMap: Record<ToastType, typeof CheckCircle> = {
  success: CheckCircle,
  error: XCircle,
  warning: AlertTriangle,
  info: Info
}

const colorMap: Record<ToastType, { bg: string; icon: string; border: string }> = {
  success: {
    bg: 'bg-emerald-50 dark:bg-emerald-900/20',
    icon: 'text-emerald-500',
    border: 'border-emerald-200 dark:border-emerald-800'
  },
  error: {
    bg: 'bg-red-50 dark:bg-red-900/20',
    icon: 'text-red-500',
    border: 'border-red-200 dark:border-red-800'
  },
  warning: {
    bg: 'bg-amber-50 dark:bg-amber-900/20',
    icon: 'text-amber-500',
    border: 'border-amber-200 dark:border-amber-800'
  },
  info: {
    bg: 'bg-blue-50 dark:bg-blue-900/20',
    icon: 'text-blue-500',
    border: 'border-blue-200 dark:border-blue-800'
  }
}

function getIcon(type: ToastType) {
  return iconMap[type]
}

function getColors(type: ToastType) {
  return colorMap[type]
}
</script>

<template>
  <Teleport to="body">
    <div class="fixed top-6 right-6 z-[9999] flex flex-col gap-3 pointer-events-none">
      <TransitionGroup name="toast">
        <div
          v-for="toast in toastStore.toasts"
          :key="toast.id"
          class="pointer-events-auto flex items-center gap-3 px-4 py-3 rounded-xl shadow-lg border min-w-[280px] max-w-[400px]"
          :class="[getColors(toast.type).bg, getColors(toast.type).border]"
          style="background: var(--bg-secondary); border-color: var(--border-color);"
        >
          <div class="flex-shrink-0">
            <component
              :is="getIcon(toast.type)"
              class="w-5 h-5"
              :class="getColors(toast.type).icon"
            />
          </div>
          <span class="text-sm flex-1" style="color: var(--text-primary)">
            {{ toast.message }}
          </span>
          <button
            @click="toastStore.remove(toast.id)"
            class="flex-shrink-0 p-1 rounded-lg transition-colors hover:bg-black/5 dark:hover:bg-white/10"
            style="color: var(--text-muted)"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.toast-enter-from {
  opacity: 0;
  transform: translateX(100%) scale(0.9);
}

.toast-leave-to {
  opacity: 0;
  transform: translateX(100%) scale(0.9);
}

.toast-move {
  transition: transform 0.3s ease;
}
</style>
