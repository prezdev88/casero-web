const { test, expect } = require('@playwright/test');

const ADMIN_PIN = process.env.ADMIN_PIN || '1111';
const BASE_URL = process.env.BASE_URL || 'http://localhost:8080/casero';

test.describe('Birthdays Dashboard', () => {
  test('acceder al dashboard y ver tabla de cumpleaños', async ({ page }) => {
    test.setTimeout(60000);
    // Calcular la fecha de hoy para asegurar que caiga dentro de "lo que queda del mes"
    const today = new Date();
    const currentDay = today.getDate().toString();
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

    // 3. Buscarlo y editar su cumpleaños para que sea HOY
    await page.getByTestId('customer-search-input').fill(customerName);
    const createdCard = page.getByTestId('customer-card').filter({ hasText: customerName }).first();
    await createdCard.waitFor();
    await createdCard.click();
    await page.getByRole('link', { name: /ver transacciones/i }).click();
    
    // Añadir fecha
    await page.getByRole('link', { name: /añadir fecha de nacimiento/i }).click();
    await page.locator('select[name="day"]').selectOption(currentDay);
    await page.locator('select[name="month"]').selectOption(currentMonth);
    await page.getByRole('button', { name: 'Guardar' }).click();

    // 4. Navegar al Dashboard usando el enlace del menú superior
    await page.getByTestId('nav-dashboard').click();
    await page.waitForURL('**/dashboard');

    // 5. Verificar que el contador de cumpleaños sea distinto de 0 (porque acabamos de agregar uno para hoy)
    const birthdayCard = page.locator('.stat-card').filter({ hasText: 'Cumpleaños' });
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
  });
});
