const { test, expect } = require('@playwright/test');

const ADMIN_PIN = process.env.ADMIN_PIN || '1111';
const BASE_URL = process.env.BASE_URL || 'http://localhost:8080/casero';

test.describe('Birthdays Dashboard', () => {
  test('acceder al dashboard y ver tabla de cumpleaños', async ({ page }) => {
    test.setTimeout(60000);
    // Use the first day to include birthdays that have already passed this month.
    const today = new Date();
    const birthdayDay = '1';
    const currentMonth = (today.getMonth() + 1).toString();
    const customerName = `Cumpleañero E2E ${Date.now()}`;

    // 1. Iniciar sesión
    await page.goto(`${BASE_URL}/login`);
    await page.getByTestId('login-pin').fill(ADMIN_PIN);
    await page.waitForURL('**/customers');

    // 2. Crear un usuario de prueba
    await page.getByTestId('nav-customers-new').click();
    await page.getByTestId('customer-name').fill(customerName);
    await page.getByTestId('customer-address').fill('Direccion Test');
    await page.getByTestId('customer-sector').selectOption({ index: 0 });
    await page.getByTestId('customer-create-submit').click();
    await page.waitForURL('**/customers');

    // 3. Set the birthday to the first day of the current month.
    await page.getByTestId('customer-search-input').fill(customerName);
    const createdCard = page.getByTestId('customer-card').filter({ hasText: customerName }).first();
    await createdCard.waitFor();
    await createdCard.click();
    await page.getByRole('link', { name: /ver transacciones/i }).click();
    
    // Añadir fecha
    await page.getByRole('link', { name: /añadir fecha de nacimiento/i }).click();
    await page.locator('select[name="day"]').selectOption(birthdayDay);
    await page.locator('select[name="month"]').selectOption(currentMonth);
    await page.getByRole('button', { name: 'Guardar' }).click();
    await page.waitForURL(/\/customers\/\d+$/);
    const customerDetailUrl = page.url();

    // 4. Navegar al Dashboard usando el enlace del menú superior
    await page.getByTestId('nav-dashboard').click();
    await page.waitForURL('**/dashboard');

    // 5. Verify the count includes the birthday on the first day of the month.
    const birthdayCard = page.locator('h3:has-text("Cumpleaños")').locator('..');
    await expect(birthdayCard).toBeVisible();
    
    // Obtenemos el texto del número grande y validamos que no sea "0"
    const countText = await birthdayCard.locator('p').innerText();
    expect(parseInt(countText)).toBeGreaterThanOrEqual(1);

    // 6. Hacer clic en la tarjeta para ir a la tabla detallada
    await birthdayCard.click();
    await page.waitForURL('**/dashboard/birthdays');

    // 7. Verificar que el usuario que acabamos de crear aparezca en la tabla
    const table = page.locator('table');
    await expect(table).toBeVisible();
    await expect(table.locator('td', { hasText: customerName })).toBeVisible();

    // 8. Open the exact customer detail page from the birthday list.
    const customerLink = table.getByRole('link', { name: customerName, exact: true });
    await expect(customerLink).toBeVisible();
    await customerLink.click();
    await page.waitForURL(customerDetailUrl);
    await expect(page.locator('.customer-header h2')).toContainText(customerName);
  });
});
