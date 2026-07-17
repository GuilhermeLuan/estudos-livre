import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
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
    const fetchMock = vi.fn(async (input: RequestInfo | URL, _init?: RequestInit) => {
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

  it("shows the current stage, target, progress and remaining time above the complete flow", async () => {
    window.history.pushState({}, "", "/ciclos");
    document.cookie = "XSRF-TOKEN=token-ativacao; Path=/";
    const activeCycle = {
      id: "cycle-active",
      name: "Reta final",
      mode: "CUSTOM",
      status: "ACTIVE",
      totalMinutes: 150,
      activatable: true,
      currentRun: { id: "run-active", number: 3, status: "IN_PROGRESS", startedAt: "2026-07-16T12:00:00Z", currentStagePosition: 1 },
      stages: [
        { id: "stage-1", position: 1, subjectId: "subject-1", subjectName: "Português", targetMinutes: 60, creditedSeconds: 0, longBlockWarning: false },
        { id: "stage-2", position: 2, subjectId: "subject-2", subjectName: "Matemática", targetMinutes: 45, creditedSeconds: 0, longBlockWarning: false },
        { id: "stage-3", position: 3, subjectId: "subject-1", subjectName: "Português", targetMinutes: 45, creditedSeconds: 0, longBlockWarning: false }
      ],
      createdAt: "2026-07-16T12:00:00Z",
      updatedAt: "2026-07-16T12:00:00Z"
    };
    const pausedCycle = {
      ...activeCycle,
      id: "cycle-paused",
      name: "Manutenção",
      status: "INACTIVE",
      currentRun: { id: "run-paused", number: 1, status: "PAUSED", startedAt: "2026-07-15T12:00:00Z" }
    };
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url === "/api/auth/bootstrap-status") return jsonResponse({ registrationRequired: false });
      if (url === "/api/auth/me") return jsonResponse({ id: "user", email: "pessoa@example.com", timeZone: "America/Sao_Paulo" });
      if (url === "/api/study-sessions/current") return new Response(null, { status: 204 });
      if (url === "/api/study-cycles") return jsonResponse([activeCycle, pausedCycle]);
      throw new Error(`Unexpected request: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    renderApp();

    const activeRegion = await screen.findByRole("region", { name: "Ciclo ativo" });
    expect(within(activeRegion).getByRole("heading", { name: "Reta final" })).toBeVisible();
    expect(within(activeRegion).getByText("Volta 3")).toBeVisible();
    const flow = within(activeRegion).getByRole("list", { name: "Fluxo completo da volta" });
    expect(within(flow).getAllByRole("listitem")).toHaveLength(3);
    expect(within(flow).getAllByText("Português")).toHaveLength(2);
    expect(within(flow).getByText("Matemática")).toBeVisible();
    expect(within(flow).getByText("Sugestão")).toBeVisible();
    const currentStage = within(activeRegion).getByRole("region", { name: "Etapa atual: Português" });
    expect(within(currentStage).getByText("Etapa atual")).toBeVisible();
    expect(within(currentStage).getByText("Meta 1h")).toBeVisible();
    expect(within(currentStage).getByText("0min realizados")).toBeVisible();
    expect(within(currentStage).getByText("1h restantes")).toBeVisible();
    expect(within(currentStage).getByRole("button", { name: "Iniciar Português" })).toBeEnabled();
  });

  it("uses the persisted current position and opens the cycle run ledger", async () => {
    window.history.pushState({}, "", "/ciclos");
    const activeCycle = {
      id: "cycle-active",
      name: "Reta final",
      mode: "CUSTOM",
      status: "ACTIVE",
      totalMinutes: 150,
      activatable: true,
      currentRun: {
        id: "run-2",
        number: 2,
        status: "IN_PROGRESS",
        startedAt: "2026-07-17T12:00:00Z",
        currentStagePosition: 2
      },
      stages: [
        { id: "stage-1", position: 1, subjectId: "subject-1", subjectName: "Português", targetMinutes: 60, creditedSeconds: 3600, longBlockWarning: false },
        { id: "stage-2", position: 2, subjectId: "subject-2", subjectName: "Matemática", targetMinutes: 45, creditedSeconds: 600, longBlockWarning: false },
        { id: "stage-3", position: 3, subjectId: "subject-1", subjectName: "Português", targetMinutes: 45, creditedSeconds: 2700, longBlockWarning: false }
      ],
      createdAt: "2026-07-16T12:00:00Z",
      updatedAt: "2026-07-17T12:00:00Z"
    };
    const runs = [
      {
        id: "run-2",
        number: 2,
        status: "IN_PROGRESS",
        startedAt: "2026-07-17T12:00:00Z",
        endedAt: null,
        stages: activeCycle.stages.map((stage) => ({
          ...stage,
          sourceStageId: stage.id,
          targetSeconds: stage.targetMinutes * 60,
          completed: stage.creditedSeconds >= stage.targetMinutes * 60
        }))
      },
      {
        id: "run-1",
        number: 1,
        status: "COMPLETED",
        startedAt: "2026-07-16T12:00:00Z",
        endedAt: "2026-07-16T15:00:00Z",
        stages: [{
          id: "snapshot-1",
          sourceStageId: "stage-1",
          position: 1,
          subjectId: "subject-1",
          subjectName: "Português",
          targetSeconds: 3600,
          creditedSeconds: 3600,
          completed: true
        }]
      }
    ];
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url === "/api/auth/bootstrap-status") return jsonResponse({ registrationRequired: false });
      if (url === "/api/auth/me") return jsonResponse({ id: "user", email: "pessoa@example.com", timeZone: "America/Sao_Paulo" });
      if (url === "/api/study-sessions/current") return new Response(null, { status: 204 });
      if (url === "/api/study-cycles") return jsonResponse([activeCycle]);
      if (url === "/api/study-cycles/cycle-active/runs") return jsonResponse(runs);
      throw new Error(`Unexpected request: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    renderApp();

    const activeRegion = await screen.findByRole("region", { name: "Ciclo ativo" });
    const currentStage = within(activeRegion).getByRole("region", { name: "Etapa atual: Matemática" });
    expect(within(currentStage).getByText("10min realizados")).toBeVisible();
    expect(within(currentStage).getByText("35min restantes")).toBeVisible();
    expect(fetchMock).not.toHaveBeenCalledWith("/api/study-cycles/cycle-active/runs", expect.anything());

    fireEvent.click(within(activeRegion).getByRole("button", { name: "Abrir caderno de voltas" }));

    const ledger = await within(activeRegion).findByRole("region", { name: "Caderno de voltas" });
    expect(await within(ledger).findByText("Volta 2")).toBeVisible();
    expect(within(ledger).getByText("Em andamento")).toBeVisible();
    expect(within(ledger).getByText("Volta 1")).toBeVisible();
    expect(within(ledger).getByText("Concluída")).toBeVisible();
    expect(within(ledger).getAllByText("1h de 1h")).toHaveLength(2);
  });

  it("starts the current cycle stage with one optional content", async () => {
    window.history.pushState({}, "", "/ciclos");
    document.cookie = "XSRF-TOKEN=token-cronometro; Path=/";
    const activeCycle = {
      id: "cycle-active",
      name: "Reta final",
      mode: "CUSTOM",
      status: "ACTIVE",
      totalMinutes: 60,
      activatable: true,
      currentRun: { id: "run-active", number: 1, status: "IN_PROGRESS", startedAt: "2026-07-16T12:00:00Z" },
      stages: [{ id: "stage-1", position: 1, subjectId: "subject-1", subjectName: "Português", targetMinutes: 60, longBlockWarning: false }],
      createdAt: "2026-07-16T12:00:00Z",
      updatedAt: "2026-07-16T12:00:00Z"
    };
    const startedSession = {
      id: "session-1",
      origin: "CYCLE",
      status: "ACTIVE",
      subject: { id: "subject-1", name: "Português" },
      content: { id: "content-1", name: "Concordância verbal" },
      cycle: { id: "cycle-active", name: "Reta final", runId: "run-active", runNumber: 1, stageId: "stage-1", stagePosition: 1, targetMinutes: 60 },
      startedAt: "2026-07-16T12:00:00Z",
      measuredSeconds: 0,
      serverNow: "2026-07-16T12:00:00Z"
    };
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url === "/api/auth/bootstrap-status") return jsonResponse({ registrationRequired: false });
      if (url === "/api/auth/me") return jsonResponse({ id: "user", email: "pessoa@example.com", timeZone: "America/Sao_Paulo" });
      if (url === "/api/study-sessions/current") return new Response(null, { status: 204 });
      if (url === "/api/subjects/subject-1/contents?status=active") return jsonResponse([
        { id: "content-1", subjectId: "subject-1", name: "Concordância verbal", archived: false }
      ]);
      if (url === "/api/study-sessions" && init?.method === "POST") return jsonResponse(startedSession, 201);
      if (url === "/api/study-cycles") return jsonResponse([activeCycle]);
      throw new Error(`Unexpected request: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    renderApp();
    fireEvent.click(await screen.findByRole("button", { name: "Iniciar Português" }));

    const dialog = await screen.findByRole("dialog", { name: "Iniciar etapa atual" });
    expect(within(dialog).getByText("Português")).toBeVisible();
    const contentSelect = await within(dialog).findByLabelText("Conteúdo (opcional)");
    fireEvent.change(contentSelect, { target: { value: "content-1" } });
    fireEvent.click(within(dialog).getByRole("button", { name: "Iniciar cronômetro" }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
      "/api/study-sessions",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ origin: "CYCLE", cycleId: "cycle-active", contentId: "content-1" })
      })
    ));
    const timer = await screen.findByRole("region", { name: "Cronômetro em andamento" });
    expect(within(timer).getByText("Português")).toBeVisible();
    expect(within(timer).getByText("Concordância verbal")).toBeVisible();
    expect(within(timer).getByRole("button", { name: "Pausar" })).toBeEnabled();
  });

  it("starts a free session after choosing its subject", async () => {
    window.history.pushState({}, "", "/ciclos");
    document.cookie = "XSRF-TOKEN=token-livre; Path=/";
    const startedSession = {
      id: "session-free",
      origin: "FREE",
      status: "ACTIVE",
      subject: { id: "subject-law", name: "Direito Constitucional" },
      content: null,
      cycle: null,
      startedAt: "2026-07-16T12:00:00Z",
      measuredSeconds: 0,
      serverNow: "2026-07-16T12:00:00Z"
    };
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url === "/api/auth/bootstrap-status") return jsonResponse({ registrationRequired: false });
      if (url === "/api/auth/me") return jsonResponse({ id: "user", email: "pessoa@example.com", timeZone: "America/Sao_Paulo" });
      if (url === "/api/study-sessions/current") return new Response(null, { status: 204 });
      if (url === "/api/subjects?status=active") return jsonResponse([
        { id: "subject-law", name: "Direito Constitucional", archived: false }
      ]);
      if (url === "/api/subjects/subject-law/contents?status=active") return jsonResponse([]);
      if (url === "/api/study-sessions" && init?.method === "POST") return jsonResponse(startedSession, 201);
      if (url === "/api/study-cycles") return jsonResponse([]);
      throw new Error(`Unexpected request: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    renderApp();
    fireEvent.click(await screen.findByRole("button", { name: "Sessão livre" }));

    const dialog = await screen.findByRole("dialog", { name: "Iniciar sessão livre" });
    const subjectSelect = await within(dialog).findByLabelText("Matéria");
    expect(subjectSelect).toHaveValue("subject-law");
    fireEvent.click(within(dialog).getByRole("button", { name: "Iniciar cronômetro" }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
      "/api/study-sessions",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ origin: "FREE", subjectId: "subject-law" })
      })
    ));
    expect(await screen.findByRole("region", { name: "Cronômetro em andamento" })).toHaveTextContent("Sessão livre");
  });

  it("recovers a paused timer from the backend and resumes it", async () => {
    window.history.pushState({}, "", "/ciclos");
    document.cookie = "XSRF-TOKEN=token-retomada; Path=/";
    const pausedSession = {
      id: "session-paused",
      origin: "FREE",
      status: "PAUSED",
      subject: { id: "subject-1", name: "Matemática" },
      content: null,
      cycle: null,
      startedAt: "2026-07-16T10:00:00Z",
      measuredSeconds: 3723,
      serverNow: "2026-07-16T12:00:00Z"
    };
    const activeSession = { ...pausedSession, status: "ACTIVE", serverNow: "2026-07-16T12:05:00Z" };
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url === "/api/auth/bootstrap-status") return jsonResponse({ registrationRequired: false });
      if (url === "/api/auth/me") return jsonResponse({ id: "user", email: "pessoa@example.com", timeZone: "America/Sao_Paulo" });
      if (url === "/api/study-sessions/current") return jsonResponse(pausedSession);
      if (url === "/api/study-sessions/session-paused/resume" && init?.method === "POST") return jsonResponse(activeSession);
      if (url === "/api/study-cycles") return jsonResponse([]);
      throw new Error(`Unexpected request: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    renderApp();

    const timer = await screen.findByRole("region", { name: "Cronômetro pausado" });
    expect(within(timer).getByText("01:02:03")).toBeVisible();
    fireEvent.click(within(timer).getByRole("button", { name: "Retomar" }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
      "/api/study-sessions/session-paused/resume",
      expect.objectContaining({ method: "POST" })
    ));
    expect(await screen.findByRole("region", { name: "Cronômetro em andamento" })).toBeVisible();
    expect(screen.getByRole("button", { name: "Pausar" })).toBeEnabled();
  });

  it("confirms the measured duration and submits an adjusted effective duration", async () => {
    window.history.pushState({}, "", "/ciclos");
    document.cookie = "XSRF-TOKEN=token-finalizacao; Path=/";
    const activeSession = {
      id: "session-finish",
      origin: "CYCLE",
      status: "ACTIVE",
      subject: { id: "subject-1", name: "Matemática" },
      content: { id: "content-1", name: "Probabilidade" },
      cycle: { id: "cycle-1", name: "Reta final", runId: "run-1", runNumber: 2, stageId: "stage-1", stagePosition: 1, targetMinutes: 60 },
      startedAt: "2026-07-16T10:00:00Z",
      measuredSeconds: 3723,
      effectiveSeconds: null,
      finishedAt: null,
      version: 0,
      credits: [],
      serverNow: "2026-07-16T12:00:00Z"
    };
    const finishedSession = {
      ...activeSession,
      status: "FINISHED",
      effectiveSeconds: 2700,
      finishedAt: "2026-07-16T12:05:00Z",
      version: 1,
      credits: [{ runStageId: "run-stage-1", cycleId: "cycle-1", runId: "run-1", cycleStageId: "stage-1", stagePosition: 1, creditedSeconds: 2700 }]
    };
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url === "/api/auth/bootstrap-status") return jsonResponse({ registrationRequired: false });
      if (url === "/api/auth/me") return jsonResponse({ id: "user", email: "pessoa@example.com", timeZone: "America/Sao_Paulo" });
      if (url === "/api/study-sessions/current") return jsonResponse(activeSession);
      if (url === "/api/study-sessions/session-finish/finish" && init?.method === "POST") return jsonResponse(finishedSession);
      if (url === "/api/study-cycles") return jsonResponse([]);
      throw new Error(`Unexpected request: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    renderApp();
    const timer = await screen.findByRole("region", { name: "Cronômetro em andamento" });
    fireEvent.click(within(timer).getByRole("button", { name: "Finalizar" }));

    const dialog = await screen.findByRole("dialog", { name: "Finalizar sessão" });
    expect(within(dialog).getByText("01:02:03")).toBeVisible();
    expect(within(dialog).getByText("Etapa 01 · Matemática")).toBeVisible();
    const effectiveDuration = within(dialog).getByLabelText("Duração efetiva");
    expect(effectiveDuration).toHaveValue("01:02:03");
    fireEvent.change(effectiveDuration, { target: { value: "00:45:00" } });
    fireEvent.click(within(dialog).getByRole("button", { name: "Finalizar sessão" }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
      "/api/study-sessions/session-finish/finish",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ effectiveSeconds: 2700, expectedVersion: 0 })
      })
    ));
    await waitFor(() => expect(screen.queryByRole("dialog", { name: "Finalizar sessão" })).not.toBeInTheDocument());
    expect(screen.queryByRole("region", { name: "Cronômetro em andamento" })).not.toBeInTheDocument();
  });

  it("asks how to switch cycles and pauses the current run when chosen", async () => {
    window.history.pushState({}, "", "/ciclos");
    document.cookie = "XSRF-TOKEN=token-troca; Path=/";
    const activeCycle = {
      id: "cycle-active",
      name: "Reta final",
      mode: "CUSTOM",
      status: "ACTIVE",
      totalMinutes: 105,
      activatable: true,
      currentRun: { id: "run-active", number: 3, status: "IN_PROGRESS", startedAt: "2026-07-16T12:00:00Z" },
      stages: [{ id: "stage-1", position: 1, subjectId: "subject-1", subjectName: "Português", targetMinutes: 105, longBlockWarning: false }],
      createdAt: "2026-07-16T12:00:00Z",
      updatedAt: "2026-07-16T12:00:00Z"
    };
    const targetCycle = {
      ...activeCycle,
      id: "cycle-target",
      name: "Manutenção",
      status: "INACTIVE",
      currentRun: { id: "run-target", number: 1, status: "PAUSED", startedAt: "2026-07-15T12:00:00Z" }
    };
    const resumedCycle = {
      ...targetCycle,
      status: "ACTIVE",
      currentRun: { ...targetCycle.currentRun, status: "IN_PROGRESS" }
    };
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url === "/api/auth/bootstrap-status") return jsonResponse({ registrationRequired: false });
      if (url === "/api/auth/me") return jsonResponse({ id: "user", email: "pessoa@example.com", timeZone: "America/Sao_Paulo" });
      if (url === "/api/study-cycles/cycle-target/activate" && init?.method === "POST") return jsonResponse(resumedCycle);
      if (url === "/api/study-cycles") return jsonResponse([activeCycle, targetCycle]);
      throw new Error(`Unexpected request: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    renderApp();
    fireEvent.click(await screen.findByRole("button", { name: "Retomar Manutenção" }));

    const dialog = screen.getByRole("dialog", { name: "Como deseja trocar de ciclo?" });
    expect(within(dialog).getByText(/Reta final/)).toBeVisible();
    expect(within(dialog).getByRole("button", { name: "Encerrar volta e trocar" })).toBeVisible();
    expect(fetchMock).not.toHaveBeenCalledWith(
      "/api/study-cycles/cycle-target/activate",
      expect.anything()
    );
    fireEvent.click(within(dialog).getByRole("button", { name: "Pausar e trocar" }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
      "/api/study-cycles/cycle-target/activate",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ currentRunAction: "PAUSE" })
      })
    ));
    const resumedRegion = await screen.findByRole("region", { name: "Ciclo ativo" });
    expect(within(resumedRegion).getByRole("heading", { name: "Manutenção" })).toBeVisible();
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("keeps an active cycle editable but prevents saving it without activities", async () => {
    window.history.pushState({}, "", "/ciclos");
    const activeCycle = {
      id: "cycle-active",
      name: "Reta final",
      mode: "CUSTOM",
      status: "ACTIVE",
      totalMinutes: 60,
      activatable: true,
      currentRun: { id: "run-active", number: 1, status: "IN_PROGRESS", startedAt: "2026-07-16T12:00:00Z" },
      stages: [{ id: "stage-math", position: 1, subjectId: "subject-math", subjectName: "Matemática", targetMinutes: 60, longBlockWarning: false }],
      createdAt: "2026-07-16T12:00:00Z",
      updatedAt: "2026-07-16T12:00:00Z"
    };
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url === "/api/auth/bootstrap-status") return jsonResponse({ registrationRequired: false });
      if (url === "/api/auth/me") return jsonResponse({ id: "user", email: "pessoa@example.com", timeZone: "America/Sao_Paulo" });
      if (url === "/api/subjects?status=active") return jsonResponse([{ id: "subject-math", name: "Matemática", archived: false }]);
      if (url === "/api/study-cycles") return jsonResponse([activeCycle]);
      throw new Error(`Unexpected request: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    renderApp();
    fireEvent.click(await screen.findByRole("button", { name: "Editar Reta final" }));
    fireEvent.click(await screen.findByRole("button", { name: "Remover Matemática" }));

    expect(screen.getByText("O ciclo ativo precisa manter pelo menos uma atividade.")).toBeVisible();
    expect(screen.getByRole("button", { name: "Salvar ciclo" })).toBeDisabled();
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

  it("generates a suggested cycle from subject inputs and explains the result", async () => {
    window.history.pushState({}, "", "/ciclos");
    document.cookie = "XSRF-TOKEN=token-sugestao; Path=/";
    const subjects = [
      { id: "subject-portuguese", name: "Língua Portuguesa", archived: false },
      { id: "subject-law", name: "Direito", archived: false }
    ];
    const suggestedCycle = {
      id: "cycle-suggested",
      name: "Reta final sugerida",
      mode: "SUGGESTED",
      status: "DRAFT",
      totalMinutes: 600,
      activatable: true,
      currentRun: null,
      suggestion: {
        totalMinutes: 600,
        durationRule: "2h por matéria, limitado entre 10h e 30h",
        priorityRule: "questões × peso × dificuldade",
        subjects: [
          { subjectId: "subject-portuguese", subjectName: "Língua Portuguesa", questionCount: 20, weight: 2, difficulty: "EASY", priority: 40, allocatedMinutes: 320, appearanceCount: 2 },
          { subjectId: "subject-law", subjectName: "Direito", questionCount: 10, weight: 1, difficulty: "HARD", priority: 15, allocatedMinutes: 280, appearanceCount: 2 }
        ]
      },
      stages: [
        { id: "stage-1", position: 1, subjectId: "subject-portuguese", subjectName: "Língua Portuguesa", targetMinutes: 160, longBlockWarning: false },
        { id: "stage-2", position: 2, subjectId: "subject-law", subjectName: "Direito", targetMinutes: 140, longBlockWarning: false },
        { id: "stage-3", position: 3, subjectId: "subject-portuguese", subjectName: "Língua Portuguesa", targetMinutes: 160, longBlockWarning: false },
        { id: "stage-4", position: 4, subjectId: "subject-law", subjectName: "Direito", targetMinutes: 140, longBlockWarning: false }
      ],
      createdAt: "2026-07-16T12:00:00Z",
      updatedAt: "2026-07-16T12:00:00Z"
    };
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url === "/api/auth/bootstrap-status") return jsonResponse({ registrationRequired: false });
      if (url === "/api/auth/me") return jsonResponse({ id: "user", email: "pessoa@example.com", timeZone: "America/Sao_Paulo" });
      if (url === "/api/subjects?status=active") return jsonResponse(subjects);
      if (url === "/api/study-cycles/suggestions" && init?.method === "POST") return jsonResponse(suggestedCycle, 201);
      if (url === "/api/study-cycles") return jsonResponse([]);
      throw new Error(`Unexpected request: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    renderApp();
    fireEvent.click(await screen.findByRole("button", { name: "Gerar ciclo sugerido" }));
    fireEvent.change(await screen.findByLabelText("Nome da sugestão"), { target: { value: "Reta final sugerida" } });
    fireEvent.change(screen.getByLabelText("Questões de Língua Portuguesa"), { target: { value: "20" } });
    fireEvent.change(screen.getByLabelText("Peso de Língua Portuguesa"), { target: { value: "2" } });
    fireEvent.change(screen.getByLabelText("Dificuldade de Língua Portuguesa"), { target: { value: "EASY" } });
    fireEvent.change(screen.getByLabelText("Questões de Direito"), { target: { value: "10" } });
    fireEvent.change(screen.getByLabelText("Peso de Direito"), { target: { value: "1" } });
    fireEvent.change(screen.getByLabelText("Dificuldade de Direito"), { target: { value: "HARD" } });
    fireEvent.click(screen.getByRole("button", { name: "Gerar planejamento" }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
      "/api/study-cycles/suggestions",
      expect.objectContaining({ method: "POST" })
    ));
    const suggestionCall = fetchMock.mock.calls.find(([url, init]) =>
      url === "/api/study-cycles/suggestions" && init?.method === "POST"
    );
    expect(JSON.parse(String(suggestionCall?.[1]?.body))).toEqual({
      name: "Reta final sugerida",
      subjects: [
        { subjectId: "subject-portuguese", questionCount: 20, weight: 2, difficulty: "EASY" },
        { subjectId: "subject-law", questionCount: 10, weight: 1, difficulty: "HARD" }
      ]
    });

    expect(await screen.findByText("Reta final sugerida")).toBeVisible();
    fireEvent.click(screen.getByText("Entenda o cálculo"));
    expect(screen.getByText("2h por matéria, limitado entre 10h e 30h")).toBeVisible();
    expect(screen.getByText("Prioridade 40")).toBeVisible();
    expect(screen.getByLabelText("Carga de Língua Portuguesa: 5h 20min")).toBeVisible();
  });

  it("warns before the first manual save turns a suggested cycle into a custom one", async () => {
    window.history.pushState({}, "", "/ciclos");
    const suggestedCycle = {
      id: "cycle-suggested",
      name: "Reta final sugerida",
      mode: "SUGGESTED",
      status: "DRAFT",
      totalMinutes: 600,
      activatable: true,
      currentRun: null,
      suggestion: {
        totalMinutes: 600,
        durationRule: "2h por matéria, limitado entre 10h e 30h",
        priorityRule: "questões × peso × dificuldade",
        subjects: [{ subjectId: "subject-portuguese", subjectName: "Língua Portuguesa", questionCount: 20, weight: 2, difficulty: "MEDIUM", priority: 50, allocatedMinutes: 600, appearanceCount: 4 }]
      },
      stages: [{ id: "stage-1", position: 1, subjectId: "subject-portuguese", subjectName: "Língua Portuguesa", targetMinutes: 150, longBlockWarning: false }],
      createdAt: "2026-07-16T12:00:00Z",
      updatedAt: "2026-07-16T12:00:00Z"
    };
    const customizedCycle = {
      ...suggestedCycle,
      mode: "CUSTOM",
      totalMinutes: 120,
      suggestion: null,
      stages: [{ ...suggestedCycle.stages[0], targetMinutes: 120 }]
    };
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url === "/api/auth/bootstrap-status") return jsonResponse({ registrationRequired: false });
      if (url === "/api/auth/me") return jsonResponse({ id: "user", email: "pessoa@example.com", timeZone: "America/Sao_Paulo" });
      if (url === "/api/subjects?status=active") return jsonResponse([{ id: "subject-portuguese", name: "Língua Portuguesa", archived: false }]);
      if (url === `/api/study-cycles/${suggestedCycle.id}` && init?.method === "PUT") return jsonResponse(customizedCycle);
      if (url === "/api/study-cycles") return jsonResponse([suggestedCycle]);
      throw new Error(`Unexpected request: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    renderApp();
    fireEvent.click(await screen.findByRole("button", { name: "Editar Reta final sugerida" }));

    expect(await screen.findByRole("status", { name: "Mudança de modo" })).toHaveTextContent(
      "Ao salvar, este ciclo passará a personalizado"
    );
    fireEvent.change(screen.getByLabelText("Duração da atividade 1 em minutos"), { target: { value: "120" } });
    fireEvent.click(screen.getByRole("button", { name: "Salvar ciclo" }));

    expect(await screen.findByText("Ciclo salvo como personalizado.")).toBeVisible();
  });

  it("previews regeneration impact and cancels without replacing a custom plan", async () => {
    window.history.pushState({}, "", "/ciclos");
    const cycle = {
      id: "cycle-custom",
      name: "Minha organização",
      mode: "CUSTOM",
      status: "DRAFT",
      totalMinutes: 90,
      activatable: true,
      currentRun: null,
      suggestion: null,
      stages: [{ id: "stage-1", position: 1, subjectId: "subject-portuguese", subjectName: "Língua Portuguesa", targetMinutes: 90, longBlockWarning: false }],
      createdAt: "2026-07-16T12:00:00Z",
      updatedAt: "2026-07-16T12:00:00Z"
    };
    const fetchMock = vi.fn(async (input: RequestInfo | URL, _init?: RequestInit) => {
      const url = String(input);
      if (url === "/api/auth/bootstrap-status") return jsonResponse({ registrationRequired: false });
      if (url === "/api/auth/me") return jsonResponse({ id: "user", email: "pessoa@example.com", timeZone: "America/Sao_Paulo" });
      if (url === "/api/subjects?status=active") return jsonResponse([
        { id: "subject-portuguese", name: "Língua Portuguesa", archived: false },
        { id: "subject-law", name: "Direito", archived: false }
      ]);
      if (url === "/api/study-cycles") return jsonResponse([cycle]);
      throw new Error(`Unexpected request: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    renderApp();
    fireEvent.click(await screen.findByRole("button", { name: "Editar Minha organização" }));
    fireEvent.click(await screen.findByRole("button", { name: "Gerar nova sugestão" }));

    const dialog = screen.getByRole("dialog", { name: "Preparar nova sugestão" });
    expect(within(dialog).getByLabelText("Planejamento atual: 1h 30min")).toBeVisible();
    expect(within(dialog).getByLabelText("Nova sugestão estimada: 10h")).toBeVisible();
    expect(within(dialog).getByText("As atividades atuais só serão substituídas depois da sua confirmação.")).toBeVisible();
    fireEvent.click(within(dialog).getByRole("button", { name: "Cancelar" }));

    expect(screen.queryByRole("dialog", { name: "Preparar nova sugestão" })).not.toBeInTheDocument();
    expect(screen.getByLabelText("Duração total do ciclo: 1h 30min")).toBeVisible();
    expect(fetchMock.mock.calls.some(([url, init]) =>
      url === `/api/study-cycles/${cycle.id}/suggestion` && init?.method === "PUT"
    )).toBe(false);
  });

  it("requires a second confirmation before regenerating and returning the cycle to suggested mode", async () => {
    window.history.pushState({}, "", "/ciclos");
    document.cookie = "XSRF-TOKEN=token-regeneracao; Path=/";
    const cycle = {
      id: "cycle-custom",
      name: "Minha organização",
      mode: "CUSTOM",
      status: "DRAFT",
      totalMinutes: 90,
      activatable: true,
      currentRun: null,
      suggestion: null,
      stages: [{ id: "stage-1", position: 1, subjectId: "subject-portuguese", subjectName: "Língua Portuguesa", targetMinutes: 90, longBlockWarning: false }],
      createdAt: "2026-07-16T12:00:00Z",
      updatedAt: "2026-07-16T12:00:00Z"
    };
    const regenerated = {
      ...cycle,
      mode: "SUGGESTED",
      totalMinutes: 600,
      suggestion: {
        totalMinutes: 600,
        durationRule: "2h por matéria, limitado entre 10h e 30h",
        priorityRule: "questões × peso × dificuldade",
        subjects: [{ subjectId: "subject-portuguese", subjectName: "Língua Portuguesa", questionCount: 25, weight: 2, difficulty: "HARD", priority: 75, allocatedMinutes: 600, appearanceCount: 4 }]
      },
      stages: [{ id: "new-stage-1", position: 1, subjectId: "subject-portuguese", subjectName: "Língua Portuguesa", targetMinutes: 150, longBlockWarning: false }]
    };
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url === "/api/auth/bootstrap-status") return jsonResponse({ registrationRequired: false });
      if (url === "/api/auth/me") return jsonResponse({ id: "user", email: "pessoa@example.com", timeZone: "America/Sao_Paulo" });
      if (url === "/api/subjects?status=active") return jsonResponse([{ id: "subject-portuguese", name: "Língua Portuguesa", archived: false }]);
      if (url === `/api/study-cycles/${cycle.id}/suggestion` && init?.method === "PUT") return jsonResponse(regenerated);
      if (url === "/api/study-cycles") return jsonResponse([cycle]);
      throw new Error(`Unexpected request: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    renderApp();
    fireEvent.click(await screen.findByRole("button", { name: "Editar Minha organização" }));
    fireEvent.click(await screen.findByRole("button", { name: "Gerar nova sugestão" }));
    const preparation = screen.getByRole("dialog", { name: "Preparar nova sugestão" });
    fireEvent.change(within(preparation).getByLabelText("Questões de Língua Portuguesa"), { target: { value: "25" } });
    fireEvent.change(within(preparation).getByLabelText("Peso de Língua Portuguesa"), { target: { value: "2" } });
    fireEvent.change(within(preparation).getByLabelText("Dificuldade de Língua Portuguesa"), { target: { value: "HARD" } });
    fireEvent.click(within(preparation).getByRole("button", { name: "Revisar substituição" }));

    const confirmation = screen.getByRole("dialog", { name: "Confirmar nova sugestão" });
    expect(within(confirmation).getByText("Seu planejamento personalizado de 1h 30min será substituído por uma sugestão estimada de 10h.")).toBeVisible();
    expect(fetchMock.mock.calls.some(([url, init]) =>
      url === `/api/study-cycles/${cycle.id}/suggestion` && init?.method === "PUT"
    )).toBe(false);
    fireEvent.click(within(confirmation).getByRole("button", { name: "Confirmar regeneração" }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
      `/api/study-cycles/${cycle.id}/suggestion`,
      expect.objectContaining({ method: "PUT" })
    ));
    const regenerationCall = fetchMock.mock.calls.find(([url, init]) =>
      url === `/api/study-cycles/${cycle.id}/suggestion` && init?.method === "PUT"
    );
    expect(JSON.parse(String(regenerationCall?.[1]?.body))).toEqual({
      name: "Minha organização",
      subjects: [{ subjectId: "subject-portuguese", questionCount: 25, weight: 2, difficulty: "HARD" }]
    });
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(await screen.findByText("Entenda o cálculo")).toBeVisible();
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
    const addStageButton = await screen.findByRole("button", { name: "Adicionar atividade" });
    await waitFor(() => expect(addStageButton).toBeEnabled());
    fireEvent.click(addStageButton);
    fireEvent.click(addStageButton);
    fireEvent.change(screen.getByLabelText("Matéria da atividade 2"), { target: { value: "subject-math" } });
    fireEvent.change(screen.getByLabelText("Duração da atividade 2 em minutos"), { target: { value: "240" } });

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

  it("summarizes the accumulated cycle time for each subject", async () => {
    window.history.pushState({}, "", "/ciclos");
    const cycle = {
      id: "7a725fd0-2429-46a3-a786-f14ef87642a5",
      name: "Ciclo equilibrado",
      mode: "CUSTOM",
      status: "DRAFT",
      totalMinutes: 135,
      activatable: true,
      stages: [
        { id: "stage-portuguese-1", position: 1, subjectId: "subject-portuguese", subjectName: "Língua Portuguesa", targetMinutes: 30, longBlockWarning: false },
        { id: "stage-math", position: 2, subjectId: "subject-math", subjectName: "Matemática", targetMinutes: 60, longBlockWarning: false },
        { id: "stage-portuguese-2", position: 3, subjectId: "subject-portuguese", subjectName: "Língua Portuguesa", targetMinutes: 45, longBlockWarning: false }
      ],
      createdAt: "2026-07-16T12:00:00Z",
      updatedAt: "2026-07-16T12:00:00Z"
    };
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url === "/api/auth/bootstrap-status") return jsonResponse({ registrationRequired: false });
      if (url === "/api/auth/me") return jsonResponse({ id: "user", email: "pessoa@example.com", timeZone: "America/Sao_Paulo" });
      if (url === "/api/subjects?status=active") return jsonResponse([
        { id: "subject-portuguese", name: "Língua Portuguesa", archived: false },
        { id: "subject-math", name: "Matemática", archived: false }
      ]);
      if (url === "/api/study-cycles") return jsonResponse([cycle]);
      throw new Error(`Unexpected request: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    renderApp();
    fireEvent.click(await screen.findByRole("button", { name: "Editar Ciclo equilibrado" }));

    expect(await screen.findByRole("region", { name: "Total por matéria" })).toBeVisible();
    expect(screen.getByLabelText("Total de Língua Portuguesa: 1h 15min")).toBeVisible();
    expect(screen.getByText("2 aparições · 56% do ciclo")).toBeVisible();
    expect(screen.getByLabelText("Total de Matemática: 1h")).toBeVisible();

    fireEvent.change(screen.getByLabelText("Duração da atividade 1 em minutos"), { target: { value: "60" } });
    expect(screen.getByLabelText("Total de Língua Portuguesa: 1h 45min")).toBeVisible();
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
