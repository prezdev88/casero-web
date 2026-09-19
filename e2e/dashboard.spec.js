const { test, expect } = require('@playwright/test');

const ADMIN_PIN = process.env.ADMIN_PIN || '1111';
const BASE_URL = process.env.BASE_URL || 'http://localhost:8080/casero';

test.describe('Dashboard View', () => {
  test('should display all stats cards and the evolution chart', async ({ page }) => {
    // 1. Log in
    await page.goto(`${BASE_URL}/login`);
    await page.getByTestId('login-pin').fill(ADMIN_PIN);
    await page.waitForURL('**/customers');

    // 2. Navigate to Dashboard using the navbar
    await page.getByTestId('nav-dashboard').click();
    await page.waitForURL('**/dashboard');

    // 3. Verify the main header
    await expect(page.locator('h2', { hasText: '📊 Dashboard' })).toBeVisible();

    // 4. Verify stat cards are visible by their headings
    await expect(page.locator('h3', { hasText: 'Cumpleaños' })).toBeVisible();
    await expect(page.locator('h3', { hasText: 'Deuda Total' })).toBeVisible();
    await expect(page.locator('h3', { hasText: 'Morosos' })).toBeVisible();
    await expect(page.locator('h3', { hasText: 'Capital en Riesgo' })).toBeVisible();
    await expect(page.locator('h3', { hasText: 'Clientes Activos' })).toBeVisible();
    await expect(page.locator('h3', { hasText: 'Deuda Promedio' })).toBeVisible();
    await expect(page.locator('h3', { hasText: 'Cuentas Saldadas' })).toBeVisible();
    await expect(page.locator('h3', { hasText: 'Prendas Vendidas' })).toBeVisible();
    
    // The balance card contains two headings
    await expect(page.locator('h3', { hasText: 'Vendido este mes' })).toBeVisible();
    await expect(page.locator('h3', { hasText: 'Cobrado este mes' })).toBeVisible();

    // Verify the MTD text is present
    await expect(page.locator('span', { hasText: 'vs. misma fecha mes ant.' }).first()).toBeVisible();

    // Verify Top 3 Customers block is present
    await expect(page.locator('h3', { hasText: 'Mejores Pagadores' })).toBeVisible();

    // 5. Verify the Chart canvas is present
    const chartCanvas = page.locator('canvas#salesChart');
    await expect(chartCanvas).toBeAttached();

    // 6. Test navigation from a card (e.g., Cumpleaños)
    await page.locator('h3', { hasText: 'Cumpleaños' }).click();
    await page.waitForURL('**/dashboard/birthdays');
    
    // Verify it reached the birthdays view by looking for its specific UI elements or title
    await expect(page.locator('a.btn', { hasText: 'Volver' })).toBeVisible();
  });
});
