const { test, expect } = require('@playwright/test');

const ADMIN_PIN = process.env.ADMIN_PIN || '1111';
const BASE_URL = process.env.BASE_URL || 'http://localhost:8080/casero';

test.describe('Sales View', () => {
  test('should display the sales page', async ({ page }) => {
    // 1. Log in
    await page.goto(`${BASE_URL}/login`);
    await page.getByTestId('login-pin').fill(ADMIN_PIN);
    await page.waitForURL('**/customers');

    // 2. Navigate to Dashboard then to Sales
    await page.goto(`${BASE_URL}/dashboard`);
    await page.locator('h3', { hasText: 'Prendas Vendidas' }).click();
    await page.waitForURL('**/dashboard/sales');

    // 3. Verify the main header
    await expect(page.locator('h2', { hasText: '👕 Prendas Vendidas Este Mes' })).toBeVisible();
    
    // 4. Verify back button exists
    await expect(page.locator('a.btn', { hasText: 'Volver a Datos importantes' })).toBeVisible();
  });
});
