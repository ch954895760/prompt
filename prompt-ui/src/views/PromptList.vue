<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useToastStore } from '@/stores/toast'
import { useI18n } from 'vue-i18n'
import MainLayout from '@/components/MainLayout.vue'
import { getPrompts, deletePrompt, getPromptList, usePrompt, exportPromptsJson, exportPromptsMarkdown, importPrompts } from '@/api/prompt'
import { getCategoryTree } from '@/api/category'
import { getTags } from '@/api/tag'
import type { Prompt, Category, Tag } from '@/types'
import { Plus, LayoutGrid, List, Copy, Pencil, Trash2, Download, Upload, FileText } from 'lucide-vue-next'
import DeleteConfirmDialog from '@/components/DeleteConfirmDialog.vue'
import CategoryTreeSelect from '@/components/CategoryTreeSelect.vue'

const route = useRoute()
const router = useRouter()
const toastStore = useToastStore()
const { t } = useI18n()

const prompts = ref<Prompt[]>([])
const categories = ref<Category[]>([])
const categoryTree = ref<Category[]>([])
const tags = ref<Tag[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(9)
const viewMode = ref<'grid' | 'list'>('grid')
const selectedCategory = ref<number | null>(null)
const selectedTag = ref<number | null>(null)
const sortBy = ref('updatedAt')
const searchKeyword = ref('')
const showCategoryDropdown = ref(false)
const categoryDropdownRef = ref<HTMLElement | null>(null)

async function loadData() {
  loading.value = true
  try {
    const [pRes, cTree, tList] = await Promise.all([
      getPrompts({ current: currentPage.value, size: pageSize.value, categoryId: selectedCategory.value || undefined, tagId: selectedTag.value || undefined, keyword: searchKeyword.value || undefined, sortBy: sortBy.value }),
      getCategoryTree(),
      getTags(),
    ])
    prompts.value = pRes.records
    total.value = pRes.total
    categoryTree.value = cTree
    categories.value = flattenCategories(cTree)
    tags.value = tList
  } catch (e) {
    console.error('[DEBUG] Failed to load prompts:', e)
  } finally {
    loading.value = false
  }
}

// 将树形分类扁平化为列表
function flattenCategories(cats: Category[]): Category[] {
  const result: Category[] = []
  for (const cat of cats) {
    result.push(cat)
    if (cat.children && cat.children.length > 0) {
      result.push(...flattenCategories(cat.children))
    }
  }
  return result
}

// 获取分类的完整路径名称
function getCategoryPathName(catId: number | null): string {
  if (!catId) return t('prompt.allCategories')
  const cat = categories.value.find(c => c.id === catId)
  if (!cat) return t('prompt.allCategories')

  const path: string[] = [cat.name]
  let current = cat
  while (current.parentId) {
    const parent = categories.value.find(c => c.id === current.parentId)
    if (parent) {
      path.unshift(parent.name)
      current = parent
    } else {
      break
    }
  }
  return path.join(' / ')
}

// 选择分类
function selectCategory(id: number | null) {
  selectedCategory.value = id
  showCategoryDropdown.value = false
  currentPage.value = 1
  loadData()
}

// 点击外部关闭下拉框
function handleClickOutside(event: MouseEvent) {
  if (categoryDropdownRef.value && !categoryDropdownRef.value.contains(event.target as Node)) {
    showCategoryDropdown.value = false
  }
}

function goToEditor(id?: number) {
  router.push(id ? `/editor/${id}` : '/editor')
}

function handleDelete(id: number, title: string) {
  deleteTarget.value = { id, title }
  deleteDialogVisible.value = true
}

async function confirmDelete() {
  if (!deleteTarget.value) return
  try {
    await deletePrompt(deleteTarget.value.id)
    await loadData()
    toastStore.success(t('prompt.deleted', { title: deleteTarget.value.title }))
  } catch (e) {
    toastStore.error(t('prompt.deleteFailed'))
  } finally {
    deleteTarget.value = null
  }
}

async function copyPrompt(content: string, title: string, id: number) {
  try {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      await navigator.clipboard.writeText(content)
    } else {
      const textarea = document.createElement('textarea')
      textarea.value = content
      textarea.style.position = 'fixed'
      textarea.style.left = '-9999px'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
    }
    await usePrompt(id, t('prompt.copiedFromList'))
    toastStore.success(t('prompt.copied', { title }))
  } catch (e) {
    toastStore.error(t('common.copyFailed'))
  }
}

function handleCategoryChange(catId: number | null) {
  selectedCategory.value = catId === selectedCategory.value ? null : catId
  currentPage.value = 1
  loadData()
}

function handleTagChange(tagId: number | null) {
  selectedTag.value = tagId === selectedTag.value ? null : tagId
  currentPage.value = 1
  loadData()
}

watch(() => route.query.q, (q) => {
  searchKeyword.value = q as string || ''
  loadData()
})

const deleteDialogVisible = ref(false)
const deleteTarget = ref<{ id: number; title: string } | null>(null)
const importFile = ref<HTMLInputElement | null>(null)

function getPreview(content: string): string {
  return content.substring(0, 80).replace(/\{\{(\w+)\}\}/g, '<span style="color: var(--accent); font-family: monospace; font-size: 0.85em; background: var(--accent-soft); padding: 1px 4px; border-radius: 4px;">{{$1}}</span>') + '...'
}

async function handleExportJson() {
  try {
    const blob = await exportPromptsJson()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `prompts-${new Date().toISOString().slice(0, 10)}.json`
    a.click()
    URL.revokeObjectURL(url)
    toastStore.success(t('prompt.exportJsonSuccess'))
  } catch (e: any) {
    toastStore.error(e.message || t('prompt.exportFailed'))
  }
}

async function handleExportMarkdown() {
  try {
    const blob = await exportPromptsMarkdown()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `prompts-${new Date().toISOString().slice(0, 10)}.md`
    a.click()
    URL.revokeObjectURL(url)
    toastStore.success(t('prompt.exportMarkdownSuccess'))
  } catch (e: any) {
    toastStore.error(e.message || t('prompt.exportFailed'))
  }
}

function handleImportClick() {
  importFile.value?.click()
}

async function handleImportFile(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  try {
    const text = await file.text()
    const data = JSON.parse(text)
    if (!Array.isArray(data)) {
      toastStore.error(t('prompt.importFormatError'))
      return
    }
    const count = await importPrompts(data)
    toastStore.success(t('prompt.importSuccess', { count }))
    await loadData()
  } catch (e: any) {
    toastStore.error(e.message || t('prompt.importFailed'))
  } finally {
    target.value = ''
  }
}

onMounted(() => {
  loadData()
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <MainLayout>
    <div class="animate-fade-in">
      <div class="flex items-center justify-between mb-6">
        <div>
          <h2 class="text-2xl font-bold mb-1" style="color: var(--text-primary)">{{ t('nav.promptLibrary') }}</h2>
          <p class="text-sm" style="color: var(--text-secondary)">{{ t('prompt.manageDescription') }}</p>
        </div>
        <div class="flex items-center gap-2">
          <input ref="importFile" type="file" accept=".json" class="hidden" @change="handleImportFile">
          <button @click="handleImportClick"
            class="flex items-center gap-2 px-3 py-2 text-sm font-medium rounded-xl border transition-all hover:bg-[var(--bg-tertiary)]"
            style="border-color: var(--border-color); color: var(--text-secondary);"
            :title="t('common.import')"
          >
            <Upload class="w-4 h-4" />
            {{ t('common.import') }}
          </button>
          <button @click="handleExportJson"
            class="flex items-center gap-2 px-3 py-2 text-sm font-medium rounded-xl border transition-all hover:bg-[var(--bg-tertiary)]"
            style="border-color: var(--border-color); color: var(--text-secondary);"
            :title="t('prompt.exportJson')"
          >
            <Download class="w-4 h-4" />
            {{ t('prompt.exportJson') }}
          </button>
          <button @click="handleExportMarkdown"
            class="flex items-center gap-2 px-3 py-2 text-sm font-medium rounded-xl border transition-all hover:bg-[var(--bg-tertiary)]"
            style="border-color: var(--border-color); color: var(--text-secondary);"
            :title="t('prompt.exportMarkdown')"
          >
            <Download class="w-4 h-4" />
            {{ t('prompt.exportMarkdown') }}
          </button>
          <button @click="goToEditor()"
            class="flex items-center gap-2 px-4 py-2.5 bg-[#ea580c] hover:bg-[#c2410c] text-white text-sm font-medium rounded-xl transition-all shadow-lg shadow-[#ea580c]/20"
          >
            <Plus class="w-4 h-4" />
            {{ t('nav.newPrompt') }}
          </button>
        </div>
      </div>

      <!-- Filters -->
      <div class="flex flex-wrap items-center gap-3 mb-6 p-4 rounded-xl"
        style="background: var(--bg-secondary); border: 1px solid var(--border-color);"
      >
        <div class="relative" ref="categoryDropdownRef">
          <button
            @click.stop="showCategoryDropdown = !showCategoryDropdown"
            class="px-3 py-2 rounded-lg text-sm min-w-[160px] transition-all text-left flex items-center justify-between"
            style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
            :style="showCategoryDropdown ? 'border-color: var(--accent)' : ''"
          >
            <span class="truncate">{{ getCategoryPathName(selectedCategory) }}</span>
            <svg class="w-4 h-4 transition-transform flex-shrink-0 ml-2" :class="showCategoryDropdown ? 'rotate-180' : ''" style="color: var(--text-muted)" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
            </svg>
          </button>

          <!-- 树形分类下拉框 -->
          <div v-if="showCategoryDropdown"
            class="absolute z-50 w-[240px] mt-1 rounded-xl overflow-hidden shadow-lg"
            style="background: var(--bg-secondary); border: 1px solid var(--border-color); max-height: 320px; overflow-y: auto;"
          >
            <!-- 所有分类选项 -->
            <div
              @click="selectCategory(null)"
              class="px-4 py-2.5 cursor-pointer transition-colors flex items-center gap-2"
              :style="selectedCategory === null ? 'background: var(--accent-soft); color: var(--accent);' : 'color: var(--text-primary);'"
              @mouseenter="($event.currentTarget as HTMLElement).style.background = selectedCategory === null ? 'var(--accent-soft)' : 'var(--bg-tertiary)'"
              @mouseleave="($event.currentTarget as HTMLElement).style.background = selectedCategory === null ? 'var(--accent-soft)' : 'transparent'"
            >
              <span class="text-sm">{{ t('prompt.allCategories') }}</span>
            </div>

            <!-- 树形分类列表 -->
            <CategoryTreeSelect
              v-for="cat in categoryTree" :key="cat.id"
              :category="cat"
              :selected-id="selectedCategory"
              @select="selectCategory"
            />
          </div>
        </div>

        <div class="flex items-center gap-2 flex-wrap">
          <button v-for="t in tags.slice(0, 6)" :key="t.id"
            class="tag-pill px-3 py-1.5 text-xs font-medium rounded-lg border transition-all"
            :class="selectedTag === t.id ? 'border-[#ea580c] text-[#ea580c]' : ''"
            :style="selectedTag === t.id ? '' : 'border-color: var(--border-color); color: var(--text-secondary);'"
            @click="handleTagChange(t.id)"
          >
            #{{ t.name }}
          </button>
        </div>

        <div class="ml-auto flex items-center gap-2">
          <select v-model="sortBy" @change="loadData()"
            class="px-3 py-2 rounded-lg text-sm transition-all"
            style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
          >
            <option value="updatedAt">{{ t('prompt.sortByUpdated') }}</option>
            <option value="usageCount">{{ t('prompt.sortByUsage') }}</option>
            <option value="title">{{ t('prompt.sortByTitle') }}</option>
          </select>
          <div class="flex rounded-lg overflow-hidden border" style="border-color: var(--border-color); background: var(--bg-primary);">
            <button @click="viewMode = 'grid'"
              class="px-3 py-2 transition-colors"
              :class="viewMode === 'grid' ? 'bg-[var(--accent-soft)]' : 'hover:bg-[var(--bg-tertiary)]'"
              :style="viewMode === 'grid' ? 'color: var(--accent)' : 'color: var(--text-muted)'"
            >
              <LayoutGrid class="w-4 h-4" />
            </button>
            <button @click="viewMode = 'list'"
              class="px-3 py-2 transition-colors"
              :class="viewMode === 'list' ? 'bg-[var(--accent-soft)]' : 'hover:bg-[var(--bg-tertiary)]'"
              :style="viewMode === 'list' ? 'color: var(--accent)' : 'color: var(--text-muted)'"
            >
              <List class="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="text-center py-16">
        <div class="inline-block w-8 h-8 border-2 border-[#ea580c] border-t-transparent rounded-full animate-spin"></div>
        <p class="mt-3 text-sm" style="color: var(--text-muted)">{{ t('common.loading') }}</p>
      </div>

      <!-- Empty state -->
      <div v-else-if="prompts.length === 0" class="text-center py-16">
        <FileText class="w-10 h-10 mx-auto mb-3" style="color: var(--text-muted)" />
        <p class="text-sm" style="color: var(--text-muted)">{{ t('prompt.noPrompts') }}</p>
        <button @click="goToEditor()" class="mt-3 text-sm font-medium hover:underline" style="color: var(--accent)">{{ t('prompt.createOne') }}</button>
      </div>

      <!-- Grid view -->
      <div v-else-if="viewMode === 'grid'" class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
        <div v-for="p in prompts" :key="p.id"
          class="rounded-2xl p-5 flex flex-col group transition-all hover:-translate-y-0.5"
          style="background: var(--bg-secondary); border: 1px solid var(--border-color);"
          @mouseenter="($event.currentTarget as HTMLElement).style.borderColor = 'var(--accent)'"
          @mouseleave="($event.currentTarget as HTMLElement).style.borderColor = 'var(--border-color)'"
        >
          <div class="flex items-start justify-between mb-3">
            <div class="flex items-center gap-2">
              <div class="w-2.5 h-2.5 rounded-full" :style="{ background: p.categoryColor || '#d6d3d1' }"></div>
              <span class="text-[10px] font-medium uppercase tracking-wider" style="color: var(--text-muted)">{{ p.categoryName || t('prompt.uncategorized') }}</span>
            </div>
            <div class="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
              <button @click="goToEditor(p.id)"
                class="p-1.5 rounded-lg transition-colors"
                style="color: var(--text-muted);"
                @mouseenter="($event.currentTarget as HTMLElement).style.background = 'var(--bg-tertiary)'"
                @mouseleave="($event.currentTarget as HTMLElement).style.background = 'transparent'"
              >
                <Pencil class="w-3.5 h-3.5" />
              </button>
              <button @click="handleDelete(p.id, p.title)"
                class="p-1.5 rounded-lg transition-colors hover:bg-red-50 dark:hover:bg-red-900/30"
                style="color: var(--text-muted);"
              >
                <Trash2 class="w-3.5 h-3.5 hover:text-red-500" />
              </button>
            </div>
          </div>
          <h3 class="font-semibold mb-2 text-base" style="color: var(--text-primary)">{{ p.title }}</h3>
          <p class="text-sm leading-relaxed mb-4 flex-1" style="color: var(--text-secondary)" v-html="getPreview(p.content)"></p>
          <div class="flex items-center justify-between pt-3" style="border-top: 1px solid var(--border-color)">
            <div class="flex gap-1.5 flex-wrap">
              <span v-for="tag in (p.tags || []).slice(0, 3)" :key="tag.id"
                class="text-[10px] px-2 py-0.5 rounded-md font-medium"
                style="background: var(--bg-tertiary); color: var(--text-muted);"
              >#{{ tag.name }}</span>
            </div>
            <button @click="copyPrompt(p.content, p.title, p.id)"
              class="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-lg transition-all hover:bg-[var(--accent-soft)]"
              style="color: var(--accent);"
            >
              <Copy class="w-3 h-3" />
              {{ t('common.copy') }}
            </button>
          </div>
        </div>
      </div>

      <!-- List view -->
      <div v-else class="flex flex-col gap-3">
        <div v-for="p in prompts" :key="p.id"
          class="rounded-xl p-4 flex items-center gap-4 group transition-all"
          style="background: var(--bg-secondary); border: 1px solid var(--border-color);"
          @mouseenter="($event.currentTarget as HTMLElement).style.borderColor = 'var(--accent)'"
          @mouseleave="($event.currentTarget as HTMLElement).style.borderColor = 'var(--border-color)'"
        >
          <div class="w-2.5 h-2.5 rounded-full flex-shrink-0" :style="{ background: p.categoryColor || '#d6d3d1' }"></div>
          <div class="flex-1 min-w-0">
            <h3 class="font-semibold text-sm mb-0.5 truncate" style="color: var(--text-primary)">{{ p.title }}</h3>
            <p class="text-xs truncate" style="color: var(--text-secondary)">{{ p.content.substring(0, 60) }}...</p>
          </div>
          <div class="flex items-center gap-2 flex-shrink-0">
            <span class="text-[10px] px-2 py-0.5 rounded-md font-medium" style="background: var(--bg-tertiary); color: var(--text-muted)">
              {{ p.categoryName || t('prompt.uncategorized') }}
            </span>
            <div class="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
              <button @click="copyPrompt(p.content, p.title, p.id)"
                class="p-1.5 rounded-lg transition-colors"
                style="color: var(--text-muted);"
                @mouseenter="($event.currentTarget as HTMLElement).style.background = 'var(--bg-tertiary)'"
                @mouseleave="($event.currentTarget as HTMLElement).style.background = 'transparent'"
              >
                <Copy class="w-3.5 h-3.5" />
              </button>
              <button @click="goToEditor(p.id)"
                class="p-1.5 rounded-lg transition-colors"
                style="color: var(--text-muted);"
                @mouseenter="($event.currentTarget as HTMLElement).style.background = 'var(--bg-tertiary)'"
                @mouseleave="($event.currentTarget as HTMLElement).style.background = 'transparent'"
              >
                <Pencil class="w-3.5 h-3.5" />
              </button>
              <button @click="handleDelete(p.id, p.title)"
                class="p-1.5 rounded-lg transition-colors hover:bg-red-50 dark:hover:bg-red-900/30"
                style="color: var(--text-muted);"
              >
                <Trash2 class="w-3.5 h-3.5 hover:text-red-500" />
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <div v-if="total > pageSize" class="flex justify-center mt-8 gap-2">
        <button v-for="page in Math.ceil(total / pageSize)" :key="page"
          @click="currentPage = page; loadData()"
          class="w-8 h-8 rounded-lg text-sm font-medium transition-all"
          :class="currentPage === page ? 'bg-[#ea580c] text-white' : ''"
          :style="currentPage !== page ? 'background: var(--bg-secondary); border: 1px solid var(--border-color); color: var(--text-secondary)' : ''"
        >
          {{ page }}
        </button>
      </div>
    </div>

    <!-- Delete Confirm Dialog -->
    <DeleteConfirmDialog
      v-model="deleteDialogVisible"
      :item-name="deleteTarget?.title"
      @confirm="confirmDelete"
    />
  </MainLayout>
</template>

<style scoped>
.tag-pill:hover {
  transform: scale(1.05);
}
</style>
