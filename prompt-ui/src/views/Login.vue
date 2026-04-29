<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useToastStore } from '@/stores/toast'
import { Sparkles, Sun, Moon, Check, X, Eye, EyeOff } from 'lucide-vue-next'

const router = useRouter()
const userStore = useUserStore()
const toastStore = useToastStore()
const authMode = ref('login')
const loading = ref(false)
const shakeForm = ref(false)

// 登录表单
const loginForm = ref({ email: '', password: '', remember: false })
const showLoginPassword = ref(false)

// 注册表单
const registerForm = ref({ username: '', email: '', password: '', confirmPassword: '' })
const showRegisterPassword = ref(false)
const showConfirmPassword = ref(false)

// 表单验证状态
const loginTouched = ref({ email: false, password: false })
const registerTouched = ref({ username: false, email: false, password: false, confirmPassword: false })

// 验证规则
const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const passwordMinLength = 6

// 登录表单验证
const loginErrors = computed(() => {
  const errors: { email?: string; password?: string } = {}
  
  if (loginTouched.value.email) {
    if (!loginForm.value.email) {
      errors.email = '请输入邮箱'
    } else if (!emailRegex.test(loginForm.value.email)) {
      errors.email = '请输入有效的邮箱地址'
    }
  }
  
  if (loginTouched.value.password) {
    if (!loginForm.value.password) {
      errors.password = '请输入密码'
    } else if (loginForm.value.password.length < passwordMinLength) {
      errors.password = `密码长度至少${passwordMinLength}位`
    }
  }
  
  return errors
})

const loginValid = computed(() => {
  return emailRegex.test(loginForm.value.email) && 
         loginForm.value.password.length >= passwordMinLength
})

// 注册表单验证
const registerErrors = computed(() => {
  const errors: { 
    username?: string; 
    email?: string; 
    password?: string; 
    confirmPassword?: string 
  } = {}
  
  if (registerTouched.value.username) {
    if (!registerForm.value.username) {
      errors.username = '请输入用户名'
    } else if (registerForm.value.username.length < 2) {
      errors.username = '用户名至少2个字符'
    } else if (registerForm.value.username.length > 20) {
      errors.username = '用户名最多20个字符'
    }
  }
  
  if (registerTouched.value.email) {
    if (!registerForm.value.email) {
      errors.email = '请输入邮箱'
    } else if (!emailRegex.test(registerForm.value.email)) {
      errors.email = '请输入有效的邮箱地址'
    }
  }
  
  if (registerTouched.value.password) {
    if (!registerForm.value.password) {
      errors.password = '请输入密码'
    } else if (registerForm.value.password.length < passwordMinLength) {
      errors.password = `密码长度至少${passwordMinLength}位`
    }
  }
  
  if (registerTouched.value.confirmPassword) {
    if (!registerForm.value.confirmPassword) {
      errors.confirmPassword = '请确认密码'
    } else if (registerForm.value.password !== registerForm.value.confirmPassword) {
      errors.confirmPassword = '两次输入的密码不一致'
    }
  }
  
  return errors
})

const registerValid = computed(() => {
  return registerForm.value.username.length >= 2 &&
         emailRegex.test(registerForm.value.email) &&
         registerForm.value.password.length >= passwordMinLength &&
         registerForm.value.password === registerForm.value.confirmPassword
})

// 切换模式时重置错误
watch(authMode, () => {
  loginTouched.value = { email: false, password: false }
  registerTouched.value = { username: false, email: false, password: false, confirmPassword: false }
})

function toggleTheme() {
  userStore.toggleTheme()
}

function checkTheme() {
  document.documentElement.classList.toggle('dark', userStore.theme === 'dark')
}
checkTheme()

// 输入框失焦时标记为已触碰
function handleLoginBlur(field: 'email' | 'password') {
  loginTouched.value[field] = true
}

function handleRegisterBlur(field: 'username' | 'email' | 'password' | 'confirmPassword') {
  registerTouched.value[field] = true
}

// 密码输入时实时检查确认密码
watch(() => registerForm.value.password, () => {
  if (registerTouched.value.confirmPassword && registerForm.value.confirmPassword) {
    // 触发确认密码的验证
    registerTouched.value.confirmPassword = true
  }
})

async function handleLogin() {
  // 提交时标记所有字段为已触碰
  loginTouched.value = { email: true, password: true }

  if (!loginValid.value) return

  loading.value = true
  try {
    await userStore.login(loginForm.value.email, loginForm.value.password, loginForm.value.remember)
    toastStore.success('登录成功')
    router.push('/dashboard')
  } catch (e: any) {
    toastStore.error(e.message || '登录失败')
    // 触发表单晃动效果
    shakeForm.value = true
    setTimeout(() => {
      shakeForm.value = false
    }, 500)
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  // 提交时标记所有字段为已触碰
  registerTouched.value = { username: true, email: true, password: true, confirmPassword: true }

  if (!registerValid.value) return

  loading.value = true

  try {
    await userStore.register(registerForm.value.username, registerForm.value.email, registerForm.value.password)
    toastStore.success('注册成功')
    router.push('/dashboard')
  } catch (e: any) {
    toastStore.error(e.message || '注册失败')
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
    <button @click="toggleTheme" class="absolute top-6 right-6 w-10 h-10 rounded-xl flex items-center justify-center transition-colors hover:bg-[var(--bg-tertiary)] z-20">
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

      <div class="animate-scale-in" style="animation-delay: 0.1s;">
        <div class="rounded-2xl p-8" style="background: var(--bg-secondary); border: 1px solid var(--border-color);">
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
            <!-- 邮箱输入 -->
            <div :class="{ 'shake-animation': shakeForm }">
              <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">邮箱</label>
              <div class="relative">
                <input 
                  v-model="loginForm.email" 
                  type="email" 
                  class="w-full px-4 py-2.5 rounded-xl text-sm transition-all pr-10"
                  :class="{ 
                    'border-red-500': loginErrors.email,
                    'border-emerald-500': loginForm.email && !loginErrors.email && loginTouched.email
                  }"
                  style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
                  :style="loginErrors.email ? 'border-color: #ef4444;' : (loginForm.email && !loginErrors.email && loginTouched.email ? 'border-color: #10b981;' : '')"
                  placeholder="请输入邮箱，例如you@example.com"
                  @blur="handleLoginBlur('email')"
                >
                <!-- 验证状态图标 -->
                <div class="absolute right-3 top-1/2 -translate-y-1/2 flex items-center">
                  <Check v-if="loginForm.email && !loginErrors.email && loginTouched.email" class="w-4 h-4 text-emerald-500" />
                  <X v-else-if="loginErrors.email" class="w-4 h-4 text-red-500" />
                </div>
              </div>
              <!-- 错误提示 -->
              <p v-if="loginErrors.email" class="mt-1.5 text-xs text-red-500 flex items-center gap-1">
                <X class="w-3 h-3" />
                {{ loginErrors.email }}
              </p>
            </div>

            <!-- 密码输入 -->
            <div :class="{ 'shake-animation': shakeForm }">
              <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">密码</label>
              <div class="relative">
                <input 
                  v-model="loginForm.password" 
                  :type="showLoginPassword ? 'text' : 'password'" 
                  class="w-full px-4 py-2.5 rounded-xl text-sm transition-all pr-20"
                  :class="{ 
                    'border-red-500': loginErrors.password,
                    'border-emerald-500': loginForm.password && !loginErrors.password && loginTouched.password
                  }"
                  :style="loginErrors.password ? 'border-color: #ef4444;' : (loginForm.password && !loginErrors.password && loginTouched.password ? 'border-color: #10b981;' : '')"
                  style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
                  placeholder="请输入密码"
                  @blur="handleLoginBlur('password')"
                >
                <!-- 显示密码按钮和验证状态 -->
                <div class="absolute right-3 top-1/2 -translate-y-1/2 flex items-center gap-2">
                  <button 
                    type="button"
                    @click="showLoginPassword = !showLoginPassword"
                    class="p-1 rounded hover:bg-[var(--bg-tertiary)] transition-colors"
                    style="color: var(--text-muted)"
                  >
                    <Eye v-if="!showLoginPassword" class="w-4 h-4" />
                    <EyeOff v-else class="w-4 h-4" />
                  </button>
                  <Check v-if="loginForm.password && !loginErrors.password && loginTouched.password" class="w-4 h-4 text-emerald-500" />
                  <X v-else-if="loginErrors.password" class="w-4 h-4 text-red-500" />
                </div>
              </div>
              <!-- 错误提示 -->
              <p v-if="loginErrors.password" class="mt-1.5 text-xs text-red-500 flex items-center gap-1">
                <X class="w-3 h-3" />
                {{ loginErrors.password }}
              </p>
            </div>

            <div class="flex items-center justify-between text-xs" style="color: var(--text-secondary)">
              <label class="flex items-center gap-2 cursor-pointer">
                <input v-model="loginForm.remember" type="checkbox" class="rounded border-gray-300">
                <span>记住我</span>
              </label>
              <a href="#" class="hover:underline" style="color: var(--accent)">忘记密码?</a>
            </div>

            <button
              type="submit"
              :disabled="loading || !loginValid"
              class="w-full py-2.5 bg-[#ea580c] hover:bg-[#c2410c] text-white font-medium rounded-xl transition-all shadow-lg shadow-[#ea580c]/20 hover:shadow-[#ea580c]/30 active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {{ loading ? '登录中...' : '登录' }}
            </button>
          </div>
        </form>

        <form v-else @submit.prevent="handleRegister">
          <div class="space-y-4">
            <!-- 用户名输入 -->
            <div>
              <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">用户名</label>
              <div class="relative">
                <input 
                  v-model="registerForm.username" 
                  type="text" 
                  class="w-full px-4 py-2.5 rounded-xl text-sm transition-all pr-10"
                  :class="{ 
                    'border-red-500': registerErrors.username,
                    'border-emerald-500': registerForm.username && !registerErrors.username && registerTouched.username
                  }"
                  :style="registerErrors.username ? 'border-color: #ef4444;' : (registerForm.username && !registerErrors.username && registerTouched.username ? 'border-color: #10b981;' : '')"
                  style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
                  placeholder="请输入用户名"
                  @blur="handleRegisterBlur('username')"
                >
                <div class="absolute right-3 top-1/2 -translate-y-1/2 flex items-center">
                  <Check v-if="registerForm.username && !registerErrors.username && registerTouched.username" class="w-4 h-4 text-emerald-500" />
                  <X v-else-if="registerErrors.username" class="w-4 h-4 text-red-500" />
                </div>
              </div>
              <p v-if="registerErrors.username" class="mt-1.5 text-xs text-red-500 flex items-center gap-1">
                <X class="w-3 h-3" />
                {{ registerErrors.username }}
              </p>
            </div>

            <!-- 邮箱输入 -->
            <div>
              <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">邮箱</label>
              <div class="relative">
                <input 
                  v-model="registerForm.email" 
                  type="email" 
                  class="w-full px-4 py-2.5 rounded-xl text-sm transition-all pr-10"
                  :class="{ 
                    'border-red-500': registerErrors.email,
                    'border-emerald-500': registerForm.email && !registerErrors.email && registerTouched.email
                  }"
                  :style="registerErrors.email ? 'border-color: #ef4444;' : (registerForm.email && !registerErrors.email && registerTouched.email ? 'border-color: #10b981;' : '')"
                  style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
                  placeholder="请输入邮箱，例如you@example.com"
                  @blur="handleRegisterBlur('email')"
                >
                <div class="absolute right-3 top-1/2 -translate-y-1/2 flex items-center">
                  <Check v-if="registerForm.email && !registerErrors.email && registerTouched.email" class="w-4 h-4 text-emerald-500" />
                  <X v-else-if="registerErrors.email" class="w-4 h-4 text-red-500" />
                </div>
              </div>
              <p v-if="registerErrors.email" class="mt-1.5 text-xs text-red-500 flex items-center gap-1">
                <X class="w-3 h-3" />
                {{ registerErrors.email }}
              </p>
            </div>

            <!-- 密码输入 -->
            <div>
              <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">密码</label>
              <div class="relative">
                <input 
                  v-model="registerForm.password" 
                  :type="showRegisterPassword ? 'text' : 'password'" 
                  class="w-full px-4 py-2.5 rounded-xl text-sm transition-all pr-20"
                  :class="{ 
                    'border-red-500': registerErrors.password,
                    'border-emerald-500': registerForm.password && !registerErrors.password && registerTouched.password
                  }"
                  :style="registerErrors.password ? 'border-color: #ef4444;' : (registerForm.password && !registerErrors.password && registerTouched.password ? 'border-color: #10b981;' : '')"
                  style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
                  placeholder="请输入密码"
                  @blur="handleRegisterBlur('password')"
                >
                <div class="absolute right-3 top-1/2 -translate-y-1/2 flex items-center gap-2">
                  <button 
                    type="button"
                    @click="showRegisterPassword = !showRegisterPassword"
                    class="p-1 rounded hover:bg-[var(--bg-tertiary)] transition-colors"
                    style="color: var(--text-muted)"
                  >
                    <Eye v-if="!showRegisterPassword" class="w-4 h-4" />
                    <EyeOff v-else class="w-4 h-4" />
                  </button>
                  <Check v-if="registerForm.password && !registerErrors.password && registerTouched.password" class="w-4 h-4 text-emerald-500" />
                  <X v-else-if="registerErrors.password" class="w-4 h-4 text-red-500" />
                </div>
              </div>
              <p v-if="registerErrors.password" class="mt-1.5 text-xs text-red-500 flex items-center gap-1">
                <X class="w-3 h-3" />
                {{ registerErrors.password }}
              </p>
              <!-- 密码强度提示 -->
              <div v-else-if="registerForm.password" class="mt-1.5 flex items-center gap-1">
                <div class="flex-1 h-1 rounded-full bg-gray-200 dark:bg-gray-700 overflow-hidden">
                  <div 
                    class="h-full rounded-full transition-all duration-300"
                    :class="{
                      'w-1/3 bg-red-500': registerForm.password.length < 8,
                      'w-2/3 bg-yellow-500': registerForm.password.length >= 8 && registerForm.password.length < 12,
                      'w-full bg-emerald-500': registerForm.password.length >= 12
                    }"
                  ></div>
                </div>
                <span class="text-[10px]" :class="{
                  'text-red-500': registerForm.password.length < 8,
                  'text-yellow-500': registerForm.password.length >= 8 && registerForm.password.length < 12,
                  'text-emerald-500': registerForm.password.length >= 12
                }">
                  {{ registerForm.password.length < 8 ? '弱' : registerForm.password.length < 12 ? '中' : '强' }}
                </span>
              </div>
            </div>

            <!-- 确认密码输入 -->
            <div>
              <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">确认密码</label>
              <div class="relative">
                <input 
                  v-model="registerForm.confirmPassword" 
                  :type="showConfirmPassword ? 'text' : 'password'" 
                  class="w-full px-4 py-2.5 rounded-xl text-sm transition-all pr-20"
                  :class="{ 
                    'border-red-500': registerErrors.confirmPassword,
                    'border-emerald-500': registerForm.confirmPassword && !registerErrors.confirmPassword && registerTouched.confirmPassword
                  }"
                  :style="registerErrors.confirmPassword ? 'border-color: #ef4444;' : (registerForm.confirmPassword && !registerErrors.confirmPassword && registerTouched.confirmPassword ? 'border-color: #10b981;' : '')"
                  style="background: var(--bg-primary); border: 1px solid var(--border-color); color: var(--text-primary);"
                  placeholder="请再次输入密码"
                  @blur="handleRegisterBlur('confirmPassword')"
                >
                <div class="absolute right-3 top-1/2 -translate-y-1/2 flex items-center gap-2">
                  <button 
                    type="button"
                    @click="showConfirmPassword = !showConfirmPassword"
                    class="p-1 rounded hover:bg-[var(--bg-tertiary)] transition-colors"
                    style="color: var(--text-muted)"
                  >
                    <Eye v-if="!showConfirmPassword" class="w-4 h-4" />
                    <EyeOff v-else class="w-4 h-4" />
                  </button>
                  <Check v-if="registerForm.confirmPassword && !registerErrors.confirmPassword && registerTouched.confirmPassword" class="w-4 h-4 text-emerald-500" />
                  <X v-else-if="registerErrors.confirmPassword" class="w-4 h-4 text-red-500" />
                </div>
              </div>
              <p v-if="registerErrors.confirmPassword" class="mt-1.5 text-xs text-red-500 flex items-center gap-1">
                <X class="w-3 h-3" />
                {{ registerErrors.confirmPassword }}
              </p>
              <p v-else-if="registerForm.confirmPassword && registerForm.password === registerForm.confirmPassword" class="mt-1.5 text-xs text-emerald-500 flex items-center gap-1">
                <Check class="w-3 h-3" />
                密码匹配
              </p>
            </div>

            <button
              type="submit"
              :disabled="loading || !registerValid"
              class="w-full py-2.5 bg-[#ea580c] hover:bg-[#c2410c] text-white font-medium rounded-xl transition-all shadow-lg shadow-[#ea580c]/20 hover:shadow-[#ea580c]/30 active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {{ loading ? '创建中...' : '创建账户' }}
            </button>
          </div>
        </form>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@keyframes shake {
  0%, 100% { transform: translateX(0) scale(1); }
  10%, 30%, 50%, 70%, 90% { transform: translateX(-6px) scale(1); }
  20%, 40%, 60%, 80% { transform: translateX(6px) scale(1); }
}

.shake-animation {
  animation: shake 0.4s ease-in-out;
}

.animate-fade-in {
  animation: fadeIn 0.3s ease;
}

.animate-scale-in {
  animation: scaleIn 0.3s ease forwards;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes scaleIn {
  from { opacity: 0; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}
</style>
