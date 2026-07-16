import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { App } from "./App";

function renderApp() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false }
    }
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>
  );
}

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" }
  });
}

describe("authentication journey", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    document.cookie = "XSRF-TOKEN=; Max-Age=0; Path=/";
  });

  it("invites the first visitor to create the initial account", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(
      jsonResponse({ registrationRequired: true })
    ));

    renderApp();

    expect(await screen.findByRole("heading", { name: "Crie a primeira conta" })).toBeVisible();
    expect(screen.getByLabelText("E-mail")).toBeEnabled();
    expect(screen.getByLabelText("Senha")).toHaveAttribute("minlength", "12");
    expect(screen.getByLabelText("Fuso horário")).toBeEnabled();
  });

  it("shows login when the installation already has an account", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ registrationRequired: false }))
      .mockResolvedValueOnce(new Response(null, { status: 401 }));
    vi.stubGlobal("fetch", fetchMock);

    renderApp();

    expect(await screen.findByRole("heading", { name: "Entre no seu espaço" })).toBeVisible();
    expect(screen.getByRole("button", { name: "Entrar" })).toBeEnabled();
  });

  it("creates the initial account and continues to login", async () => {
    document.cookie = "XSRF-TOKEN=token-inicial; Path=/";
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ registrationRequired: true }))
      .mockResolvedValueOnce(new Response(null, { status: 201 }));
    vi.stubGlobal("fetch", fetchMock);

    renderApp();

    fireEvent.change(await screen.findByLabelText("E-mail"), {
      target: { value: "pessoa@example.com" }
    });
    fireEvent.change(screen.getByLabelText("Senha"), {
      target: { value: "uma frase senha segura" }
    });
    fireEvent.click(screen.getByRole("button", { name: "Criar conta" }));

    expect(await screen.findByText("Conta criada. Agora, entre para continuar.")).toBeVisible();
    expect(screen.getByRole("heading", { name: "Entre no seu espaço" })).toBeVisible();
    expect(fetchMock).toHaveBeenLastCalledWith("/api/auth/bootstrap", expect.objectContaining({
      method: "POST"
    }));
    const headers = fetchMock.mock.calls[1]?.[1]?.headers as Headers;
    expect(headers.get("X-XSRF-TOKEN")).toBe("token-inicial");
  });

  it("shows only the identity stored in the authenticated session", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ registrationRequired: false }))
      .mockResolvedValueOnce(jsonResponse({
        id: "d0508bf2-7d0e-467b-a720-b472f43ddf66",
        email: "pessoa@example.com",
        timeZone: "America/Sao_Paulo"
      }));
    vi.stubGlobal("fetch", fetchMock);

    renderApp();

    expect(await screen.findByRole("heading", { name: "Seu espaço está protegido" })).toBeVisible();
    expect(screen.getByText("pessoa@example.com")).toBeVisible();
    expect(screen.getByText("America/Sao_Paulo")).toBeVisible();
    expect(screen.getByRole("button", { name: "Sair" })).toBeEnabled();
  });

  it("submits credentials as a form and opens the authenticated session", async () => {
    document.cookie = "XSRF-TOKEN=token-login; Path=/";
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ registrationRequired: false }))
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(jsonResponse({
        id: "d0508bf2-7d0e-467b-a720-b472f43ddf66",
        email: "pessoa@example.com",
        timeZone: "America/Sao_Paulo"
      }));
    vi.stubGlobal("fetch", fetchMock);

    renderApp();

    fireEvent.change(await screen.findByLabelText("E-mail"), {
      target: { value: "pessoa@example.com" }
    });
    fireEvent.change(screen.getByLabelText("Senha"), {
      target: { value: "uma frase senha segura" }
    });
    fireEvent.click(screen.getByRole("button", { name: "Entrar" }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(4));
    expect(await screen.findByText("pessoa@example.com")).toBeVisible();
    expect(fetchMock.mock.calls[2]?.[1]).toEqual(expect.objectContaining({
      method: "POST",
      body: expect.any(URLSearchParams)
    }));
  });

  it("ends the session and returns to login", async () => {
    document.cookie = "XSRF-TOKEN=token-logout; Path=/";
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ registrationRequired: false }))
      .mockResolvedValueOnce(jsonResponse({
        id: "d0508bf2-7d0e-467b-a720-b472f43ddf66",
        email: "pessoa@example.com",
        timeZone: "America/Sao_Paulo"
      }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);

    renderApp();

    fireEvent.click(await screen.findByRole("button", { name: "Sair" }));

    expect(await screen.findByRole("heading", { name: "Entre no seu espaço" })).toBeVisible();
    expect(screen.getByText("Sessão encerrada com segurança.")).toBeVisible();
    const headers = fetchMock.mock.calls[2]?.[1]?.headers as Headers;
    expect(headers.get("X-XSRF-TOKEN")).toBe("token-logout");
  });
});
