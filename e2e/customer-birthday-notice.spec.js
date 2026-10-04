const { test, expect } = require('@playwright/test');

const BASE_URL = (process.env.BASE_URL || 'http://localhost:8080/casero').replace(/\/$/, '');
const ADMIN_PIN = process.env.ADMIN_PIN || '1111';

function getTodayInSantiago() {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: 'America/Santiago',
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
  }).formatToParts(new Date());
  const getPart = (type) => Number(parts.find((part) => part.type === type).value);
  return { day: getPart('day'), month: getPart('month'), year: getPart('year') };
}

const scenarios = [
  {
    name: 'muestra la tortita y la edad cuando cumple años hoy',
    birthdate: (today) => ({ ...today, year: today.year - 36 }),
    expectedMessage: 'Hoy cumple 36 años',
  },
  {
    name: 'usa el singular cuando cumple un año',
    birthdate: (today) => ({ ...today, year: today.year - 1 }),
    expectedMessage: 'Hoy cumple 1 año',
  },
  {
    name: 'avisa del cumpleaños sin inventar una edad cuando no hay año',
    birthdate: (today) => ({ day: today.day, month: today.month }),
    expectedMessage: 'Hoy está de cumpleaños',
  },
  {
    name: 'no muestra el aviso para otro día del mismo mes',
    birthdate: (today) => ({ ...today, day: today.day === 1 ? 2 : today.day - 1, year: today.year - 36 }),
    expectedMessage: null,
  },
  {
    name: 'no muestra el aviso para el mismo día de otro mes',
    birthdate: (today) => ({ day: today.day, month: today.month === 1 ? 3 : 1, year: today.year - 36 }),
    expectedMessage: null,
  },
  {
    name: 'no muestra el aviso cuando no hay fecha de nacimiento',
    birthdate: () => null,
    expectedMessage: null,
  },
];

async function expectBirthdayNotice(card, expectedMessage) {
  await expect(card).toBeVisible();
  const notice = card.locator('.customer-card__birthday');
  if (expectedMessage === null) {
    await expect(notice).toHaveCount(0);
    await expect(card).not.toContainText('🎂');
    await expect(card).not.toContainText('Hoy cumple');
    return;
  }
  await expect(notice).toBeVisible();
  await expect(notice).toHaveText(`🎂 ${expectedMessage}`);
}

test.describe('Aviso de cumpleaños en la búsqueda de clientes', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(`${BASE_URL}/login`);
    await page.getByTestId('login-pin').fill(ADMIN_PIN);
    await page.waitForURL('**/customers');
  });

  for (const scenario of scenarios) {
    test(scenario.name, async ({ page }, testInfo) => {
      const birthdate = scenario.birthdate(getTodayInSantiago());
      const customerName = `E2E Birthday Notice ${Date.now()} ${testInfo.retry}`;

      await page.getByTestId('nav-customers-new').click();
      await page.getByTestId('customer-name').fill(customerName);
      await page.getByTestId('customer-address').fill('Dirección E2E cumpleaños');
      await page.getByTestId('customer-sector').selectOption({ index: 0 });
      await page.getByTestId('customer-create-submit').click();
      await page.waitForURL('**/customers');
      await page.getByTestId('customer-search-input').fill(customerName);
      const card = page.getByTestId('customer-card').filter({ hasText: customerName });
      await card.click();
      await page.getByRole('link', { name: /ver transacciones/i }).click();
      await page.waitForURL(/\/customers\/\d+$/);
      const customerDetailUrl = page.url();

      try {
        if (birthdate) {
          await page.getByTestId('customer-action-birthdate').click();
          await page.locator('select[name="day"]').selectOption(String(birthdate.day));
          await page.locator('select[name="month"]').selectOption(String(birthdate.month));
          await page.locator('input[name="year"]').fill(birthdate.year ? String(birthdate.year) : '');
          await page.getByRole('button', { name: 'Guardar' }).click();
          await page.waitForURL(customerDetailUrl);
        }

        // Check cards built by the live search's JSON response.
        await page.getByTestId('nav-customers').click();
        await page.waitForURL('**/customers');
        await page.getByTestId('customer-search-input').fill(customerName);
        await expectBirthdayNotice(card, scenario.expectedMessage);

        // Check the server-rendered list when opening a search URL directly.
        await page.goto(`${BASE_URL}/customers?q=${encodeURIComponent(customerName)}`);
        await expectBirthdayNotice(card, scenario.expectedMessage);
      } finally {
        // Soft-delete only the customer created by this test.
        await page.goto(customerDetailUrl);
        await page.getByTestId('customer-delete-submit').click();
        await page.getByTestId('confirm-delete-accept').click();
        await page.waitForURL('**/customers');
      }
    });
  }
});
