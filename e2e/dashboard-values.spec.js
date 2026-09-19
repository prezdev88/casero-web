const { test, expect } = require('@playwright/test');

const ADMIN_PIN = process.env.ADMIN_PIN || '1111';
const BASE_URL = process.env.BASE_URL || 'http://localhost:8080/casero';

async function getCardValue(page, title) {
  const text = await page.locator(`h3:has-text("${title}") + p`).innerText();
  return parseInt(text.replace(/[^0-9-]/g, ''), 10) || 0;
}

test.describe('Dashboard Values Logic', () => {
  test('should update dashboard metrics when transactions occur', async ({ page }) => {
    // 1. Log in
    await page.goto(`${BASE_URL}/login`);
    await page.getByTestId('login-pin').fill(ADMIN_PIN);
    await page.waitForURL('**/customers');

    // 2. Read initial dashboard values
    await page.goto(`${BASE_URL}/dashboard`);
    
    const initialActive = await getCardValue(page, 'Clientes Activos');
    const initialDebt = await getCardValue(page, 'Deuda Total');
    const initialDebtors = await getCardValue(page, 'Morosos');
    const initialFinished = await getCardValue(page, 'Cuentas Saldadas');
    const initialItems = await getCardValue(page, 'Prendas Vendidas');
    const initialSales = await getCardValue(page, 'Vendido este mes');
    const initialPayments = await getCardValue(page, 'Cobrado este mes');

    // 3. Create Customer 1 (Moroso & partial payment)
    const customer1 = `C1 E2E ${Date.now()}`;
    await page.goto(`${BASE_URL}/customers/new`);
    await page.getByTestId('customer-name').fill(customer1);
    await page.getByTestId('customer-address').fill('Dir 1');
    await page.getByTestId('customer-sector').selectOption({ index: 0 });
    await page.getByTestId('customer-create-submit').click();
    await page.waitForURL('**/customers');

    // Go to Customer 1 details
    await page.getByTestId('customer-search-input').fill(customer1);
    const card1 = page.getByTestId('customer-card').filter({ hasText: customer1 }).first();
    await card1.waitFor();
    await card1.click();
    await page.getByRole('link', { name: /ver transacciones/i }).click();

    // Add backdated sale (2 months ago) to trigger "Moroso"
    await page.getByTestId('customer-action-sale').click();
    const twoMonthsAgo = new Date();
    twoMonthsAgo.setMonth(twoMonthsAgo.getMonth() - 2);
    const dateStr = twoMonthsAgo.toISOString().split('T')[0];
    
    await page.locator('input[type="date"]').fill(dateStr);
    await page.getByTestId('sale-detail').fill('Venta Antigua');
    await page.getByTestId('sale-items').fill('1');
    await page.getByTestId('sale-amount').fill('10000');
    await page.getByTestId('sale-submit').click();

    // Add current sale
    await page.getByTestId('customer-action-sale').click();
    await page.getByTestId('sale-detail').fill('Venta Actual');
    await page.getByTestId('sale-items').fill('2');
    await page.getByTestId('sale-amount').fill('5000');
    await page.getByTestId('sale-submit').click();

    // 4. Create Customer 2 (Finished Card)
    const customer2 = `C2 E2E ${Date.now()}`;
    await page.goto(`${BASE_URL}/customers/new`);
    await page.getByTestId('customer-name').fill(customer2);
    await page.getByTestId('customer-address').fill('Dir 2');
    await page.getByTestId('customer-sector').selectOption({ index: 0 });
    await page.getByTestId('customer-create-submit').click();
    await page.waitForURL('**/customers');

    await page.getByTestId('customer-search-input').fill(customer2);
    const card2 = page.getByTestId('customer-card').filter({ hasText: customer2 }).first();
    await card2.waitFor();
    await card2.click();
    await page.getByRole('link', { name: /ver transacciones/i }).click();

    // Add current sale
    await page.getByTestId('customer-action-sale').click();
    await page.getByTestId('sale-detail').fill('Venta C2');
    await page.getByTestId('sale-items').fill('1');
    await page.getByTestId('sale-amount').fill('3000');
    await page.getByTestId('sale-submit').click();

    // Add full payment to finish card
    await page.getByTestId('customer-action-payment').click();
    await page.getByTestId('payment-amount').fill('3000');
    await page.getByTestId('payment-submit').click();

    // 5. Verify the new dashboard values
    await page.goto(`${BASE_URL}/dashboard`);
    
    expect(await getCardValue(page, 'Clientes Activos')).toBe(initialActive + 2);
    expect(await getCardValue(page, 'Deuda Total')).toBe(initialDebt + 15000); // (10000 + 5000)
    expect(await getCardValue(page, 'Morosos')).toBe(initialDebtors + 1); // No payment made, so last_payment is NULL and debt > 0
    expect(await getCardValue(page, 'Cuentas Saldadas')).toBe(initialFinished + 1);
    expect(await getCardValue(page, 'Prendas Vendidas')).toBe(initialItems + 3);
    expect(await getCardValue(page, 'Vendido este mes')).toBe(initialSales + 8000); // 5000 + 3000
    expect(await getCardValue(page, 'Cobrado este mes')).toBe(initialPayments + 3000); // Only C2 made a payment
  });
});
