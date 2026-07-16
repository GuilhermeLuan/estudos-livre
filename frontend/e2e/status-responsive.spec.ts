import { expect, test } from "@playwright/test";

const applicationStatus = {
  status: "UP",
  database: "UP",
  schemaVersion: "1",
  version: "0.1.0"
};

test.beforeEach(async ({ page }) => {
  await page.route("**/api/status", async (route) => {
    await route.fulfill({ json: applicationStatus });
  });
});

test("keeps the status legible without horizontal overflow on a phone", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/");

  await expect(page.getByRole("heading", { name: "Servidor saudável" })).toBeVisible();
  await expect(page.getByRole("complementary", { name: "Navegação principal" })).toBeHidden();
  await expect(page.getByText("PostgreSQL conectado")).toBeVisible();

  const hasHorizontalOverflow = await page.evaluate(
    () => document.documentElement.scrollWidth > document.documentElement.clientWidth
  );
  expect(hasHorizontalOverflow).toBe(false);
});

test("uses the study rail on a desktop viewport", async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 800 });
  await page.goto("/");

  await expect(page.getByRole("complementary", { name: "Navegação principal" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Servidor saudável" })).toBeVisible();
});
