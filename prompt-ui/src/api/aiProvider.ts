import request from '@/utils/request'
import type { AiProvider, AiProviderCreateRequest, AiProviderUpdateRequest } from '@/types'

export function getAiProviders(): Promise<AiProvider[]> {
  return request.get('/ai-providers')
}

export function getAiProvider(id: number): Promise<AiProvider> {
  return request.get(`/ai-providers/${id}`)
}

export function getDefaultAiProvider(): Promise<AiProvider | null> {
  return request.get('/ai-providers/default')
}

export function createAiProvider(data: AiProviderCreateRequest): Promise<AiProvider> {
  return request.post('/ai-providers', data)
}

export function updateAiProvider(id: number, data: AiProviderUpdateRequest): Promise<AiProvider> {
  return request.put(`/ai-providers/${id}`, data)
}

export function deleteAiProvider(id: number): Promise<void> {
  return request.delete(`/ai-providers/${id}`)
}

export function setDefaultAiProvider(id: number): Promise<void> {
  return request.post(`/ai-providers/${id}/set-default`)
}
