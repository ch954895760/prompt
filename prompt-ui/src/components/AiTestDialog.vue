<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { X, Send, Square, Bot, User } from 'lucide-vue-next'
import { aiTestStream } from '@/api/setting'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'

marked.use({
  renderer: {
    code({ text, lang }) {
      const language = hljs.getLanguage(lang || '') ? lang : 'plaintext'
      const highlighted = hljs.highlight(text, { language: language || 'plaintext' }).value
      return `<pre style="margin: 0.75em 0; border-radius: 8px; overflow-x: auto; background: #1c1917;"><code class="hljs language-${language}" style="font-family: 'JetBrains Mono', Menlo, monospace; font-size: 0.85em; line-height: 1.6; padding: 12px; display: block;">${highlighted}</code></pre>`
    },
    codespan({ text }) {
      return `<code style="font-family: 'JetBrains Mono', Menlo, monospace; font-size: 0.85em; background: var(--bg-tertiary); padding: 2px 6px; border-radius: 4px; color: var(--accent);">${text}</code>`
    }
  }
})

interface Message {
  role: 'user' | 'assistant'
  content: string
  loading?: boolean
}

const props = defineProps<{
  modelValue: boolean
  initialPrompt: string
  providerId?: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const messages = ref<Message[]>([])
const inputMessage = ref('')
const aiLoading = ref(false)
const aiAbort = ref<(() => void) | null>(null)
const messagesContainerRef = ref<HTMLDivElement | null>(null)

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainerRef.value) {
      messagesContainerRef.value.scrollTop = messagesContainerRef.value.scrollHeight
    }
  })
}

function renderMarkdown(content: string): string {
  return marked(content) as string
}

function close() {
  emit('update:modelValue', false)
}

function onMaskClick(e: MouseEvent) {
  if (e.target === e.currentTarget) close()
}

function handleStopTest() {
  if (aiAbort.value) {
    aiAbort.value()
    aiAbort.value = null
  }
  aiLoading.value = false
  const lastMessage = messages.value[messages.value.length - 1]
  if (lastMessage && lastMessage.loading) {
    lastMessage.loading = false
  }
}

function sendMessage() {
  if (!inputMessage.value.trim() || aiLoading.value) return

  const userMessage = inputMessage.value.trim()
  messages.value.push({ role: 'user', content: userMessage })
  inputMessage.value = ''
  scrollToBottom()

  aiLoading.value = true
  messages.value.push({ role: 'assistant', content: '', loading: true })

  const fullContext = messages.value
    .filter(m => !m.loading)
    .map(m => `${m.role === 'user' ? '用户' : 'AI'}: ${m.content}`)
    .join('\n\n')

  let currentContent = ''

  aiAbort.value = aiTestStream(fullContext, {
    onChunk: (chunk: string) => {
      currentContent += chunk
      const lastMessage = messages.value[messages.value.length - 1]
      if (lastMessage) {
        lastMessage.content = currentContent
      }
      scrollToBottom()
    },
    onDone: () => {
      aiLoading.value = false
      aiAbort.value = null
      const lastMessage = messages.value[messages.value.length - 1]
      if (lastMessage) {
        lastMessage.loading = false
      }
      scrollToBottom()
    },
    onError: (error: string) => {
      aiLoading.value = false
      aiAbort.value = null
      const lastMessage = messages.value[messages.value.length - 1]
      if (lastMessage) {
        lastMessage.content = `错误: ${error}`
        lastMessage.loading = false
      }
      scrollToBottom()
    },
  }, props.providerId)
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    messages.value = []
    if (props.initialPrompt) {
      messages.value.push({ role: 'user', content: props.initialPrompt })
      aiLoading.value = true
      messages.value.push({ role: 'assistant', content: '', loading: true })

      let currentContent = ''
      aiAbort.value = aiTestStream(props.initialPrompt, {
        onChunk: (chunk: string) => {
          currentContent += chunk
          const lastMessage = messages.value[messages.value.length - 1]
          if (lastMessage) {
            lastMessage.content = currentContent
          }
          scrollToBottom()
        },
        onDone: () => {
          aiLoading.value = false
          aiAbort.value = null
          const lastMessage = messages.value[messages.value.length - 1]
          if (lastMessage) {
            lastMessage.loading = false
          }
          scrollToBottom()
        },
        onError: (error: string) => {
          aiLoading.value = false
          aiAbort.value = null
          const lastMessage = messages.value[messages.value.length - 1]
          if (lastMessage) {
            lastMessage.content = `错误: ${error}`
            lastMessage.loading = false
          }
          scrollToBottom()
        },
      }, props.providerId)
    }
  } else {
    if (aiAbort.value) {
      aiAbort.value()
      aiAbort.value = null
    }
    messages.value = []
    aiLoading.value = false
  }
})
</script>

<template>
  <Teleport to="body">
    <Transition name="dialog">
      <div v-if="modelValue" class="fixed inset-0 z-[100] flex items-center justify-center" @click="onMaskClick">
        <div class="absolute inset-0 bg-black/50 backdrop-blur-sm transition-opacity dialog-backdrop"></div>

        <div class="relative w-full max-w-[700px] mx-4 rounded-2xl flex flex-col dialog-panel"
          style="background: var(--bg-primary); border: 1px solid var(--border-color); box-shadow: 0 24px 80px rgba(0,0,0,0.25); height: 80vh; max-height: 700px;"
        >
          <div class="flex items-center justify-between px-5 py-4 shrink-0"
            style="border-bottom: 1px solid var(--border-color);"
          >
            <div class="flex items-center gap-3">
              <div class="w-8 h-8 rounded-full flex items-center justify-center"
                style="background: var(--accent-soft);"
              >
                <Bot class="w-4 h-4" style="color: var(--accent);" />
              </div>
              <div>
                <h3 class="text-sm font-semibold" style="color: var(--text-primary)">AI 对话测试</h3>
                <p class="text-xs" style="color: var(--text-muted)">基于当前提示词进行对话</p>
              </div>
            </div>
            <button @click="close"
              class="p-2 rounded-lg transition-colors"
              style="color: var(--text-muted);"
              @mouseenter="($event.currentTarget as HTMLElement).style.background = 'var(--bg-tertiary)'"
              @mouseleave="($event.currentTarget as HTMLElement).style.background = 'transparent'"
            >
              <X class="w-5 h-5" />
            </button>
          </div>

          <div ref="messagesContainerRef" class="flex-1 overflow-y-auto p-5 space-y-4">
            <div v-if="messages.length === 0" class="flex flex-col items-center justify-center h-full text-center">
              <div class="w-16 h-16 rounded-full flex items-center justify-center mb-4"
                style="background: var(--bg-secondary);"
              >
                <Bot class="w-8 h-8" style="color: var(--text-muted);" />
              </div>
              <p class="text-sm" style="color: var(--text-secondary)">点击"测试运行"开始与 AI 对话</p>
            </div>

            <div v-for="(message, index) in messages" :key="index"
              class="flex gap-3"
              :class="message.role === 'user' ? 'flex-row-reverse' : ''"
            >
              <div class="w-8 h-8 rounded-full flex items-center justify-center shrink-0"
                :style="{ background: message.role === 'user' ? 'var(--accent)' : 'var(--accent-soft)' }"
              >
                <User v-if="message.role === 'user'" class="w-4 h-4 text-white" />
                <Bot v-else class="w-4 h-4" style="color: var(--accent);" />
              </div>

              <div class="flex-1 max-w-[80%]"
                :class="message.role === 'user' ? 'text-right' : ''"
              >
                <div class="inline-block rounded-2xl px-4 py-3 text-sm leading-relaxed text-left"
                  :style="{
                    background: message.role === 'user' ? 'var(--accent)' : 'var(--bg-secondary)',
                    color: message.role === 'user' ? 'white' : 'var(--text-primary)',
                    border: message.role === 'user' ? 'none' : '1px solid var(--border-color)'
                  }"
                >
                  <div v-if="message.role === 'assistant'" class="ai-markdown" v-html="renderMarkdown(message.content)"></div>
                  <div v-else>{{ message.content }}</div>

                  <div v-if="message.loading" class="flex items-center gap-2 mt-2">
                    <div class="w-4 h-4 border-2 border-[#ea580c] border-t-transparent rounded-full animate-spin"></div>
                    <span class="text-xs" style="color: var(--text-muted)">生成中...</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="px-5 py-4 shrink-0" style="border-top: 1px solid var(--border-color);">
            <div class="flex gap-3">
              <div class="flex-1 relative">
                <textarea
                  v-model="inputMessage"
                  @keydown="handleKeydown"
                  placeholder="输入消息与 AI 对话..."
                  class="w-full px-4 py-3 pr-12 rounded-xl text-sm resize-none"
                  style="background: var(--bg-secondary); border: 1px solid var(--border-color); color: var(--text-primary); min-height: 48px; max-height: 120px;"
                  :disabled="aiLoading"
                  rows="1"
                ></textarea>
              </div>
              <button v-if="!aiLoading" @click="sendMessage"
                class="px-4 py-2 rounded-xl transition-all flex items-center justify-center"
                :class="inputMessage.trim() ? 'opacity-100' : 'opacity-50'"
                style="background: var(--accent); color: white;"
                :disabled="!inputMessage.trim()"
              >
                <Send class="w-5 h-5" />
              </button>
              <button v-else @click="handleStopTest"
                class="px-4 py-2 rounded-xl transition-all flex items-center justify-center"
                style="background: #dc2626; color: white;"
              >
                <Square class="w-5 h-5" />
              </button>
            </div>
            <p class="text-xs mt-2" style="color: var(--text-muted)">按 Enter 发送，Shift + Enter 换行</p>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.dialog-enter-active,
.dialog-leave-active {
  transition: all 0.2s ease;
}

.dialog-enter-from,
.dialog-leave-to {
  opacity: 0;
}

.dialog-enter-from .dialog-panel,
.dialog-leave-to .dialog-panel {
  opacity: 0;
  transform: scale(0.96) translateY(8px);
}

.dialog-panel {
  transition: all 0.2s ease;
}
</style>

<style>
.ai-markdown h1 {
  font-size: 1.25em;
  font-weight: 700;
  margin: 0.5em 0 0.3em;
  line-height: 1.35;
}
.ai-markdown h2 {
  font-size: 1.1em;
  font-weight: 600;
  margin: 0.5em 0 0.3em;
  line-height: 1.35;
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 0.2em;
}
.ai-markdown h3 {
  font-size: 1em;
  font-weight: 600;
  margin: 0.4em 0 0.2em;
}
.ai-markdown p {
  margin: 0.4em 0;
  line-height: 1.6;
}
.ai-markdown ul, .ai-markdown ol {
  margin: 0.4em 0;
  padding-left: 1.2em;
}
.ai-markdown li {
  margin: 0.2em 0;
  line-height: 1.5;
}
.ai-markdown blockquote {
  margin: 0.5em 0;
  padding: 0.4em 0.6em;
  border-left: 3px solid var(--accent);
  background: var(--bg-primary);
  border-radius: 0 6px 6px 0;
}
.ai-markdown a {
  color: var(--accent);
  text-decoration: none;
}
.ai-markdown a:hover {
  text-decoration: underline;
}
.ai-markdown code {
  font-family: 'JetBrains Mono', Menlo, monospace;
  font-size: 0.85em;
}
.ai-markdown pre {
  margin: 0.5em 0;
}
</style>
