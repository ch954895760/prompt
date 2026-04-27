import request from '@/utils/request'
import type { PromptOptimizeRequest, PromptOptimizeResponse } from '@/types'

export function optimizePrompt(data: PromptOptimizeRequest): Promise<PromptOptimizeResponse> {
  return request.post('/prompts/optimize', data)
}

export function clearOptimizeCache(): Promise<void> {
  return request.delete('/prompts/optimize/cache')
}
