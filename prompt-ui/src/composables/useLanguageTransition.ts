import { ref, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { setLocale, getLocale, toggleLocale as toggleI18nLocale } from '@/i18n'

type Locale = 'zh-CN' | 'en'

const isTransitioning = ref(false)

export function useLanguageTransition() {
  const { locale } = useI18n()

  async function toggleLocaleWithTransition(): Promise<Locale> {
    if (isTransitioning.value) return getLocale()

    isTransitioning.value = true

    // 创建过渡遮罩
    const overlay = document.createElement('div')
    overlay.className = 'language-transition-overlay'
    overlay.style.cssText = `
      position: fixed;
      inset: 0;
      background: var(--bg-primary, #ffffff);
      z-index: 9999;
      opacity: 0;
      transition: opacity 200ms ease;
      pointer-events: none;
    `
    document.body.appendChild(overlay)

    // 淡出动画
    await nextTick()
    overlay.style.opacity = '1'

    // 等待淡出完成
    await new Promise(resolve => setTimeout(resolve, 200))

    // 切换语言
    const newLocale = toggleI18nLocale()
    locale.value = newLocale

    // 等待 DOM 更新
    await nextTick()

    // 淡入动画
    overlay.style.opacity = '0'

    // 等待淡入完成并移除遮罩
    await new Promise(resolve => setTimeout(resolve, 200))
    overlay.remove()

    isTransitioning.value = false
    return newLocale
  }

  async function setLocaleWithTransition(newLocale: Locale): Promise<void> {
    if (isTransitioning.value || newLocale === getLocale()) return

    isTransitioning.value = true

    // 创建过渡遮罩
    const overlay = document.createElement('div')
    overlay.className = 'language-transition-overlay'
    overlay.style.cssText = `
      position: fixed;
      inset: 0;
      background: var(--bg-primary, #ffffff);
      z-index: 9999;
      opacity: 0;
      transition: opacity 200ms ease;
      pointer-events: none;
    `
    document.body.appendChild(overlay)

    // 淡出动画
    await nextTick()
    overlay.style.opacity = '1'

    // 等待淡出完成
    await new Promise(resolve => setTimeout(resolve, 200))

    // 切换语言
    setLocale(newLocale)
    locale.value = newLocale

    // 等待 DOM 更新
    await nextTick()

    // 淡入动画
    overlay.style.opacity = '0'

    // 等待淡入完成并移除遮罩
    await new Promise(resolve => setTimeout(resolve, 200))
    overlay.remove()

    isTransitioning.value = false
  }

  return {
    isTransitioning,
    toggleLocaleWithTransition,
    setLocaleWithTransition,
    currentLocale: () => getLocale(),
  }
}
