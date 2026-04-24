import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type { User } from '@/types'
import { login as apiLogin, register as apiRegister } from '@/api/auth'

const STORAGE_KEY_PREFIX = 'prompt_vault_'

function getStorage(rememberMe: boolean | null): Storage {
  if (rememberMe === null) {
    const stored = localStorage.getItem(`${STORAGE_KEY_PREFIX}remember_me`)
    return stored === 'true' ? localStorage : sessionStorage
  }
  return rememberMe ? localStorage : sessionStorage
}

export const useUserStore = defineStore('user', () => {
  const storage = getStorage(null)
  const token = ref(storage.getItem(`${STORAGE_KEY_PREFIX}token`) || '')
  const refreshToken = ref(storage.getItem(`${STORAGE_KEY_PREFIX}refreshToken`) || '')
  const user = ref<User | null>(getStoredUser())
  const theme = ref<'light' | 'dark'>(getStoredTheme())
  const rememberMe = ref(localStorage.getItem(`${STORAGE_KEY_PREFIX}remember_me`) === 'true')

  const isLoggedIn = computed(() => !!token.value)

  function getStoredTheme(): 'light' | 'dark' {
    const local = localStorage.getItem('theme') as 'light' | 'dark' | null
    if (local) return local
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  }

  function toggleTheme() {
    const newTheme = theme.value === 'dark' ? 'light' : 'dark'
    setTheme(newTheme)
  }

  function setTheme(newTheme: 'light' | 'dark') {
    theme.value = newTheme
    localStorage.setItem('theme', newTheme)
    document.documentElement.classList.toggle('dark', newTheme === 'dark')
  }

  function getStoredUser(): User | null {
    const raw = localStorage.getItem(`${STORAGE_KEY_PREFIX}user`) || sessionStorage.getItem(`${STORAGE_KEY_PREFIX}user`)
    if (!raw) return null
    try {
      return JSON.parse(raw) as User
    } catch {
      return null
    }
  }

  function storeUser(u: User | null, useLocalStorage: boolean) {
    const storage = useLocalStorage ? localStorage : sessionStorage
    if (u) {
      storage.setItem(`${STORAGE_KEY_PREFIX}user`, JSON.stringify(u))
    } else {
      localStorage.removeItem(`${STORAGE_KEY_PREFIX}user`)
      sessionStorage.removeItem(`${STORAGE_KEY_PREFIX}user`)
    }
  }

  function clearAllStorage() {
    const keys = ['token', 'refreshToken', 'user']
    keys.forEach(key => {
      localStorage.removeItem(`${STORAGE_KEY_PREFIX}${key}`)
      sessionStorage.removeItem(`${STORAGE_KEY_PREFIX}${key}`)
    })
    localStorage.removeItem(`${STORAGE_KEY_PREFIX}remember_me`)
  }

  async function login(email: string, password: string, isRememberMe = false) {
    const res = await apiLogin(email, password, isRememberMe)
    token.value = res.accessToken
    refreshToken.value = res.refreshToken
    user.value = res.user
    rememberMe.value = isRememberMe

    localStorage.setItem(`${STORAGE_KEY_PREFIX}remember_me`, String(isRememberMe))

    const storage = isRememberMe ? localStorage : sessionStorage
    storage.setItem(`${STORAGE_KEY_PREFIX}token`, res.accessToken)
    storage.setItem(`${STORAGE_KEY_PREFIX}refreshToken`, res.refreshToken)
    storeUser(res.user, isRememberMe)

    return res
  }

  async function register(username: string, email: string, password: string) {
    const res = await apiRegister(username, email, password)
    token.value = res.accessToken
    refreshToken.value = res.refreshToken
    user.value = res.user
    rememberMe.value = false

    localStorage.setItem(`${STORAGE_KEY_PREFIX}remember_me`, 'false')

    const storage = sessionStorage
    storage.setItem(`${STORAGE_KEY_PREFIX}token`, res.accessToken)
    storage.setItem(`${STORAGE_KEY_PREFIX}refreshToken`, res.refreshToken)
    storeUser(res.user, false)

    return res
  }

  function logout() {
    token.value = ''
    refreshToken.value = ''
    user.value = null
    rememberMe.value = false
    clearAllStorage()
  }

  function setUser(u: User) {
    user.value = u
    storeUser(u, rememberMe.value)
  }

  return { token, refreshToken, user, theme, rememberMe, isLoggedIn, login, register, logout, setUser, toggleTheme, setTheme }
})
