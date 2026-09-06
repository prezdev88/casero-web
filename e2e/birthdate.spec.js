const { test, expect } = require('@playwright/test');

test.describe('Cumpleaños de clientes', () => {
  test('Añadir y editar fecha de nacimiento', async ({ page }) => {
    const baseUrl = process.env.BASE_URL || 'http://localhost:8080/casero';
    const adminPin = process.env.ADMIN_PIN || '1111';
    const customerName = `E2E Cumple ${Date.now()}`;

    // Login
    await page.goto(`${baseUrl}/login`);
    await page.getByTestId('login-pin').fill(adminPin);
    await page.waitForURL('**/customers');

    // Create a new customer to ensure clean state
    await page.getByTestId('nav-customers-new').click();
    await page.waitForURL('**/customers/new');
    await page.getByTestId('customer-name').fill(customerName);
    await page.getByTestId('customer-address').fill('Direccion E2E');
    await page.getByTestId('customer-sector').selectOption({ index: 1 });
    await page.getByTestId('customer-create-submit').click();
    await page.waitForURL('**/customers');

    // Search and go to detail
    await page.getByTestId('customer-search-input').fill(customerName);
    const createdCard = page.getByTestId('customer-card').filter({ hasText: customerName }).first();
    await createdCard.waitFor();
    await createdCard.click();
    
    // Click "Ver transacciones" from the modal menu
    await page.getByRole('link', { name: /ver transacciones/i }).click();
    await page.waitForURL(/\/customers\/\d+$/);

    // Initial state: Should show "Añadir fecha de nacimiento" link
    const addBirthdateLink = page.getByRole('link', { name: /añadir fecha de nacimiento/i });
    await expect(addBirthdateLink).toBeVisible();
    await addBirthdateLink.click();

    // Fill form
    await page.waitForURL(/\/actions\/birthdate\/edit$/);
    await page.locator('select[name="day"]').selectOption('15');
    await page.locator('select[name="month"]').selectOption('10');
    await page.locator('input[name="year"]').fill('1990');
    await page.getByRole('button', { name: 'Guardar' }).click();

    // Verify it saved and returned to detail view
    await page.waitForURL(/\/customers\/\d+$/);
    
    // Verify birthdate text is displayed
    await expect(page.getByText(/15 de Octubre de 1990/)).toBeVisible();
    await expect(page.getByText(/años\)/)).toBeVisible();

    // Verify the link "Añadir fecha de nacimiento" is no longer visible
    await expect(addBirthdateLink).not.toBeVisible();

    // Edit again using the big action button in the grid
    await page.getByTestId('customer-action-birthdate').click();
    await page.waitForURL(/\/actions\/birthdate\/edit$/);

    // Remove year
    await page.locator('input[name="year"]').fill('');
    await page.getByRole('button', { name: 'Guardar' }).click();
    await page.waitForURL(/\/customers\/\d+$/);

    // Verify it saved without year
    await expect(page.getByText(/15 de Octubre/)).toBeVisible();
    await expect(page.getByText(/1990/)).not.toBeVisible();
    await expect(page.getByText(/años\)/)).not.toBeVisible();

    // Verify it also appears in the search modal menu (list.html)
    await page.getByTestId('nav-customers').click();
    await page.waitForURL('**/customers');
    await page.getByTestId('customer-search-input').fill(customerName);
    const cardAgain = page.getByTestId('customer-card').filter({ hasText: customerName }).first();
    await cardAgain.waitFor();
    await cardAgain.click(); // Abre el modal de acciones
    await expect(page.getByText(/🎂 15 de Octubre/)).toBeVisible();
  });
});
