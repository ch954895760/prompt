<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MainLayout from '@/components/MainLayout.vue'
import { getPrompts, deletePrompt, getPromptList, usePrompt, exportPromptsJson, exportPromptsMarkdown, importPrompts } from '@/api/prompt'
import { getCategoryList } from '@/api/category'
import { getTags } from '@/api/tag'
import type { Prompt, Category, Tag } from '@/types'
import { Plus, LayoutGrid, List, Copy, Pencil, Trash2, Download, Upload } from 'lucide-vue-next'
import DeleteConfirmDialog from '@/components/DeleteConfirmDialog.vue'

const route = useRoute()
const router = useRouter()

const prompts = ref<Prompt[]>([])
const categories = ref<Category[]>([])
const tags = ref<Tag[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const viewMode = ref<'grid' | 'list'>('grid')
const selectedCategory = ref<number | null>(null)
const selectedTag = ref<number | null>(null)
const sortBy = ref('updatedAt')
const searchKeyword = ref('')

async function loadData() {
  loading.value = true
  try {
    const [pRes, cList, tList] = await Promise.all([
      getPrompts({ current: currentPage.value, size: pageSize.value, categoryId: selectedCategory.value || undefined, tagId: selectedTag.value || undefined, keyword: searchKeyword.value || undefined }),
      getCategoryList(),
      getTags(),
    ])
    prompts.value = pRes.records
    total.value = pRes.total
    categories.value = cList
    tags.value = tList
  } catch (e) {
    console.error('[DEBUG] Failed to load prompts:', e)
  } finally {
    loading.value = false
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
    showToast(`"${deleteTarget.value.title}" 已删除`)
  } catch (e) {
    showToast('删除失败')
  } finally {
    deleteTarget.value = null
  }
}

async function copyPrompt(content: string, title: string, id: number) {
  await navigator.clipboard.writeText(content)
  await usePrompt(id)
  showToast(`"${title}" 已复制`)
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
    showToast('JSON 导出成功')
  } catch (e: any) {
    showToast(e.message || '导出失败')
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
    showToast('Markdown 导出成功')
  } catch (e: any) {
    showToast(e.message || '导出失败')
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
      showToast('文件格式错误：应为 JSON 数组')
      return
    }
    const count = await importPrompts(data)
    showToast(`成功导入 ${count} 条提示词`)
    await loadData()
  } catch (e: any) {
    showToast(e.message || '导入失败')
  } finally {
    target.value = ''
  }
}

onMounted(loadData)
</script>

<template>
  <MainLayout>
    <div class="animate-fade-in">
      <div class="flex items-center justify-between mb-6">
        <div>
          <h2 class="text-2xl font-bold mb-1" style="color: var(--text-primary)">提示词库</h2>
          <p class="text-sm" style="color: var(--text-secondary)">管理你的所有提示词模板</p>
        </div>
        <div class="flex items-center gap-2">
          <input ref="importFile" type="file" accept=".json" class="hidden" @change="handleImportFile">
          <button @click="handleImportClick"
            class="flex items-center gap-2 px-3 py-2 text-sm font-medium rounded-xl border transition-all hover:bg-surface-100 dark:hover:bg-surface-800"
            style="border-color: var(--border-color); color: var(--text-secondary);"
            title="导入数据"
          >
            <Upload class="w-4 h-4" />
            导入
          </button>
          <button @click="handleExportJson"
            class="flex items-center gap-2 px-3 py-2 text-sm font-medium rounded-xl border transition-all hover:bg-surface-100 dark:hover:bg-surface-800"
            style="border-color: var(--border-color); color: var(--text-secondary);"
            title="导出 JSON"
          >
            <Download class="w-4 h-4" />
            导出 JSON
          </button>
          <button @click="handleExportMarkdown"
            class="flex items-center gap-2 px-3 py-2 text-sm font-medium rounded-xl border transition-all hover:bg-surface-100 dark:hover:bg-surface-800"
            style="border-color: var(--border-color); color: var(--text-secondary);"
            title="导出 Markdown"
          >
            <Download class="w-4 h-4" />
            导出 Markdown
          </button>
          <button @click="goToEditor()"
            class="flex items-center gap-2 px-4 py-2.5 bg-[#ea580c] hover:bg-[#c2410c] text-white text-sm font-medium rounded-xl transition-all shadow-lg shadow-[#ea580c]/20"
          >
            <Plus class="w-4 h-4" />
            新建提示词
          </button>
        </div>
      </div>

      <!-- Filters -->
      <div class="flex flex-wrap items-center gap-3 mb-6 p-4 rounded-xl"
        style="background: var(--bg-secondary); border: 1px solid var(--border-color);"
      >
        <select v-model="selectedCategory" @change="loadData()"
          class="px-3 py-2 rounded-lg text-sm min-w-[140px] transition-all"
          style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
        >
          <option :value="null">所有分类</option>
          <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
        </select>

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
            <option value="updatedAt">最近更新</option>
            <option value="usageCount">使用最多</option>
            <option value="title">名称排序</option>
          </select>
          <div class="flex rounded-lg overflow-hidden border" style="border-color: var(--border-color)">
            <button @click="viewMode = 'grid'"
              class="px-3 py-2 transition-colors"
              :class="viewMode === 'grid' ? 'bg-[#fff7ed] dark:bg-[#451a03]' : ''"
              :style="viewMode === 'grid' ? 'color: var(--accent)' : 'color: var(--text-muted)'"
            >
              <LayoutGrid class="w-4 h-4" />
            </button>
            <button @click="viewMode = 'list'"
              class="px-3 py-2 transition-colors"
              :class="viewMode === 'list' ? 'bg-[#fff7ed] dark:bg-[#451a03]' : ''"
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
        <p class="mt-3 text-sm" style="color: var(--text-muted)">加载中...</p>
      </div>

      <!-- Empty state -->
      <div v-else-if="prompts.length === 0" class="text-center py-16">
        <FileText class="w-10 h-10 mx-auto mb-3" style="color: var(--text-muted)" />
        <p class="text-sm" style="color: var(--text-muted)">暂无提示词</p>
        <button @click="goToEditor()" class="mt-3 text-sm font-medium hover:underline" style="color: var(--accent)">创建一个</button>
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
              <span class="text-[10px] font-medium uppercase tracking-wider" style="color: var(--text-muted)">{{ p.categoryName || '未分类' }}</span>
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
              class="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-lg transition-all hover:bg-[#fff7ed] dark:hover:bg-[#451a03]"
              style="color: var(--accent);"
            >
              <Copy class="w-3 h-3" />
              复制
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
              {{ p.categoryName || '未分类' }}
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
.tag-pill:hover {
  transform: scale(1.05);
}
</style>
