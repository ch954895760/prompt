import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type { User } from '@/types'
import { login as apiLogin, register as apiRegister } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')
  const user = ref<User | null>(getStoredUser())
  const theme = ref<'light' | 'dark'>(getStoredTheme())

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
    const raw = localStorage.getItem('user')
    if (!raw) return null
    try {
      return JSON.parse(raw) as User
    } catch {
      return null
    }
  }

  function storeUser(u: User | null) {
    if (u) {
      localStorage.setItem('user', JSON.stringify(u))
    } else {
      localStorage.removeItem('user')
    }
  }

  async function login(email: string, password: string) {
    const res = await apiLogin(email, password)
    token.value = res.accessToken
    refreshToken.value = res.refreshToken
    user.value = res.user
    localStorage.setItem('token', res.accessToken)
    localStorage.setItem('refreshToken', res.refreshToken)
    storeUser(res.user)
    return res
  }

  async function register(username: string, email: string, password: string) {
    const res = await apiRegister(username, email, password)
    token.value = res.accessToken
    refreshToken.value = res.refreshToken
    user.value = res.user
    localStorage.setItem('token', res.accessToken)
    localStorage.setItem('refreshToken', res.refreshToken)
    storeUser(res.user)
    return res
  }

  function logout() {
    token.value = ''
    refreshToken.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    storeUser(null)
  }

  function setUser(u: User) {
    user.value = u
    storeUser(u)
  }

  return { token, refreshToken, user, theme, isLoggedIn, login, register, logout, setUser, toggleTheme, setTheme }
})
