import { useQuery } from "@tanstack/react-query";
import "./styles.css";

type ApplicationStatus = {
  status: "UP";
  database: "UP";
  schemaVersion: string;
  version: string;
};

async function fetchApplicationStatus(): Promise<ApplicationStatus> {
  const response = await fetch("/api/status", {
    headers: { Accept: "application/json" }
  });

  if (!response.ok) {
    throw new Error("Não foi possível consultar o servidor.");
  }

  return response.json() as Promise<ApplicationStatus>;
}

function BrandMark() {
  return (
    <span className="brand-mark" aria-hidden="true">
      <svg viewBox="0 0 24 24">
        <path d="M7 5.5h7a3 3 0 0 1 3 3V19H9a2 2 0 0 0-2 2V5.5Z" />
        <path d="M7 5.5A2.5 2.5 0 0 0 4.5 8v10.5A2.5 2.5 0 0 0 7 21" />
      </svg>
    </span>
  );
}

function ServerIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <rect x="4" y="4" width="16" height="6" rx="2" />
      <rect x="4" y="14" width="16" height="6" rx="2" />
      <path d="M8 7h.01M8 17h.01M12 7h5M12 17h5" />
    </svg>
  );
}

function AppShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="app-shell">
      <aside className="side-rail" aria-label="Navegação principal">
        <div className="brand">
          <BrandMark />
          <span className="brand-name">Estuda Livre</span>
        </div>
        <p className="prototype-label">Instalação local</p>

        <p className="rail-section-label">Espaço de estudo</p>
        <nav className="rail-nav">
          <span className="nav-item active" aria-current="page">
            <ServerIcon />
            Estado da instância
          </span>
        </nav>

        <div className="rail-spacer" />
        <div className="rail-note">
          <span className="note-kicker">Self-hosted</span>
          <p>Seus estudos e seus dados no mesmo lugar.</p>
        </div>
      </aside>

      <section className="workspace">
        <header className="topbar">
          <div className="mobile-brand">
            <BrandMark />
            <span className="brand-name">Estuda Livre</span>
          </div>
          <div className="instance-label">
            <span className="status-dot" aria-hidden="true" />
            Instância local
          </div>
          <span className="topbar-meta">Bootstrap da aplicação</span>
        </header>
        {children}
      </section>
    </div>
  );
}

export function App() {
  const status = useQuery({
    queryKey: ["application-status"],
    queryFn: fetchApplicationStatus
  });

  return (
    <AppShell>
      <main className="content">
        <div className="page-heading">
          <div>
            <span className="eyebrow">Primeiro caminho executável</span>
            <h1>Sua base de estudos começa saudável.</h1>
            <p>
              Esta tela confirma o caminho completo do navegador ao banco, antes de você
              cadastrar a primeira matéria.
            </p>
          </div>
          <div className="heading-bookmark" aria-hidden="true">01</div>
        </div>

        {status.isPending && (
          <section className="status-card loading-card" aria-busy="true" aria-live="polite">
            <div className="status-icon skeleton" />
            <div className="loading-copy">
              <span className="skeleton skeleton-title" />
              <span className="skeleton skeleton-line" />
            </div>
            <span className="sr-only">Verificando servidor</span>
          </section>
        )}

        {status.isSuccess && (
          <section className="status-card" aria-labelledby="server-status-title">
            <div className="status-card-head">
              <div className="status-icon"><ServerIcon /></div>
              <div>
                <span className="card-kicker">Aplicação pronta</span>
                <h2 id="server-status-title">Servidor saudável</h2>
              </div>
              <span className="health-pill">
                <span className="status-dot" aria-hidden="true" />
                Online
              </span>
            </div>

            <div className="health-grid">
              <article className="health-item">
                <span className="health-number">01</span>
                <div>
                  <strong>Spring Boot respondendo</strong>
                  <span>API publicada em /api</span>
                </div>
              </article>
              <article className="health-item">
                <span className="health-number">02</span>
                <div>
                  <strong>PostgreSQL conectado</strong>
                  <span>Readiness validada pelo banco</span>
                </div>
              </article>
              <article className="health-item">
                <span className="health-number">03</span>
                <div>
                  <strong>Migração {status.data.schemaVersion} aplicada</strong>
                  <span>Schema inicial verificado pelo Flyway</span>
                </div>
              </article>
            </div>

            <footer className="status-footer">
              <span>Estuda Livre</span>
              <span className="status-version">v{status.data.version}</span>
            </footer>
          </section>
        )}

        {status.isError && (
          <section className="status-card error-card" role="alert" aria-labelledby="server-error-title">
            <div className="status-icon error-icon"><ServerIcon /></div>
            <div className="error-copy">
              <span className="card-kicker">Conexão interrompida</span>
              <h2 id="server-error-title">Servidor indisponível</h2>
              <p>Não conseguimos confirmar a API e o banco de dados neste momento.</p>
            </div>
            <button className="retry-button" type="button" onClick={() => void status.refetch()}>
              Verificar novamente
            </button>
          </section>
        )}
      </main>
    </AppShell>
  );
}
