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
    window.history.replaceState({}, "", "/");
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

  it("offers public registration only when the operator enabled it", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({
        registrationRequired: false,
        registrationEnabled: true
      }))
      .mockResolvedValueOnce(new Response(null, { status: 401 }));
    vi.stubGlobal("fetch", fetchMock);

    renderApp();

    fireEvent.click(await screen.findByRole("button", { name: "Criar uma conta" }));

    expect(await screen.findByRole("heading", { name: "Crie sua conta" })).toBeVisible();
  });

  it("creates a public account and returns to login", async () => {
    document.cookie = "XSRF-TOKEN=token-cadastro; Path=/";
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({
        registrationRequired: false,
        registrationEnabled: true
      }))
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(new Response(null, { status: 201 }));
    vi.stubGlobal("fetch", fetchMock);

    renderApp();
    fireEvent.click(await screen.findByRole("button", { name: "Criar uma conta" }));
    fireEvent.change(screen.getByLabelText("E-mail"), { target: { value: "pessoa@example.com" } });
    fireEvent.change(screen.getByLabelText("Senha"), { target: { value: "uma frase senha segura" } });
    fireEvent.click(screen.getByRole("button", { name: "Criar conta" }));

    expect(await screen.findByText("Conta criada. Agora, entre para continuar.")).toBeVisible();
    expect(screen.getByRole("heading", { name: "Entre no seu espaço" })).toBeVisible();
    expect(fetchMock).toHaveBeenLastCalledWith("/api/auth/register", expect.objectContaining({
      method: "POST"
    }));
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
    window.history.pushState({}, "", "/conta");
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

  it("opens the authenticated subject catalog with the user's active subjects", async () => {
    window.history.pushState({}, "", "/materias");
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ registrationRequired: false }))
      .mockResolvedValueOnce(jsonResponse({
        id: "d0508bf2-7d0e-467b-a720-b472f43ddf66",
        email: "pessoa@example.com",
        timeZone: "America/Sao_Paulo"
      }))
      .mockResolvedValueOnce(jsonResponse([{
        id: "4b89b888-5b2b-49f7-b82c-f8fc30cdcc51",
        name: "Língua Portuguesa",
        archived: false,
        createdAt: "2026-07-16T12:00:00Z",
        updatedAt: "2026-07-16T12:00:00Z"
      }]));
    vi.stubGlobal("fetch", fetchMock);

    renderApp();

    expect(await screen.findByRole("heading", { name: "Minhas matérias" })).toBeVisible();
    expect(await screen.findByText("Língua Portuguesa")).toBeVisible();
    expect(screen.getByRole("button", { name: "Nova matéria" })).toBeEnabled();
  });

  it("creates a subject and shows it without reloading the application", async () => {
    window.history.pushState({}, "", "/materias");
    document.cookie = "XSRF-TOKEN=token-materia; Path=/";
    const created = {
      id: "4b89b888-5b2b-49f7-b82c-f8fc30cdcc51",
      name: "Direito Administrativo",
      archived: false,
      createdAt: "2026-07-16T12:00:00Z",
      updatedAt: "2026-07-16T12:00:00Z"
    };
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ registrationRequired: false }))
      .mockResolvedValueOnce(jsonResponse({
        id: "d0508bf2-7d0e-467b-a720-b472f43ddf66",
        email: "pessoa@example.com",
        timeZone: "America/Sao_Paulo"
      }))
      .mockResolvedValueOnce(jsonResponse([]))
      .mockResolvedValueOnce(jsonResponse(created, 201));
    vi.stubGlobal("fetch", fetchMock);

    renderApp();
    fireEvent.click(await screen.findByRole("button", { name: "Nova matéria" }));
    fireEvent.change(screen.getByLabelText("Nome da matéria"), {
      target: { value: "Direito Administrativo" }
    });
    fireEvent.click(screen.getByRole("button", { name: "Adicionar matéria" }));

    expect(await screen.findByText("Direito Administrativo")).toBeVisible();
    expect(fetchMock).toHaveBeenLastCalledWith("/api/subjects", expect.objectContaining({
      method: "POST"
    }));
  });

  it("renames a subject in place", async () => {
    window.history.pushState({}, "", "/materias");
    const original = {
      id: "4b89b888-5b2b-49f7-b82c-f8fc30cdcc51",
      name: "Português",
      archived: false,
      createdAt: "2026-07-16T12:00:00Z",
      updatedAt: "2026-07-16T12:00:00Z"
    };
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ registrationRequired: false }))
      .mockResolvedValueOnce(jsonResponse({ id: "user", email: "pessoa@example.com", timeZone: "America/Sao_Paulo" }))
      .mockResolvedValueOnce(jsonResponse([original]))
      .mockResolvedValueOnce(jsonResponse({ ...original, name: "Língua Portuguesa" }));
    vi.stubGlobal("fetch", fetchMock);

    renderApp();
    fireEvent.click(await screen.findByRole("button", { name: "Editar Português" }));
    const input = screen.getByLabelText("Editar nome da matéria");
    fireEvent.change(input, { target: { value: "Língua Portuguesa" } });
    fireEvent.click(screen.getByRole("button", { name: "Salvar alteração" }));

    expect(await screen.findByText("Língua Portuguesa")).toBeVisible();
    expect(screen.queryByText("Português")).not.toBeInTheDocument();
  });

  it("moves a subject between active and archived catalogs", async () => {
    window.history.pushState({}, "", "/materias");
    const subject = {
      id: "4b89b888-5b2b-49f7-b82c-f8fc30cdcc51",
      name: "Matemática",
      archived: false,
      createdAt: "2026-07-16T12:00:00Z",
      updatedAt: "2026-07-16T12:00:00Z"
    };
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ registrationRequired: false }))
      .mockResolvedValueOnce(jsonResponse({ id: "user", email: "pessoa@example.com", timeZone: "America/Sao_Paulo" }))
      .mockResolvedValueOnce(jsonResponse([subject]))
      .mockResolvedValueOnce(jsonResponse({ ...subject, archived: true }))
      .mockResolvedValueOnce(jsonResponse({ ...subject, archived: false }));
    vi.stubGlobal("fetch", fetchMock);

    renderApp();
    fireEvent.click(await screen.findByRole("button", { name: "Arquivar Matemática" }));
    fireEvent.click(screen.getByRole("button", { name: "Confirmar arquivamento" }));
    await waitFor(() => expect(screen.queryByText("Matemática")).not.toBeInTheDocument());

    fireEvent.click(screen.getByRole("tab", { name: "Arquivadas" }));
    expect(await screen.findByText("Matemática")).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: "Restaurar Matemática" }));
    await waitFor(() => expect(screen.queryByText("Matemática")).not.toBeInTheDocument());

    fireEvent.click(screen.getByRole("tab", { name: "Ativas" }));
    expect(await screen.findByText("Matemática")).toBeVisible();
  });

  it("opens the selected subject content catalog", async () => {
    const subjectId = "4b89b888-5b2b-49f7-b82c-f8fc30cdcc51";
    window.history.pushState({}, "", `/materias/${subjectId}/conteudos`);
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ registrationRequired: false }))
      .mockResolvedValueOnce(jsonResponse({
        id: "user",
        email: "pessoa@example.com",
        timeZone: "America/Sao_Paulo"
      }))
      .mockResolvedValueOnce(jsonResponse({
        id: subjectId,
        name: "Língua Portuguesa",
        archived: false,
        createdAt: "2026-07-16T12:00:00Z",
        updatedAt: "2026-07-16T12:00:00Z"
      }))
      .mockResolvedValueOnce(jsonResponse([{
        id: "017e2d9a-6082-4aee-a3f4-3b43029efc13",
        subjectId,
        name: "Concordância verbal",
        archived: false,
        createdAt: "2026-07-16T12:00:00Z",
        updatedAt: "2026-07-16T12:00:00Z"
      }]));
    vi.stubGlobal("fetch", fetchMock);

    renderApp();

    expect(await screen.findByRole("heading", { name: "Conteúdos de Língua Portuguesa" })).toBeVisible();
    expect(await screen.findByText("Concordância verbal")).toBeVisible();
    expect(screen.getByRole("button", { name: "Novo conteúdo" })).toBeEnabled();
  });

  it("creates content inside the selected subject without reloading the application", async () => {
    const subjectId = "4b89b888-5b2b-49f7-b82c-f8fc30cdcc51";
    window.history.pushState({}, "", `/materias/${subjectId}/conteudos`);
    document.cookie = "XSRF-TOKEN=token-conteudo; Path=/";
    const created = {
      id: "017e2d9a-6082-4aee-a3f4-3b43029efc13",
      subjectId,
      name: "Concordância verbal",
      archived: false,
      createdAt: "2026-07-16T12:00:00Z",
      updatedAt: "2026-07-16T12:00:00Z"
    };
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ registrationRequired: false }))
      .mockResolvedValueOnce(jsonResponse({ id: "user", email: "pessoa@example.com", timeZone: "America/Sao_Paulo" }))
      .mockResolvedValueOnce(jsonResponse({
        id: subjectId,
        name: "Língua Portuguesa",
        archived: false,
        createdAt: "2026-07-16T12:00:00Z",
        updatedAt: "2026-07-16T12:00:00Z"
      }))
      .mockResolvedValueOnce(jsonResponse([]))
      .mockResolvedValueOnce(jsonResponse(created, 201));
    vi.stubGlobal("fetch", fetchMock);

    renderApp();
    fireEvent.click(await screen.findByRole("button", { name: "Novo conteúdo" }));
    fireEvent.change(screen.getByLabelText("Nome do conteúdo"), {
      target: { value: "Concordância verbal" }
    });
    fireEvent.click(screen.getByRole("button", { name: "Adicionar conteúdo" }));

    expect(await screen.findByText("Concordância verbal")).toBeVisible();
    expect(fetchMock).toHaveBeenLastCalledWith(
      `/api/subjects/${subjectId}/contents`,
      expect.objectContaining({ method: "POST" })
    );
  });

  it("renames, archives and restores content without leaving the subject", async () => {
    const subjectId = "4b89b888-5b2b-49f7-b82c-f8fc30cdcc51";
    const contentId = "017e2d9a-6082-4aee-a3f4-3b43029efc13";
    window.history.pushState({}, "", `/materias/${subjectId}/conteudos`);
    const original = {
      id: contentId,
      subjectId,
      name: "Concordância",
      archived: false,
      createdAt: "2026-07-16T12:00:00Z",
      updatedAt: "2026-07-16T12:00:00Z"
    };
    const renamed = { ...original, name: "Concordância verbal" };
    const archived = { ...renamed, archived: true };
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ registrationRequired: false }))
      .mockResolvedValueOnce(jsonResponse({ id: "user", email: "pessoa@example.com", timeZone: "America/Sao_Paulo" }))
      .mockResolvedValueOnce(jsonResponse({
        id: subjectId,
        name: "Língua Portuguesa",
        archived: false,
        createdAt: "2026-07-16T12:00:00Z",
        updatedAt: "2026-07-16T12:00:00Z"
      }))
      .mockResolvedValueOnce(jsonResponse([original]))
      .mockResolvedValueOnce(jsonResponse(renamed))
      .mockResolvedValueOnce(jsonResponse(archived))
      .mockResolvedValueOnce(jsonResponse(renamed));
    vi.stubGlobal("fetch", fetchMock);

    renderApp();
    fireEvent.click(await screen.findByRole("button", { name: "Editar Concordância" }));
    fireEvent.change(screen.getByLabelText("Editar nome do conteúdo"), {
      target: { value: "Concordância verbal" }
    });
    fireEvent.click(screen.getByRole("button", { name: "Salvar alteração" }));
    expect(await screen.findByText("Concordância verbal")).toBeVisible();

    fireEvent.click(screen.getByRole("button", { name: "Arquivar Concordância verbal" }));
    fireEvent.click(screen.getByRole("button", { name: "Confirmar arquivamento" }));
    await waitFor(() => expect(screen.queryByText("Concordância verbal")).not.toBeInTheDocument());

    fireEvent.click(screen.getByRole("tab", { name: "Arquivados" }));
    expect(await screen.findByText("Concordância verbal")).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: "Restaurar Concordância verbal" }));
    await waitFor(() => expect(screen.queryByText("Concordância verbal")).not.toBeInTheDocument());

    fireEvent.click(screen.getByRole("tab", { name: "Ativos" }));
    expect(await screen.findByText("Concordância verbal")).toBeVisible();
  });

  it("keeps the content form open and explains a duplicate name conflict", async () => {
    const subjectId = "4b89b888-5b2b-49f7-b82c-f8fc30cdcc51";
    window.history.pushState({}, "", `/materias/${subjectId}/conteudos`);
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ registrationRequired: false }))
      .mockResolvedValueOnce(jsonResponse({ id: "user", email: "pessoa@example.com", timeZone: "America/Sao_Paulo" }))
      .mockResolvedValueOnce(jsonResponse({ id: subjectId, name: "Direito", archived: false }))
      .mockResolvedValueOnce(jsonResponse([]))
      .mockResolvedValueOnce(jsonResponse({ detail: "Já existe um conteúdo ativo com esse nome nesta matéria." }, 409));
    vi.stubGlobal("fetch", fetchMock);

    renderApp();
    fireEvent.click(await screen.findByRole("button", { name: "Novo conteúdo" }));
    fireEvent.change(screen.getByLabelText("Nome do conteúdo"), { target: { value: "Atos administrativos" } });
    fireEvent.click(screen.getByRole("button", { name: "Adicionar conteúdo" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Já existe um conteúdo ativo com esse nome nesta matéria."
    );
    expect(screen.getByLabelText("Nome do conteúdo")).toHaveValue("Atos administrativos");
  });

  it("recovers the content catalog after an error and then shows its empty state", async () => {
    const subjectId = "4b89b888-5b2b-49f7-b82c-f8fc30cdcc51";
    window.history.pushState({}, "", `/materias/${subjectId}/conteudos`);
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ registrationRequired: false }))
      .mockResolvedValueOnce(jsonResponse({ id: "user", email: "pessoa@example.com", timeZone: "America/Sao_Paulo" }))
      .mockResolvedValueOnce(jsonResponse({ id: subjectId, name: "Matemática", archived: false }))
      .mockResolvedValueOnce(jsonResponse({ detail: "Banco temporariamente indisponível." }, 503))
      .mockResolvedValueOnce(jsonResponse([]));
    vi.stubGlobal("fetch", fetchMock);

    renderApp();
    expect(await screen.findByRole("heading", { name: "Os conteúdos não puderam ser abertos" })).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: "Tentar novamente" }));

    expect(await screen.findByRole("heading", { name: "Adicione o primeiro conteúdo" })).toBeVisible();
  });

  it("recovers the subject catalog after a loading error", async () => {
    window.history.pushState({}, "", "/materias");
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ registrationRequired: false }))
      .mockResolvedValueOnce(jsonResponse({ id: "user", email: "pessoa@example.com", timeZone: "America/Sao_Paulo" }))
      .mockResolvedValueOnce(jsonResponse({ detail: "Banco temporariamente indisponível." }, 503))
      .mockResolvedValueOnce(jsonResponse([]));
    vi.stubGlobal("fetch", fetchMock);

    renderApp();
    expect(await screen.findByRole("heading", { name: "O catálogo não pôde ser aberto" })).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: "Tentar novamente" }));

    expect(await screen.findByRole("heading", { name: "Seu catálogo começa aqui" })).toBeVisible();
  });

  it("opens the authenticated custom cycle workspace with the user's drafts", async () => {
    window.history.pushState({}, "", "/ciclos");
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url === "/api/auth/bootstrap-status") {
        return jsonResponse({ registrationRequired: false });
      }
      if (url === "/api/auth/me") {
        return jsonResponse({
          id: "d0508bf2-7d0e-467b-a720-b472f43ddf66",
          email: "pessoa@example.com",
          timeZone: "America/Sao_Paulo"
        });
      }
      if (url === "/api/study-cycles") {
        return jsonResponse([{
          id: "7a725fd0-2429-46a3-a786-f14ef87642a5",
          name: "Ciclo pós-edital",
          mode: "CUSTOM",
          status: "DRAFT",
          totalMinutes: 150,
          activatable: true,
          stages: [],
          createdAt: "2026-07-16T12:00:00Z",
          updatedAt: "2026-07-16T12:00:00Z"
        }]);
      }
      if (url === "/api/subjects?status=active") return jsonResponse([]);
      throw new Error(`Unexpected request: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    renderApp();

    expect(await screen.findByRole("heading", { name: "Ciclos de estudo" })).toBeVisible();
    expect(await screen.findByText("Ciclo pós-edital")).toBeVisible();
    expect(screen.getByText("2h 30min")).toBeVisible();
    expect(screen.getByText("Rascunho")).toBeVisible();
    expect(screen.getAllByRole("link", { name: "Ciclos" })[0]).toHaveAttribute("aria-current", "page");
  });

  it("creates a named custom cycle draft without leaving the workspace", async () => {
    window.history.pushState({}, "", "/ciclos");
    document.cookie = "XSRF-TOKEN=token-ciclo; Path=/";
    const createdCycle = {
      id: "7a725fd0-2429-46a3-a786-f14ef87642a5",
      name: "Reta final",
      mode: "CUSTOM",
      status: "DRAFT",
      totalMinutes: 0,
      activatable: false,
      stages: [],
      createdAt: "2026-07-16T12:00:00Z",
      updatedAt: "2026-07-16T12:00:00Z"
    };
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url === "/api/auth/bootstrap-status") return jsonResponse({ registrationRequired: false });
      if (url === "/api/auth/me") return jsonResponse({ id: "user", email: "pessoa@example.com", timeZone: "America/Sao_Paulo" });
      if (url === "/api/study-cycles" && init?.method === "POST") return jsonResponse(createdCycle, 201);
      if (url === "/api/study-cycles") return jsonResponse([]);
      throw new Error(`Unexpected request: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    renderApp();
    fireEvent.click(await screen.findByRole("button", { name: "Novo ciclo" }));
    fireEvent.change(screen.getByLabelText("Nome do ciclo"), { target: { value: "Reta final" } });
    fireEvent.click(screen.getByRole("button", { name: "Criar rascunho" }));

    expect(await screen.findByText("Reta final")).toBeVisible();
    expect(fetchMock).toHaveBeenCalledWith("/api/study-cycles", expect.objectContaining({ method: "POST" }));
  });

  it("adds removes reorders and saves custom cycle stages with a long-block warning", async () => {
    window.history.pushState({}, "", "/ciclos");
    document.cookie = "XSRF-TOKEN=token-etapas; Path=/";
    const cycle = {
      id: "7a725fd0-2429-46a3-a786-f14ef87642a5",
      name: "Ciclo intensivo",
      mode: "CUSTOM",
      status: "DRAFT",
      totalMinutes: 0,
      activatable: false,
      stages: [],
      createdAt: "2026-07-16T12:00:00Z",
      updatedAt: "2026-07-16T12:00:00Z"
    };
    const subjects = [
      { id: "subject-portuguese", name: "Língua Portuguesa", archived: false },
      { id: "subject-math", name: "Matemática", archived: false }
    ];
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url === "/api/auth/bootstrap-status") return jsonResponse({ registrationRequired: false });
      if (url === "/api/auth/me") return jsonResponse({ id: "user", email: "pessoa@example.com", timeZone: "America/Sao_Paulo" });
      if (url === "/api/subjects?status=active") return jsonResponse(subjects);
      if (url === `/api/study-cycles/${cycle.id}` && init?.method === "PUT") {
        return jsonResponse({
          ...cycle,
          totalMinutes: 240,
          activatable: true,
          stages: [{
            id: "stage-math",
            position: 1,
            subjectId: "subject-math",
            subjectName: "Matemática",
            targetMinutes: 240,
            longBlockWarning: true
          }]
        });
      }
      if (url === "/api/study-cycles") return jsonResponse([cycle]);
      throw new Error(`Unexpected request: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    renderApp();
    fireEvent.click(await screen.findByRole("button", { name: "Editar Ciclo intensivo" }));
    const addStageButton = await screen.findByRole("button", { name: "Adicionar etapa" });
    await waitFor(() => expect(addStageButton).toBeEnabled());
    fireEvent.click(addStageButton);
    fireEvent.click(addStageButton);
    fireEvent.change(screen.getByLabelText("Matéria da etapa 2"), { target: { value: "subject-math" } });
    fireEvent.change(screen.getByLabelText("Duração da etapa 2 em minutos"), { target: { value: "240" } });

    expect(screen.getByText("Bloco longo: considere dividir esta matéria em mais aparições.")).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: "Mover Matemática para cima" }));
    fireEvent.click(screen.getByRole("button", { name: "Remover Língua Portuguesa" }));
    fireEvent.click(screen.getByRole("button", { name: "Salvar ciclo" }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
      `/api/study-cycles/${cycle.id}`,
      expect.objectContaining({ method: "PUT" })
    ));
    const updateCall = fetchMock.mock.calls.find(([url, init]) =>
      url === `/api/study-cycles/${cycle.id}` && init?.method === "PUT"
    );
    expect(JSON.parse(String(updateCall?.[1]?.body))).toEqual({
      name: "Ciclo intensivo",
      stages: [{ subjectId: "subject-math", targetMinutes: 240 }]
    });
    expect(await screen.findByLabelText("Duração total do ciclo: 4h")).toBeVisible();
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
      }))
      .mockResolvedValueOnce(jsonResponse([]));
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
    expect(await screen.findByRole("heading", { name: "Minhas matérias" })).toBeVisible();
    expect(fetchMock.mock.calls[2]?.[1]).toEqual(expect.objectContaining({
      method: "POST",
      body: expect.any(URLSearchParams)
    }));
  });

  it("ends the session and returns to login", async () => {
    window.history.pushState({}, "", "/conta");
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

  it("changes the password without ending the current session", async () => {
    window.history.pushState({}, "", "/conta");
    document.cookie = "XSRF-TOKEN=token-troca; Path=/";
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ registrationRequired: false, registrationEnabled: false }))
      .mockResolvedValueOnce(jsonResponse({
        id: "d0508bf2-7d0e-467b-a720-b472f43ddf66",
        email: "pessoa@example.com",
        timeZone: "America/Sao_Paulo"
      }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);

    renderApp();
    fireEvent.change(await screen.findByLabelText("Senha atual"), {
      target: { value: "uma frase senha segura" }
    });
    fireEvent.change(screen.getByLabelText("Nova senha"), {
      target: { value: "uma nova frase senha segura" }
    });
    fireEvent.click(screen.getByRole("button", { name: "Atualizar senha" }));

    expect(await screen.findByText("Senha atualizada. Esta sessão continua ativa.")).toBeVisible();
    expect(screen.getByRole("heading", { name: "Seu espaço está protegido" })).toBeVisible();
  });

  it("opens the password reset route directly and asks only for the new password", async () => {
    window.history.pushState({}, "", "/redefinir-senha?token=token-do-link");
    vi.stubGlobal("fetch", vi.fn());

    renderApp();

    expect(await screen.findByRole("heading", { name: "Defina uma nova senha" })).toBeVisible();
    expect(screen.getByLabelText("Nova senha")).toBeEnabled();
    expect(screen.queryByLabelText("E-mail")).not.toBeInTheDocument();
  });

  it("resets the password from the link and redirects to login", async () => {
    window.history.pushState({}, "", "/redefinir-senha?token=token-do-link");
    document.cookie = "XSRF-TOKEN=token-reset; Path=/";
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(jsonResponse({ registrationRequired: false, registrationEnabled: false }))
      .mockResolvedValueOnce(new Response(null, { status: 401 }));
    vi.stubGlobal("fetch", fetchMock);

    renderApp();
    fireEvent.change(await screen.findByLabelText("Nova senha"), {
      target: { value: "uma nova frase senha segura" }
    });
    fireEvent.click(screen.getByRole("button", { name: "Redefinir senha" }));

    expect(await screen.findByText("Senha redefinida. Entre com sua nova senha.")).toBeVisible();
    expect(screen.getByRole("heading", { name: "Entre no seu espaço" })).toBeVisible();
    expect(fetchMock).toHaveBeenNthCalledWith(1, "/api/auth/password/reset", expect.objectContaining({
      method: "POST",
      body: JSON.stringify({ token: "token-do-link", newPassword: "uma nova frase senha segura" })
    }));
  });

  it("keeps the visitor on the reset page when the link is invalid", async () => {
    window.history.pushState({}, "", "/redefinir-senha?token=token-invalido");
    document.cookie = "XSRF-TOKEN=token-reset; Path=/";
    vi.stubGlobal("fetch", vi.fn().mockResolvedValueOnce(jsonResponse({
      detail: "Este link de redefinição é inválido ou expirou."
    }, 400)));

    renderApp();
    fireEvent.change(await screen.findByLabelText("Nova senha"), {
      target: { value: "uma nova frase senha segura" }
    });
    fireEvent.click(screen.getByRole("button", { name: "Redefinir senha" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Este link de redefinição é inválido ou expirou."
    );
    expect(screen.getByRole("heading", { name: "Defina uma nova senha" })).toBeVisible();
  });
});
