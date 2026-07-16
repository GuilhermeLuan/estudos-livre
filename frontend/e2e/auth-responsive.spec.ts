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
  await page.route("**/api/subjects?status=active", async (route) => {
    await route.fulfill({ json: [{
      id: "4b89b888-5b2b-49f7-b82c-f8fc30cdcc51",
      name: "Língua Portuguesa",
      archived: false,
      createdAt: "2026-07-16T12:00:00Z",
      updatedAt: "2026-07-16T12:00:00Z"
    }] });
  });
  await page.setViewportSize({ width: 1280, height: 800 });
  await page.goto("/");

  await expect(page.getByRole("complementary", { name: "Navegação principal" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Minhas matérias" })).toBeVisible();
  await expect(page.getByText("Língua Portuguesa")).toBeVisible();
  await expect(page.getByRole("link", { name: "Matérias" }).first()).toHaveAttribute("aria-current", "page");
});

test("keeps the subject catalog usable without horizontal overflow on a phone", async ({ page }) => {
  await page.route("**/api/auth/bootstrap-status", async (route) => {
    await route.fulfill({ json: { registrationRequired: false } });
  });
  await page.route("**/api/auth/me", async (route) => {
    await route.fulfill({ json: { id: "user", email: "pessoa@example.com", timeZone: "America/Sao_Paulo" } });
  });
  await page.route("**/api/subjects?status=active", async (route) => {
    await route.fulfill({ json: [{
      id: "4b89b888-5b2b-49f7-b82c-f8fc30cdcc51",
      name: "Conhecimentos Bancários e Atualidades do Mercado Financeiro",
      archived: false,
      createdAt: "2026-07-16T12:00:00Z",
      updatedAt: "2026-07-16T12:00:00Z"
    }] });
  });
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/materias");

  await expect(page.getByRole("heading", { name: "Minhas matérias" })).toBeVisible();
  await expect(page.getByRole("navigation", { name: "Navegação móvel" })).toBeVisible();
  await expect(page.getByRole("complementary", { name: "Navegação principal" })).toBeHidden();
  await expect(page.getByText("Conhecimentos Bancários e Atualidades do Mercado Financeiro")).toBeVisible();

  const hasHorizontalOverflow = await page.evaluate(
    () => document.documentElement.scrollWidth > document.documentElement.clientWidth
  );
  expect(hasHorizontalOverflow).toBe(false);
});

test("opens a subject, creates content and keeps the catalog within a phone viewport", async ({ page }) => {
  const subjectId = "4b89b888-5b2b-49f7-b82c-f8fc30cdcc51";
  await page.route("**/api/auth/bootstrap-status", async (route) => {
    await route.fulfill({ json: { registrationRequired: false } });
  });
  await page.route("**/api/auth/me", async (route) => {
    await route.fulfill({ json: { id: "user", email: "pessoa@example.com", timeZone: "America/Sao_Paulo" } });
  });
  await page.route("**/api/subjects?status=active", async (route) => {
    await route.fulfill({ json: [{
      id: subjectId,
      name: "Língua Portuguesa",
      archived: false,
      createdAt: "2026-07-16T12:00:00Z",
      updatedAt: "2026-07-16T12:00:00Z"
    }] });
  });
  await page.route(`**/api/subjects/${subjectId}`, async (route) => {
    await route.fulfill({ json: {
      id: subjectId,
      name: "Língua Portuguesa",
      archived: false,
      createdAt: "2026-07-16T12:00:00Z",
      updatedAt: "2026-07-16T12:00:00Z"
    } });
  });
  await page.route(`**/api/subjects/${subjectId}/contents?status=active`, async (route) => {
    await route.fulfill({ json: [] });
  });
  await page.route(`**/api/subjects/${subjectId}/contents`, async (route) => {
    await route.fulfill({
      status: 201,
      json: {
        id: "017e2d9a-6082-4aee-a3f4-3b43029efc13",
        subjectId,
        name: "Concordância verbal",
        archived: false,
        createdAt: "2026-07-16T12:00:00Z",
        updatedAt: "2026-07-16T12:00:00Z"
      }
    });
  });
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/materias");

  await page.getByRole("link", { name: "Ver conteúdos" }).click();
  await expect(page.getByRole("heading", { name: "Conteúdos de Língua Portuguesa" })).toBeVisible();
  await page.getByRole("button", { name: "Novo conteúdo" }).click();
  await page.getByLabel("Nome do conteúdo").fill("Concordância verbal");
  await page.getByRole("button", { name: "Adicionar conteúdo" }).click();

  await expect(page.getByRole("heading", { name: "Concordância verbal" })).toBeVisible();
  const hasHorizontalOverflow = await page.evaluate(
    () => document.documentElement.scrollWidth > document.documentElement.clientWidth
  );
  expect(hasHorizontalOverflow).toBe(false);
});

test("opens a password-reset link directly without horizontal overflow on a phone", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/redefinir-senha?token=token-operacional");

  await expect(page.getByRole("heading", { name: "Defina uma nova senha" })).toBeVisible();
  await expect(page.getByRole("textbox", { name: "Nova senha" })).toBeVisible();
  await expect(page.getByLabel("E-mail")).toHaveCount(0);

  const hasHorizontalOverflow = await page.evaluate(
    () => document.documentElement.scrollWidth > document.documentElement.clientWidth
  );
  expect(hasHorizontalOverflow).toBe(false);
});
