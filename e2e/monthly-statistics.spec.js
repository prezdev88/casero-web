const { test, expect } = require('@playwright/test');

const ADMIN_PIN = process.env.ADMIN_PIN || '1111';
const BASE_URL = process.env.BASE_URL || 'http://localhost:8080/casero';

test.describe('Monthly Statistics View', () => {
  test('should display month/year filters, custom range filters, and the period subtitle', async ({ page }) => {
    // 1. Log in
    await page.goto(`${BASE_URL}/login`);
    await page.getByTestId('login-pin').fill(ADMIN_PIN);
    await page.waitForURL('**/customers');

    // 2. Navigate to monthly statistics
    await page.goto(`${BASE_URL}/statistics/monthly`);

    // 3. Verify elements are visible
    // Selectors by name for the month and year dropdowns
    const monthSelect = page.locator('select[name="month"]');
    const yearSelect = page.locator('select[name="year"]');
    await expect(monthSelect).toBeVisible();
    await expect(yearSelect).toBeVisible();

    // Verify custom range inputs
    const startInput = page.locator('input[name="start"]');
    const endInput = page.locator('input[name="end"]');
    await expect(startInput).toBeVisible();
    await expect(endInput).toBeVisible();

    // 4. Verify the new period subtitle in the "Montos" card
    const periodSubtitle = page.locator('p', { hasText: /Periodo:.*al.*/ });
    await expect(periodSubtitle).toBeVisible();

    // 5. Test Month/Year form submission
    // Select Febrero (2) and 2026
    await monthSelect.selectOption('2');
    await yearSelect.selectOption('2026');
    await page.getByRole('button', { name: 'Filtrar por Mes' }).click();

    // Wait for reload
    await page.waitForURL('**/statistics/monthly?month=2&year=2026');
    
    // Verify subtitle says from 01/02/2026 to 28/02/2026
    await expect(page.locator('p', { hasText: 'Periodo: 01/02/2026 al 28/02/2026' })).toBeVisible();

    // 6. Test Custom Range form submission
    await startInput.fill('2026-03-15');
    await endInput.fill('2026-03-20');
    await page.getByRole('button', { name: 'Filtrar por Rango' }).click();

    // Wait for reload
    await page.waitForURL('**/statistics/monthly?start=2026-03-15&end=2026-03-20');

    // Verify subtitle for the exact custom range
    await expect(page.locator('p', { hasText: 'Periodo: 15/03/2026 al 20/03/2026' })).toBeVisible();
  });
});
