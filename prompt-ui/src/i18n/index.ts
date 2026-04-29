import { createI18n } from 'vue-i18n'
import zhCN from './locales/zh-CN'
import en from './locales/en'

type Locale = 'zh-CN' | 'en'

const STORAGE_KEY = 'prompt_vault_locale'

function getStoredLocale(): Locale {
  const stored = localStorage.getItem(STORAGE_KEY) as Locale | null
  if (stored && (stored === 'zh-CN' || stored === 'en')) return stored
  // 根据浏览器语言自动检测
  const browserLang = navigator.language.toLowerCase()
  if (browserLang.startsWith('zh')) return 'zh-CN'
  return 'en'
}

export const i18n = createI18n({
  legacy: false,
  locale: getStoredLocale(),
  fallbackLocale: 'en',
  messages: {
    'zh-CN': zhCN,
    'en': en,
  },
})

export function setLocale(locale: Locale) {
  i18n.global.locale.value = locale
  localStorage.setItem(STORAGE_KEY, locale)
}

export function getLocale(): Locale {
  return i18n.global.locale.value as Locale
}

export function toggleLocale(): Locale {
  const current = getLocale()
  const next: Locale = current === 'zh-CN' ? 'en' : 'zh-CN'
  setLocale(next)
  return next
}
