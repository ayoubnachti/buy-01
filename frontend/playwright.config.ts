import { defineConfig, devices } from '@playwright/test';

// These tests run against the full app stack (frontend + gateway + every
// backend service), started with `docker compose up -d --wait` — not just
// `ng serve`. Start the stack yourself before running `npm run e2e`.
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  retries: 1,
  reporter: [
    ['list'],
    ['html', { open: 'never' }],
    ['junit', { outputFile: 'test-results/e2e-junit.xml' }],
  ],
  use: {
    baseURL: process.env['E2E_BASE_URL'] ?? 'http://localhost:4200',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
