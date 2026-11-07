import { test, expect, Page, Browser } from '@playwright/test';

// These tests exercise the full "buy something" journey against the real,
// running stack (frontend + gateway + every backend service). They create
// their own seller, their own client, and their own product each run, so
// they don't depend on demo data and can run again and again without
// clashing with a previous run.
//
// The seller and the client always use separate browser contexts, like two
// different people: a logged-in seller would otherwise be redirected away
// from /register by guestGuard when the client registers.

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
  // The dashboard can show two "Create New Product" buttons (header + empty state, which
  // appears once the product list has loaded); the test id targets the header one, always present.
  await page.getByTestId('create-product').click();

  await page.getByLabel('Name:').fill(name);
  await page.getByLabel('Description:').fill('Created by an automated end-to-end test.');
  await page.getByLabel('Price:').fill('19.99');
  await page.getByLabel('Quantity:').fill('5');
  // Category keeps its default (Electronics) — not relevant to this flow.

  await page.getByRole('button', { name: 'Submit' }).click();
  await expect(page.getByText('Product created.')).toBeVisible();
}

/** A new seller with one product, in its own browser context, closed afterwards. */
async function sellerWithProduct(browser: Browser, email: string, productName: string) {
  const sellerContext = await browser.newContext();
  try {
    const seller = await sellerContext.newPage();
    await register(seller, 'SELLER', email, 'E2E Seller');
    await login(seller, email);
    await createProduct(seller, productName);
  } finally {
    await sellerContext.close();
  }
}

/** Finds the product from the home page search and adds it to the client's cart. */
async function addToCartFromSearch(page: Page, productName: string) {
  await page.goto('/');
  await page.getByPlaceholder('Search products…').fill(productName);
  const productCard = page.getByRole('heading', { name: productName });
  await expect(productCard).toBeVisible();

  await page
    .locator('article', { has: productCard })
    .getByRole('button', { name: 'Add to cart' })
    .click();
  await expect(page.getByText(`${productName} added to cart.`)).toBeVisible();
}

test.describe('shopping flow', () => {
  test('a client can find a product, buy it on delivery, and see the order', async ({ browser, page }) => {
    const id = uniqueSuffix();
    const sellerEmail = `e2e.seller.${id}@example.com`;
    const clientEmail = `e2e.client.${id}@example.com`;
    const productName = `E2E Test Product ${id}`;

    // --- Seller (separate browser): register, log in, list a product ---
    await sellerWithProduct(browser, sellerEmail, productName);

    // --- Client: the flow under test, on the built-in page (screenshots and traces on failure) ---
    await register(page, 'CLIENT', clientEmail, 'E2E Client');
    await login(page, clientEmail);
    await addToCartFromSearch(page, productName);

    // --- Cart: the item and its unit price are there ---
    await page.goto('/cart');
    const cartRow = page.locator('li.list-group-item', { hasText: productName });
    await expect(cartRow).toBeVisible();
    // "each" picks the unit price: with quantity 1 the line total shows the same amount.
    await expect(cartRow.getByText('$19.99 each')).toBeVisible();

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

    // --- Done: order placed (one seller, so one order), shows up in "my orders" ---
    await expect(page.getByText('Thank you, your order is placed.')).toBeVisible();
    const orderLink = page.getByRole('link', { name: /^Order / });
    await expect(orderLink).toBeVisible();
    const orderId = (await orderLink.textContent())?.replace('Order ', '').trim();

    await page.getByRole('link', { name: 'See my orders' }).click();
    await expect(page).toHaveURL(/\/orders$/);
    await expect(page.getByText(`#${orderId?.slice(-6)}`)).toBeVisible();
  });

  test('a cart item survives a page refresh', async ({ browser, page }) => {
    const id = uniqueSuffix();
    const sellerEmail = `e2e.seller.${id}@example.com`;
    const clientEmail = `e2e.client.${id}@example.com`;
    const productName = `E2E Refresh Product ${id}`;

    await sellerWithProduct(browser, sellerEmail, productName);

    await register(page, 'CLIENT', clientEmail, 'E2E Client');
    await login(page, clientEmail);
    await addToCartFromSearch(page, productName);

    await page.goto('/cart');
    const cartRow = page.locator('li.list-group-item', { hasText: productName });
    await expect(cartRow).toBeVisible();

    // Bump the quantity to 2 and wait until the server has saved it: the stepper shows 2 at once
    // but only sends the change after its debounce, so reloading too early would lose it.
    const saved = page.waitForResponse(
      (r) => r.url().includes('/cart/items/') && r.request().method() === 'PUT' && r.ok(),
    );
    await page.getByRole('button', { name: `Increase quantity of ${productName}` }).click();
    await saved;
    await expect(cartRow.getByText('2', { exact: true })).toBeVisible();

    await page.reload();

    const cartRowAfterReload = page.locator('li.list-group-item', { hasText: productName });
    await expect(cartRowAfterReload).toBeVisible();
    await expect(cartRowAfterReload.getByText('2', { exact: true })).toBeVisible();
  });
});