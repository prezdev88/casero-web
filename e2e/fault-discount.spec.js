const { test, expect } = require('@playwright/test');

const ADMIN_PIN = process.env.ADMIN_PIN || '1111';
const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';

const parseMoney = (text) => {
  const digits = text.replace(/[^\d-]/g, '');
  return digits ? parseInt(digits, 10) : 0;
};

test.describe('Flujo descuento por falla', () => {
  test('crear cliente, generar deuda y aplicar descuento por falla con saldo actualizado', async ({ page }) => {
    const customerName = `E2E Falla ${Date.now()}`;
    const saleAmount = 2000;
    const discountAmount = 800;
    const today = new Date().toISOString().slice(0, 10);

    await page.goto(`${BASE_URL}/login`);
    await page.getByTestId('login-pin').fill(ADMIN_PIN);
    await page.waitForURL('**/customers');

    // Crear cliente base
    await page.getByTestId('nav-customers-new').click();
    await page.waitForURL('**/customers/new');
    await page.getByTestId('customer-name').fill(customerName);
    await page.getByTestId('customer-address').fill('Dirección descuento E2E');
    await page.getByTestId('customer-sector').selectOption({ index: 0 });
    await page.getByTestId('customer-create-submit').click();

    await page.waitForURL('**/customers');
    await page.getByTestId('customer-search-input').fill(customerName);
    const createdCard = page.getByTestId('customer-card').filter({ hasText: customerName }).first();
    await createdCard.waitFor();
    await createdCard.click();
    await page.getByRole('link', { name: /ver transacciones/i }).click();
    await page.waitForURL(/\/customers\/\d+$/);

    // Venta inicial para tener deuda
    await page.getByTestId('customer-action-sale').click();
    await page.waitForURL(/\/actions\/sale$/);
    await page.getByTestId('sale-detail').fill('Venta base E2E');
    await page.getByTestId('sale-items').fill('1');
    await page.getByTestId('sale-amount').fill(String(saleAmount));
    await page.getByTestId('sale-submit').click();
    await page.waitForURL(/\/customers\/\d+$/);

    const balanceBeforeText = await page.getByTestId('customer-balance').textContent();
    const balanceBefore = parseMoney(balanceBeforeText);
    expect(balanceBefore).toBe(saleAmount);

    // Aplicar descuento por falla
    await page.getByTestId('customer-action-fault-discount').click();
    await page.waitForURL(/\/actions\/fault-discount$/);
    await page.getByTestId('fault-detail').fill('Descuento por falla E2E');
    await page.getByTestId('fault-date').fill(today);
    await page.getByTestId('fault-amount').fill(String(discountAmount));
    await page.getByTestId('fault-submit').click();
    await page.waitForURL(/\/customers\/\d+$/);

    // Validar saldo y transacción
    const balanceAfterText = await page.getByTestId('customer-balance').textContent();
    const balanceAfter = parseMoney(balanceAfterText);
    expect(balanceAfter).toBe(Math.max(0, saleAmount - discountAmount));

    await page.getByTestId('transactions-list').waitFor({ state: 'visible' });
    const transactions = page.getByTestId('transaction-card');
    await expect(transactions).toHaveCount(2);
    const texts = await transactions.allTextContents();
    expect(texts.join(' ')).toContain('Descuento por falla');
  });
});
