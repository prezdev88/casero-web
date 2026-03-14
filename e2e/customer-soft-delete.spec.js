const { test, expect } = require('@playwright/test');

const ADMIN_PIN = process.env.ADMIN_PIN || '1111';
const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';

test.describe('Borrado lógico de cliente', () => {
  test('oculta al cliente del sistema después de eliminarlo', async ({ page }) => {
    const customerName = `E2E Soft Delete ${Date.now()}`;
    const customerAddress = 'Dirección soft delete E2E';

    await page.goto(`${BASE_URL}/login`);
    await page.getByTestId('login-pin').fill(ADMIN_PIN);
    await page.waitForURL('**/customers');

    await page.getByTestId('nav-customers-new').click();
    await page.waitForURL('**/customers/new');
    await page.getByTestId('customer-name').fill(customerName);
    await page.getByTestId('customer-address').fill(customerAddress);
    await page.getByTestId('customer-sector').selectOption({ index: 0 });
    await page.getByTestId('customer-create-submit').click();

    await page.waitForURL('**/customers');
    await page.getByTestId('customer-search-input').fill(customerName);

    const createdCard = page.getByTestId('customer-card').filter({ hasText: customerName }).first();
    await createdCard.waitFor();
    await createdCard.click();
    await page.getByRole('link', { name: /ver transacciones/i }).click();
    await page.waitForURL(/\/customers\/\d+$/);

    await page.getByTestId('customer-action-sale').click();
    await page.waitForURL(/\/actions\/sale$/);
    await page.getByTestId('sale-detail').fill('Venta soft delete E2E');
    await page.getByTestId('sale-items').fill('1');
    await page.getByTestId('sale-amount').fill('900');
    await page.getByTestId('sale-submit').click();
    await page.waitForURL(/\/customers\/\d+$/);

    await Promise.all([
      page.waitForURL('**/customers'),
      (async () => {
        await page.getByTestId('customer-delete-submit').click();
        await page.getByTestId('confirm-delete-accept').click();
      })()
    ]);

    await page.getByTestId('customer-search-input').fill(customerName);
    await expect(page.getByTestId('customer-card')).toHaveCount(0);
    await expect(page.locator('#results-empty')).toContainText('Sin resultados');

    await page.goto(`${BASE_URL}/transactions`);
    await page.waitForURL('**/transactions');
    await expect(page.locator('body')).not.toContainText(customerName);
  });
});
