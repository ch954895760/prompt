import request from '@/utils/request'
import type { UserSetting } from '@/types'

export function getSettings(): Promise<UserSetting> {
  return request.get('/settings')
}

export function updateSettings(data: Partial<UserSetting>): Promise<UserSetting> {
  return request.put('/settings', data)
}

export function aiTest(content: string, providerId?: number | null): Promise<string> {
  return request.post('/settings/ai-test', { content, providerId })
}

export interface AiTestStreamCallbacks {
  onChunk: (text: string) => void
  onDone: () => void
  onError: (error: string) => void
}

export function aiTestStream(content: string, callbacks: AiTestStreamCallbacks, providerId?: number | null): () => void {
  const STORAGE_KEY_PREFIX = 'prompt_vault_'
  const rememberMe = localStorage.getItem(`${STORAGE_KEY_PREFIX}remember_me`) === 'true'
  const storage = rememberMe ? localStorage : sessionStorage
  const token = storage.getItem(`${STORAGE_KEY_PREFIX}token`) || ''

  const controller = new AbortController()

  fetch('/api/settings/ai-test', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    body: JSON.stringify({ content, providerId }),
    signal: controller.signal,
  }).then(async (response) => {
    if (!response.ok) {
      const text = await response.text()
      let message = 'AI 请求失败'
      try {
        const json = JSON.parse(text)
        message = json.message || message
      } catch {
        message = text || message
      }
      throw new Error(message)
    }

    const reader = response.body?.getReader()
    if (!reader) {
      throw new Error('无法读取响应流')
    }

    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        const trimmed = line.trim()
        if (!trimmed.startsWith('data:')) continue

        const data = trimmed.slice(5).trim()
        if (!data) continue

        if (data === '[DONE]') {
          callbacks.onDone()
          return
        }

        try {
          const parsed = JSON.parse(data)
          if (typeof parsed === 'object' && parsed !== null) {
            if (parsed.error) {
              callbacks.onError(parsed.error)
              return
            }
            callbacks.onChunk(parsed.content || '')
          } else {
            callbacks.onChunk(String(parsed))
          }
        } catch {
          callbacks.onChunk(data)
        }
      }
    }

    callbacks.onDone()
  }).catch((err) => {
    if (err.name !== 'AbortError') {
      callbacks.onError(err.message || 'AI 请求失败')
    }
  })

  return () => controller.abort()
}
