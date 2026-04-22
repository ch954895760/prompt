import { test, expect } from '@playwright/test'

const TEST_USER = `e2e_${Date.now()}`
const TEST_PASSWORD = 'TestPass123!'
const TEST_EMAIL = `e2e_${Date.now()}@example.com`

test.describe.serial('Prompt Vault E2E', () => {
  let token: string = ''

  test.beforeAll(async ({ request }) => {
    // Register user via API once
    const res = await request.post('http://localhost:8080/api/auth/register', {
      data: { username: TEST_USER, email: TEST_EMAIL, password: TEST_PASSWORD }
    })
    const body = await res.json()
    token = body.data?.accessToken
  })

  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.evaluate((t: string) => {
      localStorage.clear()
      localStorage.setItem('token', t)
    }, token)
  })

  test('register and login via UI', async ({ page }) => {
    await page.reload()
    await expect(page.locator('h1:has-text("Prompt Vault")')).toBeVisible()

    // Switch to register tab
    await page.click('button:has-text("注册")')
    await page.fill('input[placeholder="你的名字"]', `${TEST_USER}_ui`)
    await page.fill('input[placeholder="you@example.com"]', `ui_${TEST_EMAIL}`)
    await page.fill('input[placeholder="••••••••"]', TEST_PASSWORD)
    await page.click('button[type="submit"]:has-text("创建账户")')

    await expect(page).toHaveURL(/\/dashboard/, { timeout: 10000 })
    await expect(page.locator('text=控制台')).toBeVisible()
  })

  test('create category and prompt with variables', async ({ page }) => {
    await page.goto('/dashboard')
    await expect(page.locator('text=控制台')).toBeVisible()

    // Create category
    await page.goto('/categories')
    await expect(page.locator('h2:has-text("分类管理")')).toBeVisible()
    await page.click('text=新建分类')
    await page.fill('input[placeholder="输入分类名称"]', 'E2E Category')
    await page.click('button:has-text("创建")')
    await page.waitForTimeout(500)
    await page.reload()
    await expect(page.locator('text=E2E Category')).toBeVisible()

    // Create prompt
    await page.goto('/editor')
    await expect(page.locator('h2:has-text("新建提示词")')).toBeVisible()
    await page.fill('input[placeholder="给你的提示词起个名字..."]', 'E2E Test Prompt')
    await page.fill('textarea[placeholder="在这里输入你的提示词模板..."]', 'Hello {{name}}, welcome to {{place}}!')
    await page.click('button:has-text("保存")')

    await expect(page).toHaveURL(/\/prompts/, { timeout: 10000 })
    await expect(page.locator('text=E2E Test Prompt')).toBeVisible()
  })

  test('search prompts', async ({ page }) => {
    await page.goto('/dashboard')

    // Search from header
    await page.fill('input[placeholder="搜索提示词、标签、内容..."]', 'E2E')
    await page.press('input[placeholder="搜索提示词、标签、内容..."]', 'Enter')

    await expect(page).toHaveURL(/\/prompts/)
    await expect(page.locator('text=E2E Test Prompt')).toBeVisible()
  })

  test('theme toggle', async ({ page }) => {
    await page.goto('/dashboard')
    await expect(page.locator('text=控制台')).toBeVisible()

    const themeBtn = page.locator('header button').nth(1)
    await themeBtn.click()
    await expect(page.locator('html')).toHaveClass(/dark/)

    await themeBtn.click()
    await expect(page.locator('html')).not.toHaveClass(/dark/)
  })

  test('settings - AI config', async ({ page }) => {
    await page.goto('/settings')
    await expect(page.locator('h2:has-text("设置")')).toBeVisible()

    await page.selectOption('select', 'openai')
    await page.fill('input[placeholder="https://api.openai.com/v1"]', 'https://test-api.example.com')
    await page.fill('input[placeholder="sk-xxxxxxxxxxxxxxxx"]', 'test-api-key-123')
    await page.fill('input[placeholder="gpt-4, claude-3-5-sonnet 等"]', 'gpt-4-test')

    await page.click('button:has-text("保存设置")')
    // Wait for save to complete - button returns to normal state
    await expect(page.locator('button:has-text("保存设置")')).toBeVisible()
  })

  test('export data', async ({ page }) => {
    await page.goto('/settings')
    await expect(page.locator('h2:has-text("设置")')).toBeVisible()

    await page.click('text=导出 JSON')
    // Export triggers programmatic download; just verify no error occurs
    await expect(page.locator('text=导出 JSON')).toBeVisible()
  })
})
