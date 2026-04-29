<script setup lang="ts">
import { ref, nextTick, computed } from 'vue'

const props = defineProps<{
  currentTheme: 'light' | 'dark'
}>()

const isAnimating = ref(false)
const rippleStyle = ref({
  left: '0px',
  top: '0px',
  width: '0px',
  height: '0px',
})

// 目标主题色（即将切换到的主题）
const targetThemeColor = computed(() => {
  // 如果当前是深色，目标是浅色，使用浅色背景色
  // 如果当前是浅色，目标是深色，使用深色背景色
  return props.currentTheme === 'dark' ? '#ffffff' : '#0c0a09'
})

const emit = defineEmits<{
  toggle: []
}>()

async function triggerTransition(event: MouseEvent) {
  if (isAnimating.value) return

  const button = event.currentTarget as HTMLElement
  const rect = button.getBoundingClientRect()
  const x = rect.left + rect.width / 2
  const y = rect.top + rect.height / 2

  const maxRadius = Math.max(
    Math.hypot(x, y),
    Math.hypot(window.innerWidth - x, y),
    Math.hypot(x, window.innerHeight - y),
    Math.hypot(window.innerWidth - x, window.innerHeight - y)
  )

  rippleStyle.value = {
    left: `${x}px`,
    top: `${y}px`,
    width: `${maxRadius * 2}px`,
    height: `${maxRadius * 2}px`,
  }

  isAnimating.value = true

  await nextTick()

  setTimeout(() => {
    emit('toggle')
  }, 300)

  setTimeout(() => {
    isAnimating.value = false
  }, 600)
}
</script>

<template>
  <div class="theme-transition-wrapper">
    <slot :trigger="triggerTransition" />
    <Teleport to="body">
      <div
        v-if="isAnimating"
        class="theme-ripple"
        :style="[rippleStyle, { backgroundColor: targetThemeColor }]"
      />
    </Teleport>
  </div>
</template>

<style scoped>
.theme-transition-wrapper {
  display: contents;
}

.theme-ripple {
  position: fixed;
  border-radius: 50%;
  pointer-events: none;
  z-index: 9999;
  transform: translate(-50%, -50%) scale(0);
  animation: themeRippleExpand 0.6s ease-out forwards;
}

@keyframes themeRippleExpand {
  0% {
    transform: translate(-50%, -50%) scale(0);
    opacity: 0.9;
  }
  50% {
    opacity: 0.7;
  }
  100% {
    transform: translate(-50%, -50%) scale(1);
    opacity: 0;
  }
}
</style>
