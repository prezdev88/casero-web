const { test, expect } = require('@playwright/test');

const ADMIN_PIN = process.env.ADMIN_PIN || '1111';
const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';

test.describe('Flujo creación de mantención', () => {
  test('login, crear cliente y registrar mantención', async ({ page }) => {
    const customerName = `E2E Mantención ${Date.now()}`;

    await page.goto(`${BASE_URL}/login`);
    await page.getByTestId('login-pin').fill(ADMIN_PIN);
    await page.waitForURL('**/customers');

    // Crear cliente base
    await page.getByTestId('nav-customers-new').click();
    await page.waitForURL('**/customers/new');
    await page.getByTestId('customer-name').fill(customerName);
    await page.getByTestId('customer-address').fill('Dirección mantención E2E');
    await page.getByTestId('customer-sector').selectOption({ index: 0 });
    await page.getByTestId('customer-create-submit').click();

    await page.waitForURL('**/customers');
    await page.getByTestId('customer-search-input').fill(customerName);
    const createdCard = page.getByTestId('customer-card').filter({ hasText: customerName }).first();
    await createdCard.waitFor();
    await createdCard.click();
    await page.getByRole('link', { name: /ver transacciones/i }).click();
    await page.waitForURL(/\/customers\/\d+$/);

    // Venta inicial para generar deuda (marca NEW_SALE en backend)
    await page.getByTestId('customer-action-sale').click();
    await page.waitForURL(/\/actions\/sale$/);
    await page.getByTestId('sale-detail').fill('Venta inicial E2E');
    await page.getByTestId('sale-items').fill('1');
    await page.getByTestId('sale-amount').fill('1000');
    await page.getByTestId('sale-submit').click();
    await page.waitForURL(/\/customers\/\d+$/);

    // Segunda venta: el backend la clasifica como mantención porque ya hay saldo previo
    await page.getByTestId('customer-action-sale').click();
    await page.waitForURL(/\/actions\/sale$/);
    await page.getByTestId('sale-detail').fill('Mantención E2E');
    await page.getByTestId('sale-items').fill('1');
    await page.getByTestId('sale-amount').fill('700');
    await page.getByTestId('sale-submit').click();
    await page.waitForURL(/\/customers\/\d+$/);

    // Verificar que aparezcan ambas transacciones, incluida la mantención
    await page.getByTestId('transactions-list').waitFor({ state: 'visible' });
    const transactions = page.getByTestId('transaction-card');
    await expect(transactions).toHaveCount(2);
    const texts = await transactions.allTextContents();
    expect(texts.join(' ')).toContain('Venta inicial E2E');
    expect(texts.join(' ')).toContain('Mantención E2E');
  });
});
