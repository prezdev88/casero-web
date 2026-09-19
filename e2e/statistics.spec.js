const { test, expect } = require('@playwright/test');

const ADMIN_PIN = process.env.ADMIN_PIN || '1111';
const BASE_URL = process.env.BASE_URL || 'http://localhost:8080/casero';

test.describe('Statistics View', () => {
  test('should display the general statistics page', async ({ page }) => {
    // 1. Log in
    await page.goto(`${BASE_URL}/login`);
    await page.getByTestId('login-pin').fill(ADMIN_PIN);
    await page.waitForURL('**/customers');

    // 2. Navigate to Dashboard then to Statistics (Deuda Total)
    await page.goto(`${BASE_URL}/dashboard`);
    await page.locator('h3', { hasText: 'Deuda Total' }).click();
    await page.waitForURL('**/statistics');

    // 3. Verify the main header
    await expect(page.locator('h2', { hasText: 'Deuda total' })).toBeVisible();
    
    // 4. Verify some actions or sections are present
    await expect(page.locator('h3', { hasText: 'Acciones' })).toBeVisible();
  });
});
