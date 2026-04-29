<script setup lang="ts">
import { AlertTriangle, X } from 'lucide-vue-next'
import { useI18n } from 'vue-i18n'
import { computed } from 'vue'

const { t } = useI18n()

const props = withDefaults(defineProps<{
  modelValue: boolean
  title?: string
  itemName?: string
  description?: string
  confirmText?: string
  cancelText?: string
}>(), {
  title: '',
  description: '',
  confirmText: '',
  cancelText: '',
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: []
  cancel: []
}>()

// 使用计算属性提供默认值
const displayTitle = computed(() => props.title || t('dialog.deleteTitle'))
const displayConfirmText = computed(() => props.confirmText || t('common.delete'))
const displayCancelText = computed(() => props.cancelText || t('common.cancel'))

function close() {
  emit('update:modelValue', false)
  emit('cancel')
}

function confirm() {
  emit('update:modelValue', false)
  emit('confirm')
}

function onMaskClick(e: MouseEvent) {
  if (e.target === e.currentTarget) close()
}
</script>

<template>
  <Teleport to="body">
    <Transition name="dialog">
      <div v-if="modelValue" class="fixed inset-0 z-[100] flex items-center justify-center" @click="onMaskClick">
        <!-- Backdrop -->
        <div class="absolute inset-0 bg-black/40 backdrop-blur-sm transition-opacity dialog-backdrop"></div>

        <!-- Dialog -->
        <div class="relative w-full max-w-[400px] mx-4 rounded-2xl p-6 dialog-panel"
          style="background: var(--bg-primary); border: 1px solid var(--border-color); box-shadow: 0 24px 80px rgba(0,0,0,0.2);"
        >
          <!-- Close button -->
          <button @click="close"
            class="absolute top-4 right-4 p-1 rounded-lg transition-colors"
            style="color: var(--text-muted);"
            @mouseenter="($event.currentTarget as HTMLElement).style.background = 'var(--bg-tertiary)'"
            @mouseleave="($event.currentTarget as HTMLElement).style.background = 'transparent'"
          >
            <X class="w-4 h-4" />
          </button>

          <!-- Icon -->
          <div class="w-12 h-12 rounded-full flex items-center justify-center mb-4"
            style="background: rgba(239, 68, 68, 0.1);"
          >
            <AlertTriangle class="w-6 h-6 text-red-500" />
          </div>

          <!-- Content -->
          <h3 class="text-lg font-semibold mb-2" style="color: var(--text-primary)">{{ displayTitle }}</h3>
          <p v-if="description" class="text-sm leading-relaxed mb-1" style="color: var(--text-secondary)">{{ description }}</p>
          <p v-else-if="itemName" class="text-sm leading-relaxed mb-1" style="color: var(--text-secondary)">
            {{ t('dialog.deleteConfirm', { name: itemName }) }}
          </p>

          <!-- Actions -->
          <div class="flex gap-3 mt-6">
            <button @click="close"
              class="flex-1 px-4 py-2.5 text-sm font-medium rounded-xl transition-all"
              style="background: var(--bg-secondary); border: 1px solid var(--border-color); color: var(--text-primary);"
              @mouseenter="($event.currentTarget as HTMLElement).style.background = 'var(--bg-tertiary)'"
              @mouseleave="($event.currentTarget as HTMLElement).style.background = 'var(--bg-secondary)'"
            >
              {{ displayCancelText }}
            </button>
            <button @click="confirm"
              class="flex-1 px-4 py-2.5 text-sm font-medium rounded-xl transition-all bg-red-500 hover:bg-red-600 text-white"
              style="box-shadow: 0 4px 16px rgba(239, 68, 68, 0.25);"
            >
              {{ displayConfirmText }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.dialog-enter-active,
.dialog-leave-active {
  transition: all 0.2s ease;
}

.dialog-enter-from,
.dialog-leave-to {
  opacity: 0;
}

.dialog-enter-from .dialog-panel,
.dialog-leave-to .dialog-panel {
  opacity: 0;
  transform: scale(0.96) translateY(8px);
}

.dialog-panel {
  transition: all 0.2s ease;
}
</style>
