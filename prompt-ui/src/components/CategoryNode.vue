<script setup lang="ts">
import type { Category } from '@/types'
import { ChevronRight, Pencil, Trash2, Plus, GripVertical } from 'lucide-vue-next'
import { ref } from 'vue'

const props = defineProps<{
  category: Category
  expandedIds: Set<number>
  level?: number
  draggable?: boolean
}>()

const emit = defineEmits<{
  toggle: [id: number]
  edit: [category: Category]
  delete: [id: number, name: string]
  addChild: [parentId: number]
  dragStart: [event: DragEvent, category: Category]
  dragOver: [event: DragEvent, category: Category]
  dragLeave: [event: DragEvent, category: Category]
  drop: [event: DragEvent, category: Category]
  dragEnd: [event: DragEvent]
}>()

const isExpanded = () => props.expandedIds.has(props.category.id)
const hasChildren = () => props.category.children && props.category.children.length > 0

const isDragging = ref(false)
const isDragOver = ref(false)

function handleDragStart(event: DragEvent) {
  isDragging.value = true
  emit('dragStart', event, props.category)
}

function handleDragOver(event: DragEvent) {
  event.preventDefault()
  isDragOver.value = true
  emit('dragOver', event, props.category)
}

function handleDragLeave(event: DragEvent) {
  isDragOver.value = false
  emit('dragLeave', event, props.category)
}

function handleDrop(event: DragEvent) {
  event.preventDefault()
  isDragOver.value = false
  emit('drop', event, props.category)
}

function handleDragEnd(event: DragEvent) {
  isDragging.value = false
  emit('dragEnd', event)
}
</script>

<template>
  <div>
    <div
      class="flex items-center gap-3 py-2.5 px-3 rounded-xl transition-all group cursor-pointer"
      :class="{
        'opacity-50': isDragging,
        'ring-2 ring-[#ea580c] ring-opacity-50 bg-[#ea580c]/5': isDragOver
      }"
      :style="{ paddingLeft: `${12 + (level || 0) * 24}px` }"
      :draggable="draggable"
      @dragstart="handleDragStart"
      @dragover="handleDragOver"
      @dragleave="handleDragLeave"
      @drop="handleDrop"
      @dragend="handleDragEnd"
      @mouseenter="($event.currentTarget as HTMLElement).style.background = isDragOver ? '' : 'var(--bg-tertiary)'"
      @mouseleave="($event.currentTarget as HTMLElement).style.background = 'transparent'"
    >
      <!-- Drag Handle -->
      <div
        v-if="draggable"
        class="w-5 h-5 flex items-center justify-center flex-shrink-0 cursor-grab active:cursor-grabbing text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
      >
        <GripVertical class="w-3.5 h-3.5" />
      </div>

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
        :draggable="draggable"
        @toggle="emit('toggle', $event)"
        @edit="emit('edit', $event)"
        @delete="(id, name) => emit('delete', id, name)"
        @add-child="emit('addChild', $event)"
        @drag-start="(evt: DragEvent, cat: Category) => emit('dragStart', evt, cat)"
        @drag-over="(evt: DragEvent, cat: Category) => emit('dragOver', evt, cat)"
        @drag-leave="(evt: DragEvent, cat: Category) => emit('dragLeave', evt, cat)"
        @drop="(evt: DragEvent, cat: Category) => emit('drop', evt, cat)"
        @drag-end="(evt: DragEvent) => emit('dragEnd', evt)"
      />
    </div>
  </div>
</template>
