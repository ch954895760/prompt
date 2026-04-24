<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Sparkles, Sun, Moon } from 'lucide-vue-next'

const router = useRouter()
const userStore = useUserStore()
const authMode = ref('login')
const loading = ref(false)
const error = ref('')

const loginForm = ref({ email: '', password: '', remember: false })
const registerForm = ref({ username: '', email: '', password: '' })

function toggleTheme() {
  userStore.toggleTheme()
}

function checkTheme() {
  document.documentElement.classList.toggle('dark', userStore.theme === 'dark')
}
checkTheme()

async function handleLogin() {
  loading.value = true
  error.value = ''
  try {
    await userStore.login(loginForm.value.email, loginForm.value.password, loginForm.value.remember)
    router.push('/dashboard')
  } catch (e: any) {
    error.value = e.message || '登录失败'
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  loading.value = true
  error.value = ''
  try {
    await userStore.register(registerForm.value.username, registerForm.value.email, registerForm.value.password)
    router.push('/dashboard')
  } catch (e: any) {
    error.value = e.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center relative overflow-hidden">
    <!-- Decorative background -->
    <div class="absolute inset-0 overflow-hidden pointer-events-none">
      <div class="absolute -top-1/2 -right-1/4 w-[800px] h-[800px] rounded-full bg-gradient-to-br from-[#fb923c]/10 to-transparent blur-3xl"></div>
      <div class="absolute -bottom-1/2 -left-1/4 w-[600px] h-[600px] rounded-full bg-gradient-to-tr from-[#ea580c]/5 to-transparent blur-3xl"></div>
      <div class="absolute inset-0 opacity-[0.03]" style="background-image: radial-gradient(circle, var(--text-primary) 1px, transparent 1px); background-size: 32px 32px;"></div>
    </div>

    <!-- Theme toggle -->
    <button @click="toggleTheme" class="absolute top-6 right-6 w-10 h-10 rounded-xl flex items-center justify-center transition-colors hover:bg-surface-200 dark:hover:bg-surface-800 z-20">
      <Sun v-if="userStore.theme === 'dark'" class="w-5 h-5" style="color: var(--text-secondary)" />
      <Moon v-else class="w-5 h-5" style="color: var(--text-secondary)" />
    </button>

    <div class="relative z-10 w-full max-w-md px-6">
      <div class="text-center mb-10 animate-fade-in">
        <div class="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-[#ea580c] text-white mb-5 shadow-lg shadow-[#ea580c]/20">
          <Sparkles class="w-7 h-7" />
        </div>
        <h1 class="text-3xl font-bold tracking-tight mb-2" style="font-family: 'Outfit', sans-serif; color: var(--text-primary)">Prompt Vault</h1>
        <p class="text-sm" style="color: var(--text-secondary)">你的私人提示词库与创作工作台</p>
      </div>

      <div class="rounded-2xl p-8 animate-scale-in" style="animation-delay: 0.1s; background: var(--bg-secondary); border: 1px solid var(--border-color);">
        <div class="flex gap-2 mb-6 p-1 rounded-xl" style="background: var(--bg-tertiary)">
          <button @click="authMode = 'login'"
            class="flex-1 py-2 text-sm font-medium rounded-lg transition-all"
            :class="authMode === 'login' ? 'bg-surface-800 text-white shadow-sm' : ''"
            :style="authMode === 'login' ? '' : 'color: var(--text-secondary)'"
          >登录</button>
          <button @click="authMode = 'register'"
            class="flex-1 py-2 text-sm font-medium rounded-lg transition-all"
            :class="authMode === 'register' ? 'bg-surface-800 text-white shadow-sm' : ''"
            :style="authMode === 'register' ? '' : 'color: var(--text-secondary)'"
          >注册</button>
        </div>

        <form v-if="authMode === 'login'" @submit.prevent="handleLogin">
          <div class="space-y-4">
            <div>
              <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">邮箱</label>
              <input v-model="loginForm.email" type="email" required class="w-full px-4 py-2.5 rounded-xl text-sm transition-all"
                style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
                placeholder="you@example.com"
                @focus="($event.target as HTMLElement).style.borderColor = 'var(--accent)'"
                @blur="($event.target as HTMLElement).style.borderColor = 'var(--border-color)'"
              >
            </div>
            <div>
              <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">密码</label>
              <input v-model="loginForm.password" type="password" required class="w-full px-4 py-2.5 rounded-xl text-sm transition-all"
                style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
                placeholder="••••••••"
                @focus="($event.target as HTMLElement).style.borderColor = 'var(--accent)'"
                @blur="($event.target as HTMLElement).style.borderColor = 'var(--border-color)'"
              >
            </div>
            <div class="flex items-center justify-between text-xs" style="color: var(--text-secondary)">
              <label class="flex items-center gap-2 cursor-pointer">
                <input v-model="loginForm.remember" type="checkbox" class="rounded border-gray-300">
                <span>记住我</span>
              </label>
              <a href="#" class="hover:underline" style="color: var(--accent)">忘记密码?</a>
            </div>
            <div v-if="error" class="text-xs text-red-500 text-center">{{ error }}</div>
            <button type="submit" :disabled="loading"
              class="w-full py-2.5 bg-[#ea580c] hover:bg-[#c2410c] text-white font-medium rounded-xl transition-all shadow-lg shadow-[#ea580c]/20 hover:shadow-[#ea580c]/30 active:scale-[0.98] disabled:opacity-50"
            >
              {{ loading ? '登录中...' : '登录' }}
            </button>
          </div>
        </form>

        <form v-else @submit.prevent="handleRegister">
          <div class="space-y-4">
            <div>
              <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">用户名</label>
              <input v-model="registerForm.username" type="text" required class="w-full px-4 py-2.5 rounded-xl text-sm transition-all"
                style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
                placeholder="你的名字"
                @focus="($event.target as HTMLElement).style.borderColor = 'var(--accent)'"
                @blur="($event.target as HTMLElement).style.borderColor = 'var(--border-color)'"
              >
            </div>
            <div>
              <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">邮箱</label>
              <input v-model="registerForm.email" type="email" required class="w-full px-4 py-2.5 rounded-xl text-sm transition-all"
                style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
                placeholder="you@example.com"
                @focus="($event.target as HTMLElement).style.borderColor = 'var(--accent)'"
                @blur="($event.target as HTMLElement).style.borderColor = 'var(--border-color)'"
              >
            </div>
            <div>
              <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">密码</label>
              <input v-model="registerForm.password" type="password" required class="w-full px-4 py-2.5 rounded-xl text-sm transition-all"
                style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
                placeholder="••••••••"
                @focus="($event.target as HTMLElement).style.borderColor = 'var(--accent)'"
                @blur="($event.target as HTMLElement).style.borderColor = 'var(--border-color)'"
              >
            </div>
            <div v-if="error" class="text-xs text-red-500 text-center">{{ error }}</div>
            <button type="submit" :disabled="loading"
              class="w-full py-2.5 bg-[#ea580c] hover:bg-[#c2410c] text-white font-medium rounded-xl transition-all shadow-lg shadow-[#ea580c]/20 hover:shadow-[#ea580c]/30 active:scale-[0.98] disabled:opacity-50"
            >
              {{ loading ? '创建中...' : '创建账户' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
