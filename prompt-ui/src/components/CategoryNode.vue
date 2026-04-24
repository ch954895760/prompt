<script setup lang="ts">
import type { Category } from '@/types'
import { ChevronRight, Pencil, Trash2, Plus } from 'lucide-vue-next'

const props = defineProps<{
  category: Category
  expandedIds: Set<number>
  level?: number
}>()

const emit = defineEmits<{
  toggle: [id: number]
  edit: [category: Category]
  delete: [id: number, name: string]
  addChild: [parentId: number]
}>()

const isExpanded = () => props.expandedIds.has(props.category.id)
const hasChildren = () => props.category.children && props.category.children.length > 0
</script>

<template>
  <div>
    <div class="flex items-center gap-3 py-2.5 px-3 rounded-xl transition-colors group cursor-pointer"
      :style="{ paddingLeft: `${12 + (level || 0) * 24}px` }"
      @mouseenter="($event.currentTarget as HTMLElement).style.background = 'var(--bg-tertiary)'"
      @mouseleave="($event.currentTarget as HTMLElement).style.background = 'transparent'"
    >
      <button v-if="hasChildren()" @click.stop="emit('toggle', category.id)"
        class="w-5 h-5 rounded-md flex items-center justify-center transition-colors hover:bg-surface-200 dark:hover:bg-surface-700 flex-shrink-0"
      >
        <ChevronRight class="w-3.5 h-3.5 transition-transform" style="color: var(--text-muted)"
          :class="isExpanded() ? 'rotate-90' : ''"
        />
      </button>
      <div v-else class="w-5 h-5 flex-shrink-0"></div>

      <div class="w-3 h-3 rounded-full flex-shrink-0" :style="{ background: category.color }"></div>
      <span class="text-sm font-medium flex-1" style="color: var(--text-primary)">{{ category.name }}</span>
      <span class="text-xs px-2 py-0.5 rounded-full" style="background: var(--bg-tertiary); color: var(--text-muted)">
        {{ category.promptCount || 0 }}
      </span>
      <div class="hidden group-hover:flex items-center gap-1">
        <button @click.stop="emit('addChild', category.id)"
          class="p-1 rounded hover:bg-surface-200 dark:hover:bg-surface-700" style="color: var(--text-muted);"
        >
          <Plus class="w-3 h-3" />
        </button>
        <button @click.stop="emit('edit', category)"
          class="p-1 rounded hover:bg-surface-200 dark:hover:bg-surface-700" style="color: var(--text-muted);"
        >
          <Pencil class="w-3 h-3" />
        </button>
        <button @click.stop="emit('delete', category.id, category.name)"
          class="p-1 rounded hover:bg-red-50 dark:hover:bg-red-900/30" style="color: var(--text-muted);"
        >
          <Trash2 class="w-3 h-3 hover:text-red-500" />
        </button>
      </div>
    </div>

    <div v-if="hasChildren() && isExpanded()" class="relative">
      <div class="absolute left-[22px] top-0 bottom-0 w-[1.5px]" style="background: var(--border-color);"></div>
      <CategoryNode v-for="child in category.children" :key="child.id"
        :category="child"
        :expanded-ids="expandedIds"
        :level="(level || 0) + 1"
        @toggle="emit('toggle', $event)"
        @edit="emit('edit', $event)"
        @delete="(id, name) => emit('delete', id, name)"
        @add-child="emit('addChild', $event)"
      />
    </div>
  </div>
</template>
