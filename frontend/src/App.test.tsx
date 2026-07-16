import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { App } from "./App";

describe("application status", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("shows the real server and database status", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(JSON.stringify({
      status: "UP",
      database: "UP",
      schemaVersion: "1",
      version: "0.1.0"
    }), { status: 200, headers: { "Content-Type": "application/json" } })));

    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } }
    });

    render(
      <QueryClientProvider client={queryClient}>
        <App />
      </QueryClientProvider>
    );

    expect(await screen.findByRole("heading", { name: "Servidor saudável" })).toBeVisible();
    expect(screen.getByText("PostgreSQL conectado")).toBeVisible();
    expect(screen.getByText("Migração 1 aplicada")).toBeVisible();
  });

  it("explains when the server status cannot be loaded", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 503 })));

    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } }
    });

    render(
      <QueryClientProvider client={queryClient}>
        <App />
      </QueryClientProvider>
    );

    expect(await screen.findByRole("heading", { name: "Servidor indisponível" })).toBeVisible();
    expect(screen.getByRole("button", { name: "Verificar novamente" })).toBeEnabled();
  });
});
