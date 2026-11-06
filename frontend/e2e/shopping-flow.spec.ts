import { test, expect, Page } from '@playwright/test';

// These tests exercise the full "buy something" journey against the real,
// running stack (frontend + gateway + every backend service). They create
// their own seller, their own client, and their own product each run, so
// they don't depend on demo data and can run again and again without
// clashing with a previous run.

const PASSWORD = 'DemoOnly2026!';

function uniqueSuffix(): string {
  return `${Date.now()}-${Math.floor(Math.random() * 10000)}`;
}

async function register(page: Page, role: 'CLIENT' | 'SELLER', email: string, name: string) {
  await page.goto('/register');
  await page.locator(role === 'CLIENT' ? 'label[for="roleClient"]' : 'label[for="roleSeller"]').click();
  await page.getByLabel('Full name').fill(name);
  await page.getByLabel('Email address').fill(email);
  await page.getByLabel('Password', { exact: true }).fill(PASSWORD);
  await page.getByRole('button', { name: 'Create account' }).click();

  // Registration redirects to the login page on success.
  await expect(page).toHaveURL(/\/login$/);
}

async function login(page: Page, email: string) {
  await page.goto('/login');
  await page.getByLabel('Email address').fill(email);
  await page.getByLabel('Password', { exact: true }).fill(PASSWORD);
  await page.getByRole('button', { name: 'Sign in' }).click();

  // A logged-in user is redirected away from /login.
  await expect(page).not.toHaveURL(/\/login$/);
}

async function createProduct(page: Page, name: string) {
  await page.goto('/dashboard');
  await page.getByRole('button', { name: 'Create New Product' }).click();

  await page.getByLabel('Name:').fill(name);
  await page.getByLabel('Description:').fill('Created by an automated end-to-end test.');
  await page.getByLabel('Price:').fill('19.99');
  await page.getByLabel('Quantity:').fill('5');
  // Category keeps its default (Electronics) — not relevant to this flow.

  await page.getByRole('button', { name: 'Submit' }).click();
  await expect(page.getByText('Product created.')).toBeVisible();
}

test.describe('shopping flow', () => {
  test('a client can find a product, buy it on delivery, and see the order', async ({ page }) => {
    const id = uniqueSuffix();
    const sellerEmail = `e2e.seller.${id}@example.com`;
    const clientEmail = `e2e.client.${id}@example.com`;
    const productName = `E2E Test Product ${id}`;

    // --- Seller: register, log in, list a product ---
    await register(page, 'SELLER', sellerEmail, 'E2E Seller');
    await login(page, sellerEmail);
    await createProduct(page, productName);

    // --- Client: register, log in, find that exact product ---
    await register(page, 'CLIENT', clientEmail, 'E2E Client');
    await login(page, clientEmail);

    await page.goto('/');
    await page.getByPlaceholder('Search products…').fill(productName);
    const productCard = page.getByRole('heading', { name: productName });
    await expect(productCard).toBeVisible();

    // --- Add to cart ---
    await page
      .locator('article', { has: productCard })
      .getByRole('button', { name: 'Add to cart' })
      .click();
    await expect(page.getByText(`${productName} added to cart.`)).toBeVisible();

    // --- Cart: the item and its quantity are there ---
    await page.goto('/cart');
    const cartRow = page.locator('li.list-group-item', { hasText: productName });
    await expect(cartRow).toBeVisible();
    await expect(cartRow.getByText('$19.99')).toBeVisible();

    // --- Checkout: address -> review -> confirm, pay on delivery ---
    await page.getByRole('link', { name: 'Go to checkout' }).click();
    await expect(page).toHaveURL(/\/checkout$/);

    await page.getByLabel('Full name').fill('E2E Client');
    await page.getByLabel('Phone').fill('0600000000');
    await page.getByLabel('Street').fill('1 Test Street');
    await page.getByLabel('City').fill('Testville');
    await page.getByLabel('Postal code').fill('00000');
    await page.getByLabel('Country').fill('Testland');
    await page.getByRole('button', { name: 'Continue' }).click();

    // Review step: payment is "pay on delivery" by default.
    await expect(page.getByRole('heading', { name: 'Items' })).toBeVisible();
    await page.getByRole('button', { name: 'Continue' }).click();

    // Confirm step.
    await page.getByRole('button', { name: 'Place order' }).click();

    // --- Done: order placed, shows up in "my orders" ---
    await expect(page.getByText('Thank you, your order is placed.')).toBeVisible();
    const orderLink = page.getByRole('link', { name: /^Order / });
    await expect(orderLink).toBeVisible();
    const orderId = (await orderLink.textContent())?.replace('Order ', '').trim();

    await page.getByRole('link', { name: 'See my orders' }).click();
    await expect(page).toHaveURL(/\/orders$/);
    await expect(page.getByText(`#${orderId?.slice(-6)}`)).toBeVisible();
  });

  test('a cart item survives a page refresh', async ({ page }) => {
    const id = uniqueSuffix();
    const sellerEmail = `e2e.seller.${id}@example.com`;
    const clientEmail = `e2e.client.${id}@example.com`;
    const productName = `E2E Refresh Product ${id}`;

    await register(page, 'SELLER', sellerEmail, 'E2E Seller');
    await login(page, sellerEmail);
    await createProduct(page, productName);

    await register(page, 'CLIENT', clientEmail, 'E2E Client');
    await login(page, clientEmail);

    await page.goto('/');
    await page.getByPlaceholder('Search products…').fill(productName);
    await expect(page.getByRole('heading', { name: productName })).toBeVisible();
    await page
      .locator('article', { has: page.getByRole('heading', { name: productName }) })
      .getByRole('button', { name: 'Add to cart' })
      .click();
    await expect(page.getByText(`${productName} added to cart.`)).toBeVisible();

    await page.goto('/cart');
    const cartRow = page.locator('li.list-group-item', { hasText: productName });
    await expect(cartRow).toBeVisible();

    // Bump the quantity to 2, then reload the page.
    await page.getByRole('button', { name: `Increase quantity of ${productName}` }).click();
    await expect(cartRow.getByText('2', { exact: true })).toBeVisible();

    await page.reload();

    const cartRowAfterReload = page.locator('li.list-group-item', { hasText: productName });
    await expect(cartRowAfterReload).toBeVisible();
    await expect(cartRowAfterReload.getByText('2', { exact: true })).toBeVisible();
  });
});
