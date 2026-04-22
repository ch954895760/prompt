import request from '@/utils/request'
import type { Prompt } from '@/types'

export function getPrompts(params: { current?: number; size?: number; categoryId?: number; tagId?: number; keyword?: string }): Promise<{ records: Prompt[]; total: number }> {
  return request.get('/prompts', { params })
}

export function exportPromptsJson(): Promise<Blob> {
  return request.get('/prompts/export/json', { responseType: 'blob' })
}

export function exportPromptsMarkdown(): Promise<Blob> {
  return request.get('/prompts/export/markdown', { responseType: 'blob' })
}

export function importPrompts(data: any[]): Promise<number> {
  return request.post('/prompts/import', data)
}

export function getPromptList(): Promise<Prompt[]> {
  return request.get('/prompts/list')
}

export function getPrompt(id: number): Promise<Prompt> {
  return request.get(`/prompts/${id}`)
}

export function createPrompt(data: Partial<Prompt>): Promise<Prompt> {
  return request.post('/prompts', data)
}

export function updatePrompt(id: number, data: Partial<Prompt>): Promise<Prompt> {
  return request.put(`/prompts/${id}`, data)
}

export function deletePrompt(id: number): Promise<void> {
  return request.delete(`/prompts/${id}`)
}

export function usePrompt(id: number): Promise<void> {
  return request.post(`/prompts/${id}/use`)
}

export function getPromptHistory(id: number): Promise<{ id: number; promptId: number; content: string; version: number; createdAt: string }[]> {
  return request.get(`/prompts/${id}/history`)
}

export function rollbackPrompt(id: number, version: number): Promise<void> {
  return request.post(`/prompts/${id}/history/${version}/rollback`)
}
