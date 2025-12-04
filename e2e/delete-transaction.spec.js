const { test, expect } = require('@playwright/test');

const ADMIN_PIN = process.env.ADMIN_PIN || '1111';
const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';

const parseMoney = (text) => {
  const digits = text.replace(/[^\d-]/g, '');
  return digits ? parseInt(digits, 10) : 0;
};

test.describe('Flujo eliminar transacción', () => {
  test('crear venta y eliminarla dejando saldo en cero', async ({ page }) => {
    const customerName = `E2E Delete Tx ${Date.now()}`;
    const saleAmount = 1800;

    await page.goto(`${BASE_URL}/login`);
    await page.getByTestId('login-pin').fill(ADMIN_PIN);
    await page.waitForURL('**/customers');

    // Crear cliente base
    await page.getByTestId('nav-customers-new').click();
    await page.waitForURL('**/customers/new');
    await page.getByTestId('customer-name').fill(customerName);
    await page.getByTestId('customer-address').fill('Dirección delete E2E');
    await page.getByTestId('customer-sector').selectOption({ index: 0 });
    await page.getByTestId('customer-create-submit').click();

    await page.waitForURL('**/customers');
    await page.getByTestId('customer-search-input').fill(customerName);
    const createdCard = page.getByTestId('customer-card').filter({ hasText: customerName }).first();
    await createdCard.waitFor();
    await createdCard.click();
    await page.getByRole('link', { name: /ver transacciones/i }).click();
    await page.waitForURL(/\/customers\/\d+$/);

    // Registrar venta para generar saldo
    await page.getByTestId('customer-action-sale').click();
    await page.waitForURL(/\/actions\/sale$/);
    await page.getByTestId('sale-detail').fill('Venta a eliminar E2E');
    await page.getByTestId('sale-items').fill('1');
    await page.getByTestId('sale-amount').fill(String(saleAmount));
    await page.getByTestId('sale-submit').click();
    await page.waitForURL(/\/customers\/\d+$/);

    await page.getByTestId('transactions-list').waitFor({ state: 'visible' });
    const transactionsBefore = page.getByTestId('transaction-card');
    await expect(transactionsBefore).toHaveCount(1);

    const balanceBefore = parseMoney(await page.getByTestId('customer-balance').textContent());
    expect(balanceBefore).toBe(saleAmount);

    // Eliminar transacción (aceptar confirm)
    page.once('dialog', (dialog) => dialog.accept());
    await Promise.all([
      page.waitForURL(/\/customers\/\d+$/),
      page.getByTestId('transaction-delete').click()
    ]);

    // Validar que no queden transacciones y saldo en cero
    await expect(page.getByTestId('transactions-empty')).toHaveText(/sin transacciones/i);
    const transactionsAfter = page.getByTestId('transaction-card');
    await expect(transactionsAfter).toHaveCount(0);

    const balanceAfter = parseMoney(await page.getByTestId('customer-balance').textContent());
    expect(balanceAfter).toBe(0);
  });
});
