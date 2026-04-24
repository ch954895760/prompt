export interface User {
  id: number
  username: string
  email: string
  avatar?: string
  createdAt: string
}

export interface Prompt {
  id: number
  userId: number
  title: string
  content: string
  description?: string
  categoryId?: number
  categoryName?: string
  categoryColor?: string
  variablesJson?: Record<string, string>
  tagIds?: number[]
  isPublic: boolean
  usageCount: number
  tags: Tag[]
  createdAt: string
  updatedAt: string
}

export interface Category {
  id: number
  userId: number
  name: string
  parentId?: number
  sortOrder: number
  icon?: string
  color: string
  children?: Category[]
  promptCount?: number
  createdAt: string
}

export interface Tag {
  id: number
  userId: number
  name: string
  color: string
  createdAt: string
}

export interface PromptHistory {
  id: number
  promptId: number
  content: string
  version: number
  createdAt: string
}

export interface UserSetting {
  id: number
  userId: number
  theme: 'light' | 'dark'
  defaultModel?: string
  apiBaseUrl?: string
  apiKeyEncrypted?: string
  model?: string
}

export interface AiProvider {
  id: number
  name: string
  provider: string
  apiBaseUrl: string
  model: string
  isDefault: boolean
  sortOrder: number
  createdAt: string
  updatedAt: string
}

export interface AiProviderCreateRequest {
  name: string
  provider: string
  apiBaseUrl: string
  apiKey: string
  model: string
  isDefault: boolean
}

export interface AiProviderUpdateRequest {
  name: string
  provider: string
  apiBaseUrl: string
  apiKey?: string
  model: string
  isDefault: boolean
}

export interface LoginRequest {
  email: string
  password: string
  rememberMe?: boolean
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  user: User
  rememberMe?: boolean
}

export interface Result<T> {
  code: number
  message: string
  data: T
  timestamp: number
}
