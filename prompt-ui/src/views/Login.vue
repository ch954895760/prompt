<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useToastStore } from '@/stores/toast'
import { useI18n } from 'vue-i18n'
import { Sparkles, Sun, Moon, Check, X, Eye, EyeOff, Languages } from 'lucide-vue-next'
import { getLocale } from '@/i18n'
import { useLanguageTransition } from '@/composables/useLanguageTransition'
import ThemeTransition from '@/components/ThemeTransition.vue'

const router = useRouter()
const userStore = useUserStore()
const toastStore = useToastStore()
const { t } = useI18n()

const authMode = ref('login')
const loading = ref(false)
const shakeForm = ref(false)
const currentLocale = ref(getLocale())
const { toggleLocaleWithTransition } = useLanguageTransition()

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
      errors.email = t('login.emailRequired')
    } else if (!emailRegex.test(loginForm.value.email)) {
      errors.email = t('login.emailInvalid')
    }
  }
  
  if (loginTouched.value.password) {
    if (!loginForm.value.password) {
      errors.password = t('login.passwordRequired')
    } else if (loginForm.value.password.length < passwordMinLength) {
      errors.password = t('login.passwordMinLength')
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
      errors.username = t('login.usernameRequired')
    } else if (registerForm.value.username.length < 2) {
      errors.username = t('login.usernameMinLength')
    } else if (registerForm.value.username.length > 20) {
      errors.username = t('login.usernameMaxLength')
    }
  }
  
  if (registerTouched.value.email) {
    if (!registerForm.value.email) {
      errors.email = t('login.emailRequired')
    } else if (!emailRegex.test(registerForm.value.email)) {
      errors.email = t('login.emailInvalid')
    }
  }
  
  if (registerTouched.value.password) {
    if (!registerForm.value.password) {
      errors.password = t('login.passwordRequired')
    } else if (registerForm.value.password.length < passwordMinLength) {
      errors.password = t('login.passwordMinLength')
    }
  }
  
  if (registerTouched.value.confirmPassword) {
    if (!registerForm.value.confirmPassword) {
      errors.confirmPassword = t('login.confirmPasswordRequired')
    } else if (registerForm.value.password !== registerForm.value.confirmPassword) {
      errors.confirmPassword = t('login.passwordMismatch')
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

function handleThemeToggle() {
  toggleTheme()
}

async function handleToggleLocale() {
  const newLocale = await toggleLocaleWithTransition()
  currentLocale.value = newLocale
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
    toastStore.success(t('user.loginSuccess'))
    router.push('/dashboard')
  } catch (e: any) {
    toastStore.error(e.message || t('user.loginFailed'))
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
    toastStore.success(t('user.registerSuccess'))
    router.push('/dashboard')
  } catch (e: any) {
    toastStore.error(e.message || t('user.registerFailed'))
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

    <!-- Theme toggle with ripple animation -->
    <ThemeTransition :current-theme="userStore.theme" @toggle="handleThemeToggle">
      <template #default="{ trigger }">
        <button @click="trigger" :aria-label="t('theme.toggle')" class="absolute top-6 right-6 w-10 h-10 rounded-xl flex items-center justify-center transition-colors hover:bg-[var(--bg-tertiary)] z-20">
          <Sun v-if="userStore.theme === 'dark'" class="w-5 h-5" style="color: var(--text-secondary)" />
          <Moon v-else class="w-5 h-5" style="color: var(--text-secondary)" />
        </button>
      </template>
    </ThemeTransition>

    <!-- Language toggle -->
    <button @click="handleToggleLocale" :aria-label="t('language.toggle')" class="absolute top-6 right-20 w-10 h-10 rounded-xl flex items-center justify-center transition-colors hover:bg-[var(--bg-tertiary)] z-20">
      <Languages class="w-5 h-5" style="color: var(--text-secondary)" />
    </button>

    <div class="relative z-10 w-full max-w-md px-6">
      <div class="text-center mb-10 animate-fade-in">
        <div class="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-[#ea580c] text-white mb-5 shadow-lg shadow-[#ea580c]/20">
          <Sparkles class="w-7 h-7" />
        </div>
        <h1 class="text-3xl font-bold tracking-tight mb-2" style="font-family: 'Outfit', sans-serif; color: var(--text-primary)">{{ t('login.title') }}</h1>
        <p class="text-sm" style="color: var(--text-secondary)">{{ t('login.subtitle') }}</p>
      </div>

      <div class="animate-scale-in" style="animation-delay: 0.1s;">
        <div class="rounded-2xl p-8" style="background: var(--bg-secondary); border: 1px solid var(--border-color);">
          <div class="flex gap-2 mb-6 p-1 rounded-xl" style="background: var(--bg-tertiary)">
            <button @click="authMode = 'login'"
              class="flex-1 py-2 text-sm font-medium rounded-lg transition-all"
              :class="authMode === 'login' ? 'bg-surface-800 text-white shadow-sm' : ''"
              :style="authMode === 'login' ? '' : 'color: var(--text-secondary)'"
            >{{ t('user.login') }}</button>
            <button @click="authMode = 'register'"
              class="flex-1 py-2 text-sm font-medium rounded-lg transition-all"
              :class="authMode === 'register' ? 'bg-surface-800 text-white shadow-sm' : ''"
              :style="authMode === 'register' ? '' : 'color: var(--text-secondary)'"
            >{{ t('user.register') }}</button>
          </div>

        <form v-if="authMode === 'login'" @submit.prevent="handleLogin">
          <div class="space-y-4">
            <!-- 邮箱输入 -->
            <div :class="{ 'shake-animation': shakeForm }">
              <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">{{ t('user.email') }}</label>
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
                  :placeholder="t('login.emailPlaceholder')"
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
              <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">{{ t('user.password') }}</label>
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
                  :placeholder="t('login.passwordPlaceholder')"
                  @blur="handleLoginBlur('password')"
                >
                <!-- 显示密码按钮和验证状态 -->
                <div class="absolute right-3 top-1/2 -translate-y-1/2 flex items-center gap-2">
                  <button
                    type="button"
                    @click="showLoginPassword = !showLoginPassword"
                    :aria-label="showLoginPassword ? t('common.hide') : t('common.show')"
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
                <span>{{ t('user.rememberMe') }}</span>
              </label>
              <a href="#" class="hover:underline" style="color: var(--accent)">{{ t('user.forgotPassword') }}?</a>
            </div>

            <button
              type="submit"
              :disabled="loading || !loginValid"
              class="w-full py-2.5 bg-[#ea580c] hover:bg-[#c2410c] text-white font-medium rounded-xl transition-all shadow-lg shadow-[#ea580c]/20 hover:shadow-[#ea580c]/30 active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {{ loading ? t('login.loggingIn') : t('user.login') }}
            </button>
          </div>
        </form>

        <form v-else @submit.prevent="handleRegister">
          <div class="space-y-4">
            <!-- 用户名输入 -->
            <div>
              <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">{{ t('user.username') }}</label>
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
                  :placeholder="t('login.usernamePlaceholder')"
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
              <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">{{ t('user.email') }}</label>
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
                  :placeholder="t('login.emailPlaceholder')"
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
              <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">{{ t('user.password') }}</label>
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
                  :placeholder="t('login.passwordPlaceholder')"
                  @blur="handleRegisterBlur('password')"
                >
                <div class="absolute right-3 top-1/2 -translate-y-1/2 flex items-center gap-2">
                  <button
                    type="button"
                    @click="showRegisterPassword = !showRegisterPassword"
                    :aria-label="showRegisterPassword ? t('common.hide') : t('common.show')"
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
                  {{ registerForm.password.length < 8 ? t('login.passwordWeak') : registerForm.password.length < 12 ? t('login.passwordMedium') : t('login.passwordStrong') }}
                </span>
              </div>
            </div>

            <!-- 确认密码输入 -->
            <div>
              <label class="block text-xs font-medium mb-1.5" style="color: var(--text-secondary)">{{ t('user.confirmPassword') }}</label>
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
                  :placeholder="t('login.confirmPasswordPlaceholder')"
                  @blur="handleRegisterBlur('confirmPassword')"
                >
                <div class="absolute right-3 top-1/2 -translate-y-1/2 flex items-center gap-2">
                  <button
                    type="button"
                    @click="showConfirmPassword = !showConfirmPassword"
                    :aria-label="showConfirmPassword ? t('common.hide') : t('common.show')"
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
            </div>

            <button
              type="submit"
              :disabled="loading || !registerValid"
              class="w-full py-2.5 bg-[#ea580c] hover:bg-[#c2410c] text-white font-medium rounded-xl transition-all shadow-lg shadow-[#ea580c]/20 hover:shadow-[#ea580c]/30 active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {{ loading ? t('login.registering') : t('user.register') }}
            </button>
          </div>
        </form>
        </div>

        <p class="text-center mt-6 text-sm" style="color: var(--text-secondary)">
          {{ authMode === 'login' ? t('login.noAccount') : t('login.hasAccount') }}
          <button 
            @click="authMode = authMode === 'login' ? 'register' : 'login'"
            class="font-medium hover:underline ml-1" 
            style="color: var(--accent)"
          >
            {{ authMode === 'login' ? t('login.clickToRegister') : t('login.clickToLogin') }}
          </button>
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  10%, 30%, 50%, 70%, 90% { transform: translateX(-4px); }
  20%, 40%, 60%, 80% { transform: translateX(4px); }
}

.shake-animation {
  animation: shake 0.5s ease-in-out;
}

.animate-fade-in {
  animation: fadeIn 0.5s ease-out;
}

.animate-scale-in {
  animation: scaleIn 0.4s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes scaleIn {
  from { opacity: 0; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}
</style>
