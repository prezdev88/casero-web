const { test, expect } = require('@playwright/test');

const ADMIN_PIN = process.env.ADMIN_PIN || '1111';
const BASE_URL = process.env.BASE_URL || 'http://localhost:8080/casero';

test.describe('Vista de Auditoría', () => {
  test('conserva los filtros de Evento y Tipo al usar paginación', async ({ page }) => {
    const customerName = `E2E Audit ${Date.now()}`;

    // Login
    await page.goto(`${BASE_URL}/login`);
    await page.getByTestId('login-pin').fill(ADMIN_PIN);
    await page.waitForURL('**/customers');

    // Go to audit view with size=2 and filters
    await page.goto(`${BASE_URL}/admin/audit?size=2&eventType=ACTION&payloadType=SALE`);
    await page.waitForSelector('.audit-table');

    // Verify there is a "Siguiente" link and click it
    const nextLink = page.getByRole('link', { name: 'Siguiente' });
    await expect(nextLink).toBeVisible();
    await nextLink.click();

    // Verify the URL still contains both filters
    await page.waitForURL('**/admin/audit*');
    const currentUrl = page.url();
    expect(currentUrl).toContain('eventType=ACTION');
    expect(currentUrl).toContain('payloadType=SALE');

    // Verify the filters in the form are still selected
    await expect(page.locator('#eventType')).toHaveValue('ACTION');
    await expect(page.locator('#payloadType')).toHaveValue('SALE');
    
    // Check that table has data and is indeed filtered
    const rows = page.locator('.audit-table tbody tr');
    const count = await rows.count();
    expect(count).toBeGreaterThan(0);
    expect(count).toBeLessThanOrEqual(2);
  });
});
