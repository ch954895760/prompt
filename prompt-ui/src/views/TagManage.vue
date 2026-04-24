<script setup lang="ts">
import { ref, onMounted } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import { getTags, createTag, updateTag, deleteTag } from '@/api/tag'
import type { Tag } from '@/types'
import { Plus, Pencil, Trash2 } from 'lucide-vue-next'
import DeleteConfirmDialog from '@/components/DeleteConfirmDialog.vue'

const tags = ref<Tag[]>([])
const loading = ref(false)
const showModal = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)

const form = ref({ name: '', color: '#ea580c' })

const colorOptions = ['#ea580c', '#3b82f6', '#8b5cf6', '#10b981', '#f59e0b', '#ef4444', '#ec4899', '#6366f1']

async function loadData() {
  loading.value = true
  try {
    tags.value = await getTags()
  } catch (e) {
    console.error('[DEBUG] Failed to load tags:', e)
  } finally {
    loading.value = false
  }
}

function openCreateModal() {
  modalMode.value = 'create'
  editingId.value = null
  form.value = { name: '', color: '#ea580c' }
  showModal.value = true
}

function openEditModal(tag: Tag) {
  modalMode.value = 'edit'
  editingId.value = tag.id
  form.value = { name: tag.name, color: tag.color }
  showModal.value = true
}

async function handleSubmit() {
  if (!form.value.name.trim()) {
    showToast('请输入标签名称')
    return
  }
  try {
    if (modalMode.value === 'edit' && editingId.value) {
      await updateTag(editingId.value, form.value)
      showToast('标签已更新')
    } else {
      await createTag(form.value)
      showToast('标签已创建')
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
    await deleteTag(deleteTarget.value.id)
    showToast(`"${deleteTarget.value.name}" 已删除`)
    await loadData()
  } catch (e: any) {
    showToast(e.message || '删除失败')
  } finally {
    deleteTarget.value = null
  }
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
          <h2 class="text-2xl font-bold mb-1" style="color: var(--text-primary)">标签管理</h2>
          <p class="text-sm" style="color: var(--text-secondary)">管理你的提示词标签</p>
        </div>
        <button @click="openCreateModal()"
          class="flex items-center gap-2 px-4 py-2.5 bg-[#ea580c] hover:bg-[#c2410c] text-white text-sm font-medium rounded-xl transition-all shadow-lg shadow-[#ea580c]/20"
        >
          <Plus class="w-4 h-4" />
          新建标签
        </button>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- Tag list -->
        <div class="lg:col-span-2 rounded-2xl p-6" style="background: var(--bg-secondary); border: 1px solid var(--border-color);">
          <div v-if="loading" class="text-center py-8" style="color: var(--text-muted)">加载中...</div>
          <div v-else-if="tags.length === 0" class="text-center py-8" style="color: var(--text-muted)">暂无标签</div>
          <div v-else class="flex flex-wrap gap-3">
            <div v-for="tag in tags" :key="tag.id"
              class="group flex items-center justify-between gap-4 px-5 py-2.5 rounded-xl text-sm font-medium transition-all hover:shadow-md min-w-[140px]"
              :style="{ background: tag.color + '18', color: tag.color, border: `1px solid ${tag.color}30` }"
            >
              <span class="truncate">{{ tag.name }}</span>
              <div class="flex items-center gap-1 flex-shrink-0">
                <button @click="openEditModal(tag)"
                  class="p-1 rounded-md hover:bg-black/10 transition-colors"
                >
                  <Pencil class="w-3.5 h-3.5" />
                </button>
                <button @click="handleDelete(tag.id, tag.name)"
                  class="p-1 rounded-md hover:bg-black/10 transition-colors"
                >
                  <Trash2 class="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Quick stats -->
        <div class="space-y-5">
          <div class="rounded-2xl p-5" style="background: var(--bg-secondary); border: 1px solid var(--border-color);">
            <h4 class="text-sm font-semibold mb-4" style="color: var(--text-primary)">标签统计</h4>
            <div class="space-y-3">
              <div class="flex items-center justify-between">
                <span class="text-xs" style="color: var(--text-secondary)">标签总数</span>
                <span class="text-sm font-semibold" style="color: var(--text-primary)">{{ tags.length }}</span>
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
          {{ modalMode === 'create' ? '新建标签' : '编辑标签' }}
        </h3>
        <div class="space-y-4">
          <div>
            <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">标签名称</label>
            <input v-model="form.name" type="text"
              class="w-full px-4 py-2.5 rounded-xl text-sm transition-all"
              style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
              placeholder="输入标签名称"
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
