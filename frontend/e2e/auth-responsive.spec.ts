import { expect, test } from "@playwright/test";

test("keeps the initial-account form legible without horizontal overflow on a phone", async ({ page }) => {
  await page.route("**/api/auth/bootstrap-status", async (route) => {
    await route.fulfill({ json: { registrationRequired: true } });
  });
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/");

  await expect(page.getByRole("heading", { name: "Crie a primeira conta" })).toBeVisible();
  await expect(page.getByRole("complementary", { name: "Navegação principal" })).toBeHidden();
  await expect(page.getByLabel("Fuso horário")).toBeVisible();

  const hasHorizontalOverflow = await page.evaluate(
    () => document.documentElement.scrollWidth > document.documentElement.clientWidth
  );
  expect(hasHorizontalOverflow).toBe(false);
});

test("uses the study rail for an authenticated desktop session", async ({ page }) => {
  await page.route("**/api/auth/bootstrap-status", async (route) => {
    await route.fulfill({ json: { registrationRequired: false } });
  });
  await page.route("**/api/auth/me", async (route) => {
    await route.fulfill({
      json: {
        id: "d0508bf2-7d0e-467b-a720-b472f43ddf66",
        email: "pessoa@example.com",
        timeZone: "America/Sao_Paulo"
      }
    });
  });
  await page.setViewportSize({ width: 1280, height: 800 });
  await page.goto("/");

  await expect(page.getByRole("complementary", { name: "Navegação principal" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Seu espaço está protegido" })).toBeVisible();
  await expect(page.getByText("pessoa@example.com")).toBeVisible();
});
