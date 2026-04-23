<script setup lang="ts">
import { ref, onMounted } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import CategoryNode from '@/components/CategoryNode.vue'
import { getCategoryTree, createCategory, updateCategory, deleteCategory } from '@/api/category'
import type { Category } from '@/types'
import { Plus, Pencil, Trash2, ChevronRight, ChevronDown } from 'lucide-vue-next'
import DeleteConfirmDialog from '@/components/DeleteConfirmDialog.vue'

const categories = ref<Category[]>([])
const loading = ref(false)
const showModal = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const expandedIds = ref<Set<number>>(new Set())

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
  form.value = { name: cat.name, parentId: cat.parentId, color: cat.color, icon: cat.icon || '' }
  showModal.value = true
}

async function handleSubmit() {
  if (!form.value.name.trim()) {
    showToast('请输入分类名称')
    return
  }
  try {
    if (modalMode.value === 'edit' && editingId.value) {
      await updateCategory(editingId.value, form.value)
      showToast('分类已更新')
    } else {
      await createCategory(form.value)
      showToast('分类已创建')
    }
    showModal.value = false
    await loadData()
  } catch (e: any) {
    showToast(e.message || '操作失败')
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
    showToast(`"${deleteTarget.value.name}" 已删除`)
    await loadData()
  } catch (e: any) {
    showToast(e.message || '删除失败')
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

const toastVisible = ref(false)
const toastMessage = ref('')
let toastTimer: ReturnType<typeof setTimeout>

function showToast(message: string) {
  toastMessage.value = message
  toastVisible.value = true
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toastVisible.value = false
  }, 2500)
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
        <button @click="openCreateModal()"
          class="flex items-center gap-2 px-4 py-2.5 bg-[#ea580c] hover:bg-[#c2410c] text-white text-sm font-medium rounded-xl transition-all shadow-lg shadow-[#ea580c]/20"
        >
          <Plus class="w-4 h-4" />
          新建分类
        </button>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- Category tree -->
        <div class="lg:col-span-2 rounded-2xl p-6" style="background: var(--bg-secondary); border: 1px solid var(--border-color);">
          <div v-if="loading" class="text-center py-8" style="color: var(--text-muted)">加载中...</div>
          <div v-else-if="categories.length === 0" class="text-center py-8" style="color: var(--text-muted)">暂无分类</div>
          <div v-else class="space-y-1">
            <CategoryNode v-for="cat in categories.filter(c => !c.parentId)" :key="cat.id"
              :category="cat"
              :expanded-ids="expandedIds"
              @toggle="toggleExpand"
              @edit="openEditModal"
              @delete="handleDelete"
              @add-child="openCreateModal"
            />
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
                :style="{ background: color, ringColor: 'var(--text-primary)' }"
                @click="form.color = color"
              ></div>
            </div>
          </div>
        </div>
        <div class="flex gap-3 mt-6">
          <button @click="showModal = false"
            class="flex-1 py-2.5 text-sm font-medium rounded-xl transition-colors hover:bg-surface-200 dark:hover:bg-surface-800"
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

    <!-- Toast -->
    <div v-if="toastVisible"
      class="fixed bottom-6 right-6 px-5 py-3 rounded-xl flex items-center gap-2.5 z-50"
      style="background: var(--bg-secondary); border: 1px solid var(--border-color); box-shadow: 0 12px 40px rgba(0,0,0,0.15); animation: slideUp 0.3s ease;"
    >
      <div class="w-5 h-5 rounded-full bg-emerald-500/10 flex items-center justify-center">
        <div class="w-2 h-2 rounded-full bg-emerald-500"></div>
      </div>
      <span class="text-sm" style="color: var(--text-primary)">{{ toastMessage }}</span>
    </div>
  </MainLayout>
</template>

<style scoped>
@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
