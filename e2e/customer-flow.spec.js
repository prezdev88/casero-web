const { test, expect } = require('@playwright/test');

const ADMIN_PIN = process.env.ADMIN_PIN || '1111';
const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';

test.describe('Flujo principal usuario normal', () => {
  test('login, crear cliente, venta y abono', async ({ page }) => {
    const customerName = `E2E Cliente ${Date.now()}`;

    await page.goto(`${BASE_URL}/login`);
    await page.getByTestId('login-pin').fill(ADMIN_PIN);
    await page.waitForURL('**/customers');
    await expect(page.getByTestId('nav-customers')).toBeVisible();

    await page.getByTestId('nav-customers-new').click();
    await page.waitForURL('**/customers/new');
    await page.getByTestId('customer-name').fill(customerName);
    await page.getByTestId('customer-address').fill('Direccion E2E');
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
    await page.getByTestId('sale-detail').fill('Mantención E2E');
    await page.getByTestId('sale-items').fill('1');
    await page.getByTestId('sale-amount').fill('1000');
    await page.getByTestId('sale-submit').click();
    await page.waitForURL(/\/customers\/\d+$/);

    await page.getByTestId('customer-action-payment').click();
    await page.waitForURL(/\/actions\/payment$/);
    await page.getByTestId('payment-amount').fill('500');
    await page.getByTestId('payment-submit').click();
    await page.waitForURL(/\/customers\/\d+$/);

    await page.getByTestId('transactions-list').waitFor({ state: 'visible' });
    const transactions = page.getByTestId('transaction-card');
    await expect(transactions).toHaveCount(2);
    const transactionTexts = await transactions.allTextContents();
    expect(transactionTexts.join(' ')).toContain('Venta');
    expect(transactionTexts.join(' ')).toContain('Abono');
  });
});
