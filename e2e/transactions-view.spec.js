const { test, expect } = require('@playwright/test');

const ADMIN_PIN = process.env.ADMIN_PIN || '1111';
const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';

const parseMoney = (text) => {
  const digits = text.replace(/[^\d-]/g, '');
  return digits ? parseInt(digits, 10) : 0;
};

test.describe('Vista de transacciones con filtros', () => {
  test('listar y filtrar por tipo (venta, abono, devolución, descuento por falla, condonación)', async ({ page }) => {
    const customerName = `E2E Tx View ${Date.now()}`;
    const saleAmount = 3000;
    const paymentAmount = 500;
    const refundAmount = 400;
    const faultDiscountAmount = 300;
    const today = new Date().toISOString().slice(0, 10);

    await page.goto(`${BASE_URL}/login`);
    await page.getByTestId('login-pin').fill(ADMIN_PIN);
    await page.waitForURL('**/customers');

    // Crear cliente base
    await page.getByTestId('nav-customers-new').click();
    await page.waitForURL('**/customers/new');
    await page.getByTestId('customer-name').fill(customerName);
    await page.getByTestId('customer-address').fill('Dirección transacciones E2E');
    await page.getByTestId('customer-sector').selectOption({ index: 0 });
    await page.getByTestId('customer-create-submit').click();

    await page.waitForURL('**/customers');
    await page.getByTestId('customer-search-input').fill(customerName);
    const createdCard = page.getByTestId('customer-card').filter({ hasText: customerName }).first();
    await createdCard.waitFor();
    await createdCard.click();
    await page.getByRole('link', { name: /ver transacciones/i }).click();
    await page.waitForURL(/\/customers\/\d+$/);

    // Registrar venta (SALE)
    await page.getByTestId('customer-action-sale').click();
    await page.waitForURL(/\/actions\/sale$/);
    await page.getByTestId('sale-detail').fill('Venta Tx View');
    await page.getByTestId('sale-items').fill('1');
    await page.getByTestId('sale-amount').fill(String(saleAmount));
    await page.getByTestId('sale-submit').click();
    await page.waitForURL(/\/customers\/\d+$/);

    // Registrar abono (PAYMENT)
    await page.getByTestId('customer-action-payment').click();
    await page.waitForURL(/\/actions\/payment$/);
    await page.getByTestId('payment-amount').fill(String(paymentAmount));
    await page.getByTestId('payment-submit').click();
    await page.waitForURL(/\/customers\/\d+$/);

    // Registrar devolución (REFUND)
    await page.getByTestId('customer-action-refund').click();
    await page.waitForURL(/\/actions\/refund$/);
    await page.getByTestId('refund-amount').fill(String(refundAmount));
    await page.getByTestId('refund-date').fill(today);
    await page.getByTestId('refund-detail').fill('Devolución Tx View');
    await page.getByTestId('refund-submit').click();
    await page.waitForURL(/\/customers\/\d+$/);

    // Registrar descuento por falla (FAULT_DISCOUNT)
    await page.getByTestId('customer-action-fault-discount').click();
    await page.waitForURL(/\/actions\/fault-discount$/);
    await page.getByTestId('fault-detail').fill('Falla Tx View');
    await page.getByTestId('fault-date').fill(today);
    await page.getByTestId('fault-amount').fill(String(faultDiscountAmount));
    await page.getByTestId('fault-submit').click();
    await page.waitForURL(/\/customers\/\d+$/);

    // Condonar deuda (DEBT_FORGIVENESS)
    await page.getByTestId('customer-action-forgiveness').click();
    await page.waitForURL(/\/actions\/forgiveness$/);
    await page.getByTestId('forgiveness-reason').selectOption({ value: 'Descuento por pago contado' });
    await page.getByTestId('forgiveness-date').fill(today);
    await page.getByTestId('forgiveness-submit').click();
    await page.waitForURL(/\/customers\/\d+$/);

    // Ir a la vista general de transacciones
    await page.goto(`${BASE_URL}/transactions`);
    await page.waitForURL('**/transactions');

    const filterSelect = page.getByTestId('transactions-filter-type');
    await expect(filterSelect).toHaveValue('');

    const details = {
      SALE: 'Venta Tx View',
      PAYMENT: '[Abono]: $500',
      REFUND: 'Devolución Tx View',
      FAULT_DISCOUNT: 'Falla Tx View',
      DEBT_FORGIVENESS: 'Descuento por pago contado'
    };

    const cellForDetail = (text) => page.locator('td', { hasText: text }).first();

    const expectRowsForType = async (typeValue, detailText) => {
      await page.getByTestId('transactions-filter-type').selectOption(typeValue ? { value: typeValue } : { value: '' });
      await Promise.all([
        page.waitForNavigation(),
        page.getByTestId('transactions-filter-submit').click()
      ]);

      if (!typeValue) {
        await expect(page.getByTestId('transactions-filter-type')).toHaveValue('');
        const rows = page.getByTestId('transactions-row');
        const rowTypes = await rows.evaluateAll((trs) => trs.map((tr) => tr.dataset.type));
        for (const [expectedType, text] of Object.entries(details)) {
          expect(rowTypes).toContain(expectedType);
          await expect(cellForDetail(text)).toBeVisible();
        }
        return;
      }

      const rows = page.getByTestId('transactions-row');
      const rowCount = await rows.count();
      expect(rowCount).toBeGreaterThan(0);
      const types = await rows.evaluateAll((trs) => trs.map((tr) => tr.dataset.type));
      expect(types.every((t) => t === typeValue)).toBe(true);
      await expect(cellForDetail(detailText)).toBeVisible();
    };

    await expectRowsForType('', '');
    await expectRowsForType('SALE', details.SALE);
    await expectRowsForType('PAYMENT', details.PAYMENT);
    await expectRowsForType('REFUND', details.REFUND);
    await expectRowsForType('FAULT_DISCOUNT', details.FAULT_DISCOUNT);
    await expectRowsForType('DEBT_FORGIVENESS', details.DEBT_FORGIVENESS);
  });
});
