import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

export type ToastType = 'success' | 'error' | 'warning' | 'info'

export interface Toast {
  id: string
  message: string
  type: ToastType
  duration: number
}

export const useToastStore = defineStore('toast', () => {
  const toasts = ref<Toast[]>([])

  const activeToasts = computed(() => toasts.value)

  function generateId(): string {
    return `toast-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
  }

  function show(message: string, type: ToastType = 'info', duration: number = 3000) {
    const id = generateId()
    const toast: Toast = {
      id,
      message,
      type,
      duration
    }
    toasts.value.push(toast)

    if (duration > 0) {
      setTimeout(() => {
        remove(id)
      }, duration)
    }

    return id
  }

  function remove(id: string) {
    const index = toasts.value.findIndex(t => t.id === id)
    if (index > -1) {
      toasts.value.splice(index, 1)
    }
  }

  function success(message: string, duration?: number) {
    return show(message, 'success', duration)
  }

  function error(message: string, duration?: number) {
    return show(message, 'error', duration)
  }

  function warning(message: string, duration?: number) {
    return show(message, 'warning', duration)
  }

  function info(message: string, duration?: number) {
    return show(message, 'info', duration)
  }

  function clear() {
    toasts.value = []
  }

  return {
    toasts: activeToasts,
    show,
    remove,
    success,
    error,
    warning,
    info,
    clear
  }
})
