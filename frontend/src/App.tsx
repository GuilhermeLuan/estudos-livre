import { FormEvent, ReactNode, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BrowserRouter, Route, Routes, useLocation, useNavigate, useSearchParams } from "react-router-dom";
import {
  ApiError,
  AuthSnapshot,
  createInitialAccount,
  changePassword,
  registerAccount,
  resetPassword,
  loadAuthSnapshot,
  login,
  logout
} from "./auth-api";
import "./styles.css";

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

function LockIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <rect x="5" y="10" width="14" height="10" rx="2" />
      <path d="M8 10V7a4 4 0 0 1 8 0v3M12 14v2" />
    </svg>
  );
}

function AppShell({ children, authenticated }: { children: ReactNode; authenticated: boolean }) {
  return (
    <div className="app-shell">
      <aside className="side-rail" aria-label="Navegação principal">
        <div className="brand"><BrandMark /><span className="brand-name">Estuda Livre</span></div>
        <p className="prototype-label">Instalação local</p>
        <p className="rail-section-label">Espaço de estudo</p>
        <nav className="rail-nav">
          <span className="nav-item active" aria-current="page"><LockIcon />Acesso protegido</span>
        </nav>
        <div className="rail-spacer" />
        <div className="rail-note">
          <span className="note-kicker">Seus dados</span>
          <p>A sessão fica nesta instalação e é armazenada no seu PostgreSQL.</p>
        </div>
      </aside>

      <section className="workspace">
        <header className="topbar">
          <div className="mobile-brand"><BrandMark /><span className="brand-name">Estuda Livre</span></div>
          <div className="instance-label">
            <span className="status-dot" aria-hidden="true" />
            {authenticated ? "Sessão protegida" : "Instância local"}
          </div>
          <span className="topbar-meta">Identidade e acesso</span>
        </header>
        {children}
      </section>
    </div>
  );
}

function FieldError({ message }: { message?: string }) {
  return message ? <span className="field-error">{message}</span> : null;
}

function BootstrapForm({ onComplete }: { onComplete: () => void }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [timeZone, setTimeZone] = useState(() =>
    Intl.DateTimeFormat().resolvedOptions().timeZone || "America/Sao_Paulo"
  );
  const mutation = useMutation({
    mutationFn: createInitialAccount,
    onSuccess: onComplete
  });
  const error = mutation.error instanceof ApiError ? mutation.error : undefined;

  function submit(event: FormEvent) {
    event.preventDefault();
    mutation.mutate({ email, password, timeZone });
  }

  return (
    <AuthCard kicker="Configuração inicial" title="Crie a primeira conta">
      <p className="card-intro">Esta conta será a primeira identidade desta instalação.</p>
      <form className="auth-form" onSubmit={submit}>
        <label>E-mail<input type="email" autoComplete="email" required maxLength={320} value={email} onChange={(e) => setEmail(e.target.value)} /></label>
        <FieldError message={error?.fieldErrors.email} />
        <label>Senha<input type="password" autoComplete="new-password" required minLength={12} maxLength={128} value={password} onChange={(e) => setPassword(e.target.value)} /></label>
        <span className="field-hint">Use pelo menos 12 caracteres.</span>
        <FieldError message={error?.fieldErrors.password} />
        <label>Fuso horário<input type="text" required maxLength={255} value={timeZone} onChange={(e) => setTimeZone(e.target.value)} /></label>
        <span className="field-hint">Identificador IANA, como America/Sao_Paulo.</span>
        <FieldError message={error?.fieldErrors.timeZone} />
        {error && <p className="form-error" role="alert">{error.message}</p>}
        <button className="primary-button" type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? "Criando conta…" : "Criar conta"}
        </button>
      </form>
    </AuthCard>
  );
}

function LoginForm({ notice, onAuthenticated, registrationEnabled, onRegister }: {
  notice?: string;
  onAuthenticated: (snapshot: AuthSnapshot) => void;
  registrationEnabled: boolean;
  onRegister: () => void;
}) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const mutation = useMutation({
    mutationFn: () => login(email, password),
    onSuccess: (identity) => onAuthenticated({
      state: "authenticated",
      identity,
      registrationEnabled
    })
  });

  function submit(event: FormEvent) {
    event.preventDefault();
    mutation.mutate();
  }

  return (
    <AuthCard kicker="Bem-vindo de volta" title="Entre no seu espaço">
      <p className="card-intro">Use a conta cadastrada nesta instalação.</p>
      {notice && <p className="success-notice" role="status">{notice}</p>}
      <form className="auth-form" onSubmit={submit}>
        <label>E-mail<input type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} /></label>
        <label>Senha<input type="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} /></label>
        {mutation.isError && <p className="form-error" role="alert">E-mail ou senha inválidos.</p>}
        <button className="primary-button" type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? "Entrando…" : "Entrar"}
        </button>
        {registrationEnabled && (
          <button className="text-button" type="button" onClick={onRegister}>
            Criar uma conta
          </button>
        )}
      </form>
    </AuthCard>
  );
}

function RegistrationForm({ onComplete, onCancel }: { onComplete: () => void; onCancel: () => void }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [timeZone, setTimeZone] = useState(() =>
    Intl.DateTimeFormat().resolvedOptions().timeZone || "America/Sao_Paulo"
  );
  const mutation = useMutation({ mutationFn: registerAccount, onSuccess: onComplete });
  const error = mutation.error instanceof ApiError ? mutation.error : undefined;

  function submit(event: FormEvent) {
    event.preventDefault();
    mutation.mutate({ email, password, timeZone });
  }

  return (
    <AuthCard kicker="Cadastro habilitado" title="Crie sua conta">
      <p className="card-intro">Sua identidade ficará somente nesta instalação.</p>
      <form className="auth-form" onSubmit={submit}>
        <label>E-mail<input type="email" autoComplete="email" required maxLength={320} value={email} onChange={(event) => setEmail(event.target.value)} /></label>
        <FieldError message={error?.fieldErrors.email} />
        <label>Senha<input type="password" autoComplete="new-password" required minLength={12} maxLength={128} value={password} onChange={(event) => setPassword(event.target.value)} /></label>
        <span className="field-hint">Use pelo menos 12 caracteres.</span>
        <FieldError message={error?.fieldErrors.password} />
        <label>Fuso horário<input type="text" required maxLength={255} value={timeZone} onChange={(event) => setTimeZone(event.target.value)} /></label>
        <FieldError message={error?.fieldErrors.timeZone} />
        {error && <p className="form-error" role="alert">{error.message}</p>}
        <div className="form-actions">
          <button className="secondary-button" type="button" onClick={onCancel}>Voltar</button>
          <button className="primary-button" type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? "Criando conta…" : "Criar conta"}
          </button>
        </div>
      </form>
    </AuthCard>
  );
}

function AuthenticatedCard({ snapshot, onLogout }: {
  snapshot: Extract<AuthSnapshot, { state: "authenticated" }>;
  onLogout: () => void;
}) {
  const mutation = useMutation({ mutationFn: logout, onSuccess: onLogout });
  return (
    <AuthCard kicker="Sessão ativa" title="Seu espaço está protegido">
      <p className="card-intro">Você entrou com a identidade abaixo.</p>
      <dl className="identity-list">
        <div><dt>E-mail</dt><dd>{snapshot.identity.email}</dd></div>
        <div><dt>Fuso horário</dt><dd>{snapshot.identity.timeZone}</dd></div>
      </dl>
      <PasswordChangeForm />
      {mutation.isError && <p className="form-error" role="alert">Não foi possível encerrar a sessão.</p>}
      <button className="secondary-button" type="button" onClick={() => mutation.mutate()} disabled={mutation.isPending}>
        {mutation.isPending ? "Saindo…" : "Sair"}
      </button>
    </AuthCard>
  );
}

function PasswordChangeForm() {
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const mutation = useMutation({
    mutationFn: () => changePassword(currentPassword, newPassword),
    onSuccess: () => {
      setCurrentPassword("");
      setNewPassword("");
    }
  });
  const error = mutation.error instanceof ApiError ? mutation.error : undefined;

  function submit(event: FormEvent) {
    event.preventDefault();
    mutation.mutate();
  }

  return (
    <section className="credential-section" aria-labelledby="password-change-title">
      <div className="section-heading">
        <span className="card-kicker">Credencial</span>
        <h3 id="password-change-title">Atualize sua senha</h3>
        <p>As outras sessões serão encerradas; esta continuará ativa.</p>
      </div>
      <form className="auth-form" onSubmit={submit}>
        <label>Senha atual<input type="password" autoComplete="current-password" required value={currentPassword} onChange={(event) => setCurrentPassword(event.target.value)} /></label>
        <label>Nova senha<input type="password" autoComplete="new-password" required minLength={12} maxLength={128} value={newPassword} onChange={(event) => setNewPassword(event.target.value)} /></label>
        <span className="field-hint">Use pelo menos 12 caracteres.</span>
        {error && <p className="form-error" role="alert">{error.message}</p>}
        {mutation.isSuccess && <p className="success-notice" role="status">Senha atualizada. Esta sessão continua ativa.</p>}
        <button className="primary-button compact-button" type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? "Atualizando…" : "Atualizar senha"}
        </button>
      </form>
    </section>
  );
}

function AuthCard({ kicker, title, children }: { kicker: string; title: string; children: ReactNode }) {
  return (
    <section className="auth-card" aria-labelledby="auth-card-title">
      <div className="auth-card-head">
        <div className="status-icon"><LockIcon /></div>
        <div><span className="card-kicker">{kicker}</span><h2 id="auth-card-title">{title}</h2></div>
      </div>
      <div className="auth-card-body">{children}</div>
    </section>
  );
}

function CredentialHome() {
  const queryClient = useQueryClient();
  const location = useLocation();
  const locationNotice = (location.state as { notice?: string } | null)?.notice;
  const [notice, setNotice] = useState<string | undefined>(locationNotice);
  const [showRegistration, setShowRegistration] = useState(false);
  const auth = useQuery({ queryKey: ["auth-snapshot"], queryFn: loadAuthSnapshot });
  const snapshot = auth.data;

  function setSnapshot(next: AuthSnapshot) {
    queryClient.setQueryData(["auth-snapshot"], next);
  }

  return (
    <AppShell authenticated={snapshot?.state === "authenticated"}>
      <main className="content auth-content">
        <div className="page-heading">
          <div>
            <span className="eyebrow">Sua instalação, suas regras</span>
            <h1>Um lugar seguro para estudar no seu ritmo.</h1>
            <p>Crie sua identidade local ou entre para continuar de onde parou.</p>
          </div>
          <div className="heading-bookmark" aria-hidden="true">01</div>
        </div>

        {auth.isPending && (
          <section className="auth-card loading-card" aria-busy="true" aria-live="polite">
            <div className="status-icon skeleton" /><div className="loading-copy"><span className="skeleton skeleton-title" /><span className="skeleton skeleton-line" /></div>
            <span className="sr-only">Verificando acesso</span>
          </section>
        )}
        {auth.isError && (
          <section className="auth-card error-card" role="alert">
            <div className="status-icon error-icon"><LockIcon /></div>
            <div className="error-copy"><span className="card-kicker">Conexão interrompida</span><h2>Não foi possível verificar o acesso</h2><p>Confirme se a aplicação está disponível e tente novamente.</p></div>
            <button className="primary-button" type="button" onClick={() => void auth.refetch()}>Tentar novamente</button>
          </section>
        )}
        {snapshot?.state === "bootstrap" && <BootstrapForm onComplete={() => { setNotice("Conta criada. Agora, entre para continuar."); setSnapshot({ state: "login", registrationEnabled: snapshot.registrationEnabled }); }} />}
        {snapshot?.state === "login" && showRegistration && (
          <RegistrationForm
            onCancel={() => setShowRegistration(false)}
            onComplete={() => { setNotice("Conta criada. Agora, entre para continuar."); setShowRegistration(false); }}
          />
        )}
        {snapshot?.state === "login" && !showRegistration && (
          <LoginForm
            notice={notice}
            registrationEnabled={snapshot.registrationEnabled}
            onRegister={() => setShowRegistration(true)}
            onAuthenticated={setSnapshot}
          />
        )}
        {snapshot?.state === "authenticated" && <AuthenticatedCard snapshot={snapshot} onLogout={() => { setNotice("Sessão encerrada com segurança."); setSnapshot({ state: "login", registrationEnabled: snapshot.registrationEnabled }); }} />}
      </main>
    </AppShell>
  );
}

function PasswordResetPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get("token") ?? "";
  const [newPassword, setNewPassword] = useState("");
  const mutation = useMutation({
    mutationFn: () => resetPassword(token, newPassword),
    onSuccess: () => navigate("/", {
      replace: true,
      state: { notice: "Senha redefinida. Entre com sua nova senha." }
    })
  });
  const error = mutation.error instanceof ApiError ? mutation.error : undefined;

  function submit(event: FormEvent) {
    event.preventDefault();
    mutation.mutate();
  }

  return (
    <AppShell authenticated={false}>
      <main className="content auth-content reset-content">
        <div className="page-heading">
          <div>
            <span className="eyebrow">Recupere seu acesso</span>
            <h1>Uma nova chave para o seu espaço de estudo.</h1>
            <p>Este link é temporário e pode ser usado uma única vez.</p>
          </div>
          <div className="heading-bookmark recovery-bookmark" aria-hidden="true">02</div>
        </div>
        <AuthCard kicker="Link temporário" title="Defina uma nova senha">
          <p className="card-intro">Escolha uma senha com pelo menos 12 caracteres.</p>
          <form className="auth-form" onSubmit={submit}>
            <label>Nova senha<input type="password" autoComplete="new-password" required minLength={12} maxLength={128} value={newPassword} onChange={(event) => setNewPassword(event.target.value)} /></label>
            {!token && <p className="form-error" role="alert">Este link de redefinição é inválido ou expirou.</p>}
            {error && <p className="form-error" role="alert">{error.message}</p>}
            <button className="primary-button recovery-button" type="submit" disabled={mutation.isPending || !token}>
              {mutation.isPending ? "Redefinindo…" : "Redefinir senha"}
            </button>
          </form>
        </AuthCard>
      </main>
    </AppShell>
  );
}

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<CredentialHome />} />
        <Route path="/redefinir-senha" element={<PasswordResetPage />} />
      </Routes>
    </BrowserRouter>
  );
}
