const { test, expect } = require('@playwright/test');

const ADMIN_PIN = process.env.ADMIN_PIN || '1111';
const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';

test.describe('Visualización de dirección de cliente', () => {
  test('muestra la dirección en la tarjeta de búsqueda y en el modal de acciones', async ({ page }) => {
    const customerName = `E2E Dirección ${Date.now()}`;
    const customerAddress = 'Dirección visible E2E';

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
    await expect(createdCard).toContainText(customerAddress);

    await createdCard.click();
    await expect(page.locator('#customer-actions-subtitle')).toContainText(customerName);
    await expect(page.locator('#customer-actions-subtitle')).toContainText(customerAddress);
  });
});
