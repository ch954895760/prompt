<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useToastStore } from '@/stores/toast'
import MainLayout from '@/components/MainLayout.vue'
import { getTags, createTag, updateTag, deleteTag } from '@/api/tag'
import type { Tag } from '@/types'
import { Plus, Pencil, Trash2 } from 'lucide-vue-next'
import DeleteConfirmDialog from '@/components/DeleteConfirmDialog.vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const toastStore = useToastStore()

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
    toastStore.warning(t('tag.nameRequired'))
    return
  }
  try {
    if (modalMode.value === 'edit' && editingId.value) {
      await updateTag(editingId.value, form.value)
      toastStore.success(t('tag.updateSuccess'))
    } else {
      await createTag(form.value)
      toastStore.success(t('tag.createSuccess'))
    }
    showModal.value = false
    await loadData()
  } catch (e: any) {
    toastStore.error(e.message || t('tag.operationFailed'))
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
    toastStore.success(t('tag.deleteSuccess', { name: deleteTarget.value.name }))
    await loadData()
  } catch (e: any) {
    toastStore.error(e.message || t('tag.deleteFailed'))
  } finally {
    deleteTarget.value = null
  }
}

onMounted(loadData)
</script>

<template>
  <MainLayout>
    <div class="animate-fade-in">
      <div class="flex items-center justify-between mb-6">
        <div>
          <h2 class="text-2xl font-bold mb-1" style="color: var(--text-primary)">{{ t('nav.tagManage') }}</h2>
          <p class="text-sm" style="color: var(--text-secondary)">{{ t('tag.manageTags') }}</p>
        </div>
        <button @click="openCreateModal()"
          class="flex items-center gap-2 px-4 py-2.5 bg-[#ea580c] hover:bg-[#c2410c] text-white text-sm font-medium rounded-xl transition-all shadow-lg shadow-[#ea580c]/20"
        >
          <Plus class="w-4 h-4" />
          {{ t('tag.newTag') }}
        </button>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- Tag list -->
        <div class="lg:col-span-2 rounded-2xl p-6" style="background: var(--bg-secondary); border: 1px solid var(--border-color);">
          <div v-if="loading" class="text-center py-8" style="color: var(--text-muted)">{{ t('common.loading') }}</div>
          <div v-else-if="tags.length === 0" class="text-center py-8" style="color: var(--text-muted)">{{ t('tag.noTags') }}</div>
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
            <h4 class="text-sm font-semibold mb-4" style="color: var(--text-primary)">{{ t('tag.stats') }}</h4>
            <div class="space-y-3">
              <div class="flex items-center justify-between">
                <span class="text-xs" style="color: var(--text-secondary)">{{ t('tag.totalTags') }}</span>
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
          {{ modalMode === 'create' ? t('tag.createTitle') : t('tag.editTitle') }}
        </h3>
        <div class="space-y-4">
          <div>
            <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">{{ t('tag.name') }}</label>
            <input v-model="form.name" type="text"
              class="w-full px-4 py-2.5 rounded-xl text-sm transition-all"
              style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
              :placeholder="t('tag.namePlaceholder')"
              @focus="($event.target as HTMLElement).style.borderColor = 'var(--accent)'"
              @blur="($event.target as HTMLElement).style.borderColor = 'var(--border-color)'"
            >
          </div>
          <div>
            <label class="block text-xs font-medium mb-2" style="color: var(--text-secondary)">{{ t('tag.color') }}</label>
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
            {{ t('common.cancel') }}
          </button>
          <button @click="handleSubmit"
            class="flex-1 py-2.5 bg-[#ea580c] hover:bg-[#c2410c] text-white text-sm font-medium rounded-xl transition-all"
          >
            {{ modalMode === 'create' ? t('common.create') : t('common.update') }}
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
