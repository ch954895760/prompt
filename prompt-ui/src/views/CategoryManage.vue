<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useToastStore } from '@/stores/toast'
import MainLayout from '@/components/MainLayout.vue'
import CategoryNode from '@/components/CategoryNode.vue'
import { getCategoryTree, createCategory, updateCategory, deleteCategory, updateCategorySort, type SortItem } from '@/api/category'
import type { Category } from '@/types'
import { Plus, Pencil, Trash2, ChevronRight, ChevronDown, GripVertical } from 'lucide-vue-next'
import DeleteConfirmDialog from '@/components/DeleteConfirmDialog.vue'

const toastStore = useToastStore()

const categories = ref<Category[]>([])
const loading = ref(false)
const showModal = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const expandedIds = ref<Set<number>>(new Set())
const isEditMode = ref(false)

const form = ref({ name: '', parentId: null as number | null, color: '#ea580c', icon: '' })

const colorOptions = ['#ea580c', '#3b82f6', '#8b5cf6', '#10b981', '#f59e0b', '#ef4444', '#ec4899', '#6366f1']

async function loadData() {
  loading.value = true
  try {
    categories.value = await getCategoryTree()
    // 默认展开所有有子分类的节点
    expandedIds.value = new Set(getExpandableIds(categories.value))
  } catch (e) {
    console.error('[DEBUG] Failed to load categories:', e)
  } finally {
    loading.value = false
  }
}

function toggleExpand(id: number) {
  if (expandedIds.value.has(id)) {
    expandedIds.value.delete(id)
  } else {
    expandedIds.value.add(id)
  }
}

function openCreateModal(parentId?: number) {
  modalMode.value = 'create'
  editingId.value = null
  form.value = { name: '', parentId: parentId || null, color: '#ea580c', icon: '' }
  showModal.value = true
}

function openEditModal(cat: Category) {
  modalMode.value = 'edit'
  editingId.value = cat.id
  form.value = { name: cat.name, parentId: cat.parentId ?? null, color: cat.color, icon: cat.icon || '' }
  showModal.value = true
}

async function handleSubmit() {
  if (!form.value.name.trim()) {
    toastStore.warning('请输入分类名称')
    return
  }
  try {
    const data = {
      ...form.value,
      parentId: form.value.parentId ?? undefined
    }
    if (modalMode.value === 'edit' && editingId.value) {
      await updateCategory(editingId.value, data)
      toastStore.success('分类已更新')
    } else {
      await createCategory(data)
      toastStore.success('分类已创建')
    }
    showModal.value = false
    await loadData()
  } catch (e: any) {
    toastStore.error(e.message || '操作失败')
  }
}

const deleteDialogVisible = ref(false)
const deleteTarget = ref<{ id: number; name: string } | null>(null)

function handleDelete(id: number, name: string) {
  deleteTarget.value = { id, name }
  deleteDialogVisible.value = true
}

async function confirmDelete() {
  if (!deleteTarget.value) return
  try {
    await deleteCategory(deleteTarget.value.id)
    toastStore.success(`"${deleteTarget.value.name}" 已删除`)
    await loadData()
  } catch (e: any) {
    toastStore.error(e.message || '删除失败')
  } finally {
    deleteTarget.value = null
  }
}

function getExpandableIds(cats: Category[]): number[] {
  return cats.flatMap(c => {
    const ids = c.children && c.children.length > 0 ? getExpandableIds(c.children) : []
    return c.children && c.children.length > 0 ? [c.id, ...ids] : []
  })
}

function getTotalCount(cats: Category[]): number {
  return cats.reduce((sum, c) => sum + (c.promptCount || 0) + getTotalCount(c.children || []), 0)
}

function getRootCount(): number {
  return categories.value.filter(c => !c.parentId).length
}

function getChildCount(cats?: Category[]): number {
  const list = cats || categories.value
  return list.reduce((sum, c) => {
    const childCount = c.children && c.children.length > 0 ? getChildCount(c.children) : 0
    return sum + (c.parentId ? 1 : 0) + childCount
  }, 0)
}

// ==================== 拖拽排序功能 ====================

// 拖拽状态
const draggedCategory = ref<Category | null>(null)
const dragOverCategory = ref<Category | null>(null)
const dragPosition = ref<'before' | 'after' | 'inside'>('after')

// 扁平化分类列表用于排序
const flatCategories = computed(() => {
  const result: Category[] = []
  function flatten(cats: Category[], parentId?: number) {
    cats.forEach(cat => {
      result.push({ ...cat, parentId: parentId ?? cat.parentId })
      if (cat.children && cat.children.length > 0) {
        flatten(cat.children, cat.id)
      }
    })
  }
  flatten(categories.value)
  return result
})

// 获取同级别的分类
function getSiblings(category: Category): Category[] {
  if (!category.parentId) {
    return categories.value.filter(c => !c.parentId)
  }
  
  function findParent(cats: Category[], parentId: number): Category | null {
    for (const cat of cats) {
      if (cat.id === parentId) return cat
      if (cat.children && cat.children.length > 0) {
        const found = findParent(cat.children, parentId)
        if (found) return found
      }
    }
    return null
  }
  
  const parent = findParent(categories.value, category.parentId)
  return parent?.children || []
}

// 拖拽开始
function handleDragStart(event: DragEvent, category: Category) {
  if (!isEditMode.value) {
    event.preventDefault()
    return
  }
  draggedCategory.value = category
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', String(category.id))
  }
}

// 拖拽经过
function handleDragOver(event: DragEvent, category: Category) {
  if (!isEditMode.value || !draggedCategory.value) return
  event.preventDefault()
  
  dragOverCategory.value = category
  
  // 计算拖拽位置（在目标之前、之后或内部）
  const rect = (event.currentTarget as HTMLElement).getBoundingClientRect()
  const relativeY = event.clientY - rect.top
  const height = rect.height
  
  if (relativeY < height * 0.25) {
    dragPosition.value = 'before'
  } else if (relativeY > height * 0.75) {
    dragPosition.value = 'after'
  } else {
    dragPosition.value = 'inside'
  }
  
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
}

// 拖拽离开
function handleDragLeave(event: DragEvent, category: Category) {
  if (dragOverCategory.value?.id === category.id) {
    dragOverCategory.value = null
  }
}

// 放置
async function handleDrop(event: DragEvent, targetCategory: Category) {
  if (!isEditMode.value || !draggedCategory.value) return
  event.preventDefault()
  
  const source = draggedCategory.value
  const target = targetCategory
  
  // 防止拖放到自身或子元素中
  if (source.id === target.id) {
    resetDragState()
    return
  }
  
  // 检查是否拖放到自己的子元素中
  if (isDescendant(source, target)) {
    toastStore.warning('不能将分类拖放到其子分类中')
    resetDragState()
    return
  }

  try {
    // 构建新的排序数据
    const sortItems = buildSortItems(source, target, dragPosition.value)
    await updateCategorySort(sortItems)
    toastStore.success('排序已更新')
    await loadData()
  } catch (e: any) {
    toastStore.error(e.message || '排序失败')
  } finally {
    resetDragState()
  }
}

// 检查target是否是source的后代
function isDescendant(source: Category, target: Category): boolean {
  function hasDescendant(cat: Category, id: number): boolean {
    if (!cat.children || cat.children.length === 0) return false
    for (const child of cat.children) {
      if (child.id === id || hasDescendant(child, id)) return true
    }
    return false
  }
  return hasDescendant(source, target.id)
}

// 构建排序项目列表
function buildSortItems(source: Category, target: Category, position: 'before' | 'after' | 'inside'): SortItem[] {
  const items: SortItem[] = []
  
  // 确定新的parentId
  let newParentId: number | null = null
  let siblingList: Category[] = []
  
  if (position === 'inside') {
    // 成为目标的子分类
    newParentId = target.id
    siblingList = target.children || []
  } else {
    // 与目标同级
    newParentId = target.parentId ?? null
    siblingList = getSiblings(target)
  }
  
  // 重新排序同级别的分类
  const reorderedSiblings: Category[] = []
  
  if (position === 'before') {
    // 插入到目标之前
    for (const sib of siblingList) {
      if (sib.id === target.id) {
        reorderedSiblings.push(source)
      }
      if (sib.id !== source.id) {
        reorderedSiblings.push(sib)
      }
    }
  } else if (position === 'after') {
    // 插入到目标之后
    for (const sib of siblingList) {
      if (sib.id !== source.id) {
        reorderedSiblings.push(sib)
      }
      if (sib.id === target.id) {
        reorderedSiblings.push(source)
      }
    }
  } else {
    // 插入到子分类列表末尾
    reorderedSiblings.push(...siblingList.filter(s => s.id !== source.id))
    reorderedSiblings.push(source)
  }
  
  // 生成排序项
  reorderedSiblings.forEach((cat, index) => {
    items.push({
      id: cat.id,
      sortOrder: index,
      parentId: newParentId ?? cat.parentId
    })
  })
  
  return items
}

// 拖拽结束
function handleDragEnd(event: DragEvent) {
  resetDragState()
}

// 拖放到容器成为顶级分类
function handleContainerDragOver(event: DragEvent) {
  if (!isEditMode.value || !draggedCategory.value) return
  event.preventDefault()
  
  // 只有当拖拽到空白区域时才显示放置指示
  const target = event.target as HTMLElement
  if (target.closest('.category-node')) return
  
  dragOverCategory.value = null
  dragPosition.value = 'after'
  
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
}

// 放置到容器成为顶级分类
async function handleContainerDrop(event: DragEvent) {
  if (!isEditMode.value || !draggedCategory.value) return
  event.preventDefault()
  
  // 只有当放置到空白区域时才处理
  const target = event.target as HTMLElement
  if (target.closest('.category-node')) return
  
  const source = draggedCategory.value
  
  try {
    // 获取所有顶级分类
    const rootCategories = categories.value.filter(c => !c.parentId)
    
    // 如果已经是顶级分类，不做任何操作
    if (source.parentId === null && rootCategories.some(c => c.id === source.id)) {
      resetDragState()
      return
    }
    
    // 构建新的排序数据：将拖拽的分类放到顶级分类列表末尾
    const items: SortItem[] = []
    
    // 保留其他顶级分类
    rootCategories.forEach((cat, index) => {
      items.push({
        id: cat.id,
        sortOrder: index,
        parentId: null
      })
    })
    
    // 添加拖拽的分类到末尾
    items.push({
      id: source.id,
      sortOrder: rootCategories.length,
      parentId: null
    })
    
    await updateCategorySort(items)
    toastStore.success('已移动到顶级目录')
    await loadData()
  } catch (e: any) {
    toastStore.error(e.message || '移动失败')
  } finally {
    resetDragState()
  }
}

// 重置拖拽状态
function resetDragState() {
  draggedCategory.value = null
  dragOverCategory.value = null
  dragPosition.value = 'after'
}

onMounted(loadData)
</script>

<template>
  <MainLayout>
    <div class="animate-fade-in">
      <div class="flex items-center justify-between mb-6">
        <div>
          <h2 class="text-2xl font-bold mb-1" style="color: var(--text-primary)">分类管理</h2>
          <p class="text-sm" style="color: var(--text-secondary)">组织你的提示词结构</p>
        </div>
        <div class="flex items-center gap-3">
          <button
            @click="isEditMode = !isEditMode"
            class="flex items-center gap-2 px-4 py-2.5 text-sm font-medium rounded-xl transition-all"
            :class="isEditMode 
              ? 'bg-[#ea580c]/10 text-[#ea580c] hover:bg-[#ea580c]/20' 
              : 'hover:opacity-80'"
            :style="!isEditMode ? { backgroundColor: 'var(--bg-secondary)', color: 'var(--text-secondary)' } : {}"
          >
            <GripVertical class="w-4 h-4" />
            {{ isEditMode ? '完成排序' : '排序模式' }}
          </button>
          <button @click="openCreateModal()"
            class="flex items-center gap-2 px-4 py-2.5 bg-[#ea580c] hover:bg-[#c2410c] text-white text-sm font-medium rounded-xl transition-all shadow-lg shadow-[#ea580c]/20"
          >
            <Plus class="w-4 h-4" />
            新建分类
          </button>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- Category tree -->
        <div class="lg:col-span-2 rounded-2xl p-6 relative"
          style="background: var(--bg-secondary); border: 1px solid var(--border-color);"
          :class="{ 'ring-2 ring-[#ea580c] ring-opacity-30': isEditMode && draggedCategory && !dragOverCategory }"
          @dragover="handleContainerDragOver"
          @drop="handleContainerDrop"
        >
          <div v-if="loading" class="text-center py-8" style="color: var(--text-muted)">加载中...</div>
          <div v-else-if="categories.length === 0" class="text-center py-8" style="color: var(--text-muted)">暂无分类</div>
          <div v-else class="space-y-1">
            <CategoryNode v-for="cat in categories.filter(c => !c.parentId)" :key="cat.id"
              :category="cat"
              :expanded-ids="expandedIds"
              :draggable="isEditMode"
              class="category-node"
              @toggle="toggleExpand"
              @edit="openEditModal"
              @delete="handleDelete"
              @add-child="openCreateModal"
              @drag-start="handleDragStart"
              @drag-over="handleDragOver"
              @drag-leave="handleDragLeave"
              @drop="handleDrop"
              @drag-end="handleDragEnd"
            />
          </div>
          <!-- 拖放到顶级目录的提示 -->
          <div v-if="isEditMode && draggedCategory && !dragOverCategory"
            class="absolute inset-0 flex items-center justify-center pointer-events-none"
          >
            <div class="px-4 py-2 rounded-lg text-sm font-medium bg-[#ea580c]/10 text-[#ea580c] border border-[#ea580c]/30">
              拖放到此处成为顶级分类
            </div>
          </div>
        </div>

        <!-- Quick stats -->
        <div class="space-y-5">
          <div class="rounded-2xl p-5" style="background: var(--bg-secondary); border: 1px solid var(--border-color);">
            <h4 class="text-sm font-semibold mb-4" style="color: var(--text-primary)">分类统计</h4>
            <div class="space-y-3">
              <div class="flex items-center justify-between">
                <span class="text-xs" style="color: var(--text-secondary)">一级分类</span>
                <span class="text-sm font-semibold" style="color: var(--text-primary)">{{ getRootCount() }}</span>
              </div>
              <div class="flex items-center justify-between">
                <span class="text-xs" style="color: var(--text-secondary)">子分类</span>
                <span class="text-sm font-semibold" style="color: var(--text-primary)">{{ getChildCount() }}</span>
              </div>
              <div class="flex items-center justify-between">
                <span class="text-xs" style="color: var(--text-secondary)">提示词总数</span>
                <span class="text-sm font-semibold" style="color: var(--text-primary)">{{ getTotalCount(categories) }}</span>
              </div>
            </div>
          </div>
          
          <!-- 排序提示 -->
          <div v-if="isEditMode" class="rounded-2xl p-5" style="background: var(--bg-secondary); border: 1px solid var(--border-color);">
            <h4 class="text-sm font-semibold mb-3" style="color: var(--text-primary)">排序说明</h4>
            <div class="space-y-2 text-xs" style="color: var(--text-secondary)">
              <div class="flex items-center gap-2">
                <div class="w-3 h-3 rounded-full bg-[#ea580c]/20"></div>
                <span>拖拽分类可调整顺序</span>
              </div>
              <div class="flex items-center gap-2">
                <div class="w-3 h-3 rounded-full bg-[#ea580c]/20"></div>
                <span>拖放到分类上方：排在前面</span>
              </div>
              <div class="flex items-center gap-2">
                <div class="w-3 h-3 rounded-full bg-[#ea580c]/20"></div>
                <span>拖放到分类下方：排在后面</span>
              </div>
              <div class="flex items-center gap-2">
                <div class="w-3 h-3 rounded-full bg-[#ea580c]/20"></div>
                <span>拖放到分类中间：成为子分类</span>
              </div>
              <div class="flex items-center gap-2">
                <div class="w-3 h-3 rounded-full bg-[#ea580c]/30 border border-[#ea580c]/50"></div>
                <span>拖放到空白区域：成为顶级分类</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="fixed inset-0 z-50 flex items-center justify-center p-4"
      style="background: rgba(0,0,0,0.4); backdrop-filter: blur(4px);"
      @click.self="showModal = false"
    >
      <div class="rounded-2xl p-6 w-full max-w-md animate-scale-in"
        style="background: var(--bg-secondary); border: 1px solid var(--border-color);"
      >
        <h3 class="font-semibold text-lg mb-4" style="color: var(--text-primary)">
          {{ modalMode === 'create' ? '新建分类' : '编辑分类' }}
        </h3>
        <div class="space-y-4">
          <div>
            <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">分类名称</label>
            <input v-model="form.name" type="text"
              class="w-full px-4 py-2.5 rounded-xl text-sm transition-all"
              style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
              placeholder="输入分类名称"
              @focus="($event.target as HTMLElement).style.borderColor = 'var(--accent)'"
              @blur="($event.target as HTMLElement).style.borderColor = 'var(--border-color)'"
            >
          </div>
          <div>
            <label class="block text-xs font-medium mb-2" style="color: var(--text-secondary)">颜色</label>
            <div class="flex gap-2 flex-wrap">
              <div v-for="color in colorOptions" :key="color"
                class="w-6 h-6 rounded-full cursor-pointer transition-transform hover:scale-110"
                :class="form.color === color ? 'ring-2 ring-offset-1' : ''"
                :style="{ background: color }"
                @click="form.color = color"
              ></div>
            </div>
          </div>
        </div>
        <div class="flex gap-3 mt-6">
          <button @click="showModal = false"
            class="flex-1 py-2.5 text-sm font-medium rounded-xl transition-colors hover:bg-[var(--bg-tertiary)]"
            style="color: var(--text-secondary);"
          >
            取消
          </button>
          <button @click="handleSubmit"
            class="flex-1 py-2.5 bg-[#ea580c] hover:bg-[#c2410c] text-white text-sm font-medium rounded-xl transition-all"
          >
            {{ modalMode === 'create' ? '创建' : '更新' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Delete Confirm Dialog -->
    <DeleteConfirmDialog
      v-model="deleteDialogVisible"
      :item-name="deleteTarget?.name"
      @confirm="confirmDelete"
    />
  </MainLayout>
</template>
