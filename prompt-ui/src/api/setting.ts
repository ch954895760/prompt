import request from '@/utils/request'
import type { UserSetting } from '@/types'

export function getSettings(): Promise<UserSetting> {
  return request.get('/settings')
}

export function updateSettings(data: Partial<UserSetting>): Promise<UserSetting> {
  return request.put('/settings', data)
}

export function aiTest(content: string): Promise<string> {
  return request.post('/settings/ai-test', { content })
}
