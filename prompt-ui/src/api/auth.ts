import request from '@/utils/request'
import type { AuthResponse } from '@/types'

export function login(email: string, password: string, rememberMe?: boolean): Promise<AuthResponse> {
  return request.post('/auth/login', { email, password, rememberMe })
}

export function register(username: string, email: string, password: string): Promise<AuthResponse> {
  return request.post('/auth/register', { username, email, password })
}

export function refreshToken(refreshToken: string): Promise<{ accessToken: string; refreshToken: string }> {
  return request.post('/auth/refresh', { refreshToken })
}
