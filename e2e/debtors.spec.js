const { test, expect } = require('@playwright/test');

const ADMIN_PIN = process.env.ADMIN_PIN || '1111';
const BASE_URL = process.env.BASE_URL || 'http://localhost:8080/casero';

test.describe('Debtors View', () => {
  test('should display the debtors page', async ({ page }) => {
    // 1. Log in
    await page.goto(`${BASE_URL}/login`);
    await page.getByTestId('login-pin').fill(ADMIN_PIN);
    await page.waitForURL('**/customers');

    // 2. Navigate to Dashboard then to Debtors (Morosos)
    await page.goto(`${BASE_URL}/dashboard`);
    await page.locator('h3', { hasText: 'Morosos' }).click();
    await page.waitForURL('**/debtors');

    // 3. Verify the main header
    await expect(page.locator('h2', { hasText: 'Morosos' })).toBeVisible();
  });

  test('should navigate to debtors from Capital en Riesgo card', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`);
    await page.getByTestId('login-pin').fill(ADMIN_PIN);
    await page.waitForURL('**/customers');

    await page.goto(`${BASE_URL}/dashboard`);
    await page.locator('h3', { hasText: 'Capital en Riesgo' }).click();
    await page.waitForURL('**/debtors');
    
    await expect(page.locator('h2', { hasText: 'Morosos' })).toBeVisible();
  });
});
