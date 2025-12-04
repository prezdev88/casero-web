const { test, expect } = require('@playwright/test');

const ADMIN_PIN = process.env.ADMIN_PIN || '1111';
const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';

const parseMoney = (text) => {
  const digits = text.replace(/[^\d-]/g, '');
  return digits ? parseInt(digits, 10) : 0;
};

test.describe('Flujo devolución', () => {
  test('crear cliente, registrar venta y luego devolución con saldo actualizado', async ({ page }) => {
    const customerName = `E2E Devolución ${Date.now()}`;
    const saleAmount = 1500;
    const refundAmount = 600;
    const today = new Date().toISOString().slice(0, 10);

    await page.goto(`${BASE_URL}/login`);
    await page.getByTestId('login-pin').fill(ADMIN_PIN);
    await page.waitForURL('**/customers');

    // Crear cliente base
    await page.getByTestId('nav-customers-new').click();
    await page.waitForURL('**/customers/new');
    await page.getByTestId('customer-name').fill(customerName);
    await page.getByTestId('customer-address').fill('Dirección devolución E2E');
    await page.getByTestId('customer-sector').selectOption({ index: 0 });
    await page.getByTestId('customer-create-submit').click();

    await page.waitForURL('**/customers');
    await page.getByTestId('customer-search-input').fill(customerName);
    const createdCard = page.getByTestId('customer-card').filter({ hasText: customerName }).first();
    await createdCard.waitFor();
    await createdCard.click();
    await page.getByRole('link', { name: /ver transacciones/i }).click();
    await page.waitForURL(/\/customers\/\d+$/);

    // Venta inicial para tener saldo
    await page.getByTestId('customer-action-sale').click();
    await page.waitForURL(/\/actions\/sale$/);
    await page.getByTestId('sale-detail').fill('Venta para devolución E2E');
    await page.getByTestId('sale-items').fill('1');
    await page.getByTestId('sale-amount').fill(String(saleAmount));
    await page.getByTestId('sale-submit').click();
    await page.waitForURL(/\/customers\/\d+$/);

    const balanceBeforeText = await page.getByTestId('customer-balance').textContent();
    const balanceBefore = parseMoney(balanceBeforeText);
    expect(balanceBefore).toBe(saleAmount);

    // Registrar devolución
    await page.getByTestId('customer-action-refund').click();
    await page.waitForURL(/\/actions\/refund$/);
    await page.getByTestId('refund-amount').fill(String(refundAmount));
    await page.getByTestId('refund-date').fill(today);
    await page.getByTestId('refund-detail').fill('Devolución E2E');
    await page.getByTestId('refund-submit').click();
    await page.waitForURL(/\/customers\/\d+$/);

    // Validar saldo actualizado y transacción de devolución
    const balanceAfterText = await page.getByTestId('customer-balance').textContent();
    const balanceAfter = parseMoney(balanceAfterText);
    expect(balanceAfter).toBe(Math.max(0, saleAmount - refundAmount));

    await page.getByTestId('transactions-list').waitFor({ state: 'visible' });
    const transactions = page.getByTestId('transaction-card');
    await expect(transactions).toHaveCount(2);
    const texts = await transactions.allTextContents();
    expect(texts.join(' ')).toContain('Devolución');
  });
});
