import request from '@/utils/request'
import type { Category } from '@/types'

export function getCategoryTree(): Promise<Category[]> {
  return request.get('/categories')
}

export function getCategoryList(): Promise<Category[]> {
  return request.get('/categories/list')
}

export function createCategory(data: Partial<Category>): Promise<Category> {
  return request.post('/categories', data)
}

export function updateCategory(id: number, data: Partial<Category>): Promise<Category> {
  return request.put(`/categories/${id}`, data)
}

export function deleteCategory(id: number): Promise<void> {
  return request.delete(`/categories/${id}`)
}
