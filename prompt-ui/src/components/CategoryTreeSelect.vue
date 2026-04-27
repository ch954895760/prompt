<script setup lang="ts">
import type { Category } from '@/types'
import { ChevronRight } from 'lucide-vue-next'
import { ref } from 'vue'

const props = defineProps<{
  category: Category
  selectedId: number | null
  level?: number
}>()

const emit = defineEmits<{
  select: [id: number | null]
}>()

const expanded = ref(false)

const hasChildren = () => props.category.children && props.category.children.length > 0
const isSelected = () => props.selectedId === props.category.id

function toggleExpand(e: Event) {
  e.stopPropagation()
  expanded.value = !expanded.value
}

function handleSelect() {
  emit('select', props.category.id)
}
</script>

<template>
  <div>
    <div
      @click="handleSelect"
      class="cursor-pointer transition-colors flex items-center gap-2"
      :style="{
        paddingLeft: `${12 + (level || 0) * 20}px`,
        background: isSelected() ? 'var(--accent-soft)' : 'transparent',
        color: isSelected() ? 'var(--accent)' : 'var(--text-primary)'
      }"
      @mouseenter="($event.currentTarget as HTMLElement).style.background = isSelected() ? 'var(--accent-soft)' : 'var(--bg-tertiary)'"
      @mouseleave="($event.currentTarget as HTMLElement).style.background = isSelected() ? 'var(--accent-soft)' : 'transparent'"
    >
      <!-- 展开/折叠按钮 -->
      <button
        v-if="hasChildren()"
        @click.stop="toggleExpand"
        class="w-5 h-5 rounded flex items-center justify-center transition-colors hover:bg-[var(--bg-tertiary)] flex-shrink-0"
      >
        <ChevronRight
          class="w-3.5 h-3.5 transition-transform"
          style="color: var(--text-muted)"
          :class="expanded ? 'rotate-90' : ''"
        />
      </button>
      <div v-else class="w-5 h-5 flex-shrink-0"></div>

      <!-- 分类颜色标识 -->
      <div
        class="w-2.5 h-2.5 rounded-full flex-shrink-0"
        :style="{ background: category.color }"
      ></div>

      <!-- 分类名称 -->
      <span class="text-sm py-2.5 flex-1 truncate">{{ category.name }}</span>

      <!-- 选中标记 -->
      <svg
        v-if="isSelected()"
        class="w-4 h-4 mr-3 flex-shrink-0"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
      >
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
      </svg>
      <div v-else class="w-4 h-4 mr-3 flex-shrink-0"></div>
    </div>

    <!-- 子分类 -->
    <div v-if="hasChildren() && expanded">
      <CategoryTreeSelect
        v-for="child in category.children"
        :key="child.id"
        :category="child"
        :selected-id="selectedId"
        :level="(level || 0) + 1"
        @select="emit('select', $event)"
      />
    </div>
  </div>
</template>
