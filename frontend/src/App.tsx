import { FormEvent, ReactNode, useEffect, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { BrowserRouter, Link, Navigate, Route, Routes, useLocation, useNavigate, useParams, useSearchParams } from "react-router-dom";
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
import { archiveSubject, createSubject, getSubject, listSubjects, restoreSubject, Subject, SubjectStatus, updateSubject } from "./subject-api";
import {
  archiveContent,
  ContentStatus,
  createContent,
  listContents,
  restoreContent,
  StudyContent,
  updateContent
} from "./content-api";
import {
  activateStudyCycle,
  createStudyCycle,
  createSuggestedStudyCycle,
  CycleSwitchAction,
  listStudyCycleRuns,
  listStudyCycles,
  regenerateStudyCycleSuggestion,
  StudyCycle,
  StudyCycleDifficulty,
  updateStudyCycle
} from "./study-cycle-api";
import {
  createManualStudySession,
  finishStudySession,
  listStudySessionHistory,
  loadExerciseSummary,
  loadCurrentStudySession,
  pauseStudySession,
  resumeStudySession,
  startStudySession,
  StudySession,
  StartStudySessionInput,
  ExerciseResultInput,
  ExerciseSummary,
  updateExerciseResult
} from "./study-session-api";
import { listReviews, ReviewOccurrence, ReviewTiming, startReview } from "./review-api";
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

function SubjectsIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M5 4.5h10a3 3 0 0 1 3 3V20H8a3 3 0 0 0-3 3V4.5Z" />
      <path d="M5 4.5A2.5 2.5 0 0 0 2.5 7v13A2.5 2.5 0 0 0 5 22M9 9h5M9 13h5" />
    </svg>
  );
}

function ContentsIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M6 3.5h12v17H6z" />
      <path d="M9 8h6M9 12h6M9 16h4" />
    </svg>
  );
}

function CyclesIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M6 5h12M6 12h12M6 19h12" />
      <circle cx="4" cy="5" r="1" />
      <circle cx="4" cy="12" r="1" />
      <circle cx="4" cy="19" r="1" />
    </svg>
  );
}

function ReviewsIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M6 3.5h12v17l-6-3-6 3v-17Z" />
      <path d="M9 8h6M9 12h4" />
    </svg>
  );
}

function AppShell({ children, authenticated, activeSection, topbarMeta = "Identidade e acesso" }: {
  children: ReactNode;
  authenticated: boolean;
  activeSection?: "subjects" | "cycles" | "reviews" | "account";
  topbarMeta?: string;
}) {
  return (
    <div className="app-shell">
      <aside className="side-rail" aria-label="Navegação principal">
        <div className="brand"><BrandMark /><span className="brand-name">Estuda Livre</span></div>
        <p className="prototype-label">Instalação local</p>
        <p className="rail-section-label">Espaço de estudo</p>
        <nav className="rail-nav">
          {authenticated ? (
            <>
              <Link className={`nav-item ${activeSection === "subjects" ? "active" : ""}`} aria-current={activeSection === "subjects" ? "page" : undefined} to="/materias"><SubjectsIcon />Matérias</Link>
              <Link className={`nav-item ${activeSection === "cycles" ? "active" : ""}`} aria-current={activeSection === "cycles" ? "page" : undefined} to="/ciclos"><CyclesIcon />Ciclos</Link>
              <Link className={`nav-item ${activeSection === "reviews" ? "active" : ""}`} aria-current={activeSection === "reviews" ? "page" : undefined} to="/revisoes"><ReviewsIcon />Revisões</Link>
              <Link className={`nav-item ${activeSection === "account" ? "active" : ""}`} aria-current={activeSection === "account" ? "page" : undefined} to="/conta"><LockIcon />Conta</Link>
            </>
          ) : (
            <span className="nav-item active" aria-current="page"><LockIcon />Acesso protegido</span>
          )}
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
          <span className="topbar-meta">{topbarMeta}</span>
        </header>
        {authenticated && (
          <nav className="mobile-nav" aria-label="Navegação móvel">
            <Link className={activeSection === "subjects" ? "active" : ""} aria-current={activeSection === "subjects" ? "page" : undefined} to="/materias"><SubjectsIcon />Matérias</Link>
            <Link className={activeSection === "cycles" ? "active" : ""} aria-current={activeSection === "cycles" ? "page" : undefined} to="/ciclos"><CyclesIcon />Ciclos</Link>
            <Link className={activeSection === "reviews" ? "active" : ""} aria-current={activeSection === "reviews" ? "page" : undefined} to="/revisoes"><ReviewsIcon />Revisões</Link>
            <Link className={activeSection === "account" ? "active" : ""} aria-current={activeSection === "account" ? "page" : undefined} to="/conta"><LockIcon />Conta</Link>
          </nav>
        )}
        {children}
      </section>
    </div>
  );
}

function formatCycleMinutes(totalMinutes: number) {
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;
  if (hours === 0) return `${minutes}min`;
  return minutes === 0 ? `${hours}h` : `${hours}h ${minutes}min`;
}

function formatRunDate(value: string) {
  return new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "short", year: "numeric" })
    .format(new Date(value));
}

function CycleSuggestionExplanation({ cycle }: { cycle: StudyCycle }) {
  if (!cycle.suggestion) return null;
  return (
    <details className="cycle-explanation">
      <summary>Entenda o cálculo</summary>
      <div className="cycle-explanation-body">
        <div className="cycle-formula-strip">
          <span><strong>Duração</strong>{cycle.suggestion.durationRule}</span>
          <span><strong>Prioridade</strong>{cycle.suggestion.priorityRule}</span>
        </div>
        <ul aria-label="Distribuição sugerida por matéria">
          {cycle.suggestion.subjects.map((subject) => (
            <li key={subject.subjectId}>
              <div>
                <strong>{subject.subjectName}</strong>
                <span>{`Prioridade ${subject.priority}`}</span>
                <small>{`${subject.questionCount} questões · peso ${subject.weight} · ${subject.appearanceCount} ${subject.appearanceCount === 1 ? "aparição" : "aparições"}`}</small>
              </div>
              <strong aria-label={`Carga de ${subject.subjectName}: ${formatCycleMinutes(subject.allocatedMinutes)}`}>
                {formatCycleMinutes(subject.allocatedMinutes)}
              </strong>
            </li>
          ))}
        </ul>
      </div>
    </details>
  );
}

type EditableCycleStage = {
  key: string;
  subjectId: string;
  targetMinutes: number;
};

type SuggestedSubjectDraft = {
  key: string;
  subjectId: string;
  questionCount: number;
  weight: number;
  difficulty: StudyCycleDifficulty;
};

function CycleSwitchDialog({ currentCycle, targetCycle, pending, error, onCancel, onChoose }: {
  currentCycle: StudyCycle;
  targetCycle: StudyCycle;
  pending: boolean;
  error?: string;
  onCancel: () => void;
  onChoose: (action: CycleSwitchAction) => void;
}) {
  const dialogRef = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (typeof dialog.showModal === "function") dialog.showModal();
    else dialog.setAttribute("open", "");
    return () => {
      if (dialog.open && typeof dialog.close === "function") dialog.close();
    };
  }, []);

  return (
    <dialog className="cycle-switch-dialog" ref={dialogRef} aria-labelledby="cycle-switch-title" onCancel={(event) => { event.preventDefault(); onCancel(); }}>
      <div className="cycle-switch-panel">
        <header>
          <span className="cycle-switch-mark" aria-hidden="true"><CyclesIcon /></span>
          <div><span className="card-kicker">Troca livre</span><h2 id="cycle-switch-title">Como deseja trocar de ciclo?</h2></div>
        </header>
        <p className="cycle-switch-summary">Você vai sair de <strong>{currentCycle.name}</strong> e ativar <strong>{targetCycle.name}</strong>. Não há percentual mínimo para fazer essa troca.</p>
        <div className="cycle-switch-options">
          <section>
            <strong>Pausar a volta</strong>
            <p>Guarda o progresso atual para você continuar quando voltar.</p>
          </section>
          <section className="cycle-switch-abandon-option">
            <strong>Encerrar esta volta</strong>
            <p>Preserva o histórico, mas a próxima ativação começa uma volta nova.</p>
          </section>
        </div>
        {error && <p className="form-error" role="alert">{error}</p>}
        <footer>
          <button className="secondary-button" type="button" onClick={onCancel} disabled={pending}>Cancelar</button>
          <button className="danger-button" type="button" onClick={() => onChoose("ABANDON")} disabled={pending}>Encerrar volta e trocar</button>
          <button className="primary-button" type="button" autoFocus onClick={() => onChoose("PAUSE")} disabled={pending}>{pending ? "Trocando…" : "Pausar e trocar"}</button>
        </footer>
      </div>
    </dialog>
  );
}

function RegenerationDialog({ cycle, subjects, pending, error, onCancel, onConfirm }: {
  cycle: StudyCycle;
  subjects: Subject[];
  pending: boolean;
  error?: string;
  onCancel: () => void;
  onConfirm: (name: string, subjects: SuggestedSubjectDraft[]) => void;
}) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const [name, setName] = useState(cycle.name);
  const [reviewing, setReviewing] = useState(false);
  const [drafts, setDrafts] = useState<SuggestedSubjectDraft[]>(() => {
    const suggestionBySubject = new Map(cycle.suggestion?.subjects.map((subject) => [subject.subjectId, subject]));
    const subjectIds = cycle.suggestion?.subjects.map((subject) => subject.subjectId)
      ?? Array.from(new Set(cycle.stages.map((stage) => stage.subjectId)));
    return subjectIds.map((subjectId, index) => {
      const previous = suggestionBySubject.get(subjectId);
      return {
        key: `regeneration-${subjectId}-${index}`,
        subjectId,
        questionCount: previous?.questionCount ?? 1,
        weight: previous?.weight ?? 1,
        difficulty: previous?.difficulty ?? "MEDIUM"
      };
    });
  });
  const estimatedMinutes = Math.min(30 * 60, Math.max(10 * 60, drafts.length * 2 * 60));
  const valid = Boolean(name.trim() && drafts.length > 0 && drafts.every((draft) => draft.questionCount > 0 && draft.weight > 0));

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (typeof dialog.showModal === "function") dialog.showModal();
    else dialog.setAttribute("open", "");
    return () => {
      if (dialog.open && typeof dialog.close === "function") dialog.close();
    };
  }, []);

  function updateDraft(index: number, patch: Partial<Omit<SuggestedSubjectDraft, "key">>) {
    setDrafts((current) => current.map((draft, draftIndex) => draftIndex === index ? { ...draft, ...patch } : draft));
  }

  function addSubject() {
    const used = new Set(drafts.map((draft) => draft.subjectId));
    const available = subjects.find((subject) => !used.has(subject.id));
    if (!available) return;
    setDrafts((current) => [...current, {
      key: `regeneration-${available.id}-${current.length}`,
      subjectId: available.id,
      questionCount: 1,
      weight: 1,
      difficulty: "MEDIUM"
    }]);
  }

  return (
    <dialog className="regeneration-dialog" ref={dialogRef} aria-labelledby="regeneration-title" onCancel={(event) => { event.preventDefault(); if (!pending) onCancel(); }}>
      <div className="regeneration-panel">
        <header className="regeneration-dialog-head">
          <span className="regeneration-bookmark" aria-hidden="true"><CyclesIcon /></span>
          <div>
            <span className="card-kicker">Substituição consciente</span>
            <h2 id="regeneration-title">{reviewing ? "Confirmar nova sugestão" : "Preparar nova sugestão"}</h2>
            <p>As atividades atuais só serão substituídas depois da sua confirmação.</p>
          </div>
        </header>
        <div className="regeneration-impact" aria-label="Impacto da regeneração">
          <article aria-label={`Planejamento atual: ${formatCycleMinutes(cycle.totalMinutes)}`}>
            <span>Planejamento atual</span>
            <strong>{formatCycleMinutes(cycle.totalMinutes)}</strong>
            <small>{cycle.stages.length} {cycle.stages.length === 1 ? "atividade" : "atividades"}</small>
          </article>
          <span className="regeneration-arrow" aria-hidden="true">→</span>
          <article className="regeneration-new" aria-label={`Nova sugestão estimada: ${formatCycleMinutes(estimatedMinutes)}`}>
            <span>Nova sugestão estimada</span>
            <strong>{formatCycleMinutes(estimatedMinutes)}</strong>
            <small>{drafts.length} {drafts.length === 1 ? "matéria" : "matérias"}</small>
          </article>
        </div>
        {!reviewing ? (
          <form className="regeneration-form" onSubmit={(event) => { event.preventDefault(); setReviewing(true); }}>
            <label className="cycle-name-field">Nome da nova sugestão<input required maxLength={120} value={name} onChange={(event) => setName(event.target.value)} /></label>
            <div className="regeneration-inputs">
              {drafts.map((draft, index) => {
                const selected = subjects.find((subject) => subject.id === draft.subjectId);
                const subjectName = selected?.name ?? cycle.stages.find((stage) => stage.subjectId === draft.subjectId)?.subjectName ?? `Matéria ${index + 1}`;
                return (
                  <fieldset className="regeneration-subject" key={draft.key}>
                    <legend>{String(index + 1).padStart(2, "0")} · {subjectName}</legend>
                    <label>Matéria
                      <select value={draft.subjectId} onChange={(event) => updateDraft(index, { subjectId: event.target.value })}>
                        {subjects.map((subject) => <option key={subject.id} value={subject.id} disabled={drafts.some((item, itemIndex) => itemIndex !== index && item.subjectId === subject.id)}>{subject.name}</option>)}
                      </select>
                    </label>
                    <label>Questões de {subjectName}<input required type="number" min={1} value={draft.questionCount} onChange={(event) => updateDraft(index, { questionCount: Number(event.target.value) })} /></label>
                    <label>Peso de {subjectName}<input required type="number" min={1} value={draft.weight} onChange={(event) => updateDraft(index, { weight: Number(event.target.value) })} /></label>
                    <label>Dificuldade de {subjectName}
                      <select value={draft.difficulty} onChange={(event) => updateDraft(index, { difficulty: event.target.value as StudyCycleDifficulty })}>
                        <option value="EASY">Fácil · fator 1,00</option>
                        <option value="MEDIUM">Média · fator 1,25</option>
                        <option value="HARD">Difícil · fator 1,50</option>
                      </select>
                    </label>
                    <button className="suggestion-remove-subject" type="button" aria-label={`Remover ${subjectName} da nova sugestão`} onClick={() => setDrafts((current) => current.filter((_, draftIndex) => draftIndex !== index))}>Remover</button>
                  </fieldset>
                );
              })}
            </div>
            {drafts.length < subjects.length && <button className="cycle-add-stage" type="button" onClick={addSubject}>+ Adicionar matéria</button>}
            <footer className="regeneration-actions">
              <button className="secondary-button" type="button" onClick={onCancel}>Cancelar</button>
              <button className="primary-button" type="submit" disabled={!valid}>Revisar substituição</button>
            </footer>
          </form>
        ) : (
          <div className="regeneration-confirmation">
            <p>Seu planejamento personalizado de {formatCycleMinutes(cycle.totalMinutes)} será substituído por uma sugestão estimada de {formatCycleMinutes(estimatedMinutes)}.</p>
            <p className="regeneration-confirmation-note">Depois da confirmação, as atividades serão recalculadas e o ciclo voltará ao modo sugerido.</p>
            {error && <p className="form-error" role="alert">{error}</p>}
            <footer className="regeneration-actions">
              <button className="secondary-button" type="button" onClick={() => setReviewing(false)} disabled={pending}>Voltar</button>
              <button className="danger-button" type="button" autoFocus onClick={() => onConfirm(name, drafts)} disabled={pending}>{pending ? "Regenerando…" : "Confirmar regeneração"}</button>
            </footer>
          </div>
        )}
      </div>
    </dialog>
  );
}

function formatTimer(totalSeconds: number) {
  const safeSeconds = Math.max(0, Math.floor(totalSeconds));
  const hours = Math.floor(safeSeconds / 3600);
  const minutes = Math.floor((safeSeconds % 3600) / 60);
  const seconds = safeSeconds % 60;
  return [hours, minutes, seconds].map((value) => String(value).padStart(2, "0")).join(":");
}

function parseTimer(value: string) {
  const match = /^(\d+):([0-5]\d):([0-5]\d)$/.exec(value.trim());
  if (!match) return null;
  return Number(match[1]) * 3600 + Number(match[2]) * 60 + Number(match[3]);
}

function formatAccuracy(value: number) {
  return value.toLocaleString("pt-BR", { minimumFractionDigits: 1, maximumFractionDigits: 1 });
}

function useLiveSessionSeconds(session: StudySession | null | undefined) {
  const [seconds, setSeconds] = useState(session?.measuredSeconds ?? 0);

  useEffect(() => {
    if (!session) {
      setSeconds(0);
      return;
    }
    setSeconds(session.measuredSeconds);
    if (session.status !== "ACTIVE") return;
    const receivedAt = performance.now();
    const timer = window.setInterval(() => {
      setSeconds(session.measuredSeconds + Math.floor((performance.now() - receivedAt) / 1000));
    }, 250);
    return () => window.clearInterval(timer);
  }, [session]);

  return seconds;
}

function StudySessionDesk({ session, seconds, pending, error, onPause, onResume, onFinish }: {
  session: StudySession;
  seconds: number;
  pending: boolean;
  error?: string;
  onPause: () => void;
  onResume: () => void;
  onFinish: () => void;
}) {
  return (
    <section
      className={`study-session-desk ${session.status === "PAUSED" ? "is-paused" : ""}`}
      aria-label={session.status === "ACTIVE" ? "Cronômetro em andamento" : "Cronômetro pausado"}
    >
      <span className="study-session-mark" aria-hidden="true">{session.status === "ACTIVE" ? "▶" : "Ⅱ"}</span>
      <div className="study-session-copy">
        <span className="card-kicker">{session.origin === "CYCLE" ? `Etapa ${String(session.cycle?.stagePosition ?? 1).padStart(2, "0")}` : session.origin === "REVIEW" ? "Revisão" : "Sessão livre"}</span>
        <h2>{session.subject.name}</h2>
        <p>{session.content?.name ?? "Sem conteúdo específico"}</p>
      </div>
      <div className="study-session-time">
        <span>{session.status === "ACTIVE" ? "Tempo líquido" : "Pausado"}</span>
        <strong>{formatTimer(seconds)}</strong>
      </div>
      <div className="study-session-actions">
        <button
          className={session.status === "ACTIVE" ? "secondary-button" : "primary-button"}
          type="button"
          onClick={session.status === "ACTIVE" ? onPause : onResume}
          disabled={pending}
        >
          {pending ? "Sincronizando…" : session.status === "ACTIVE" ? "Pausar" : "Retomar"}
        </button>
        <button className="secondary-button" type="button" onClick={onFinish} disabled={pending}>Finalizar</button>
      </div>
      {error && <p className="form-error" role="alert">{error}</p>}
    </section>
  );
}

function FinishStudySessionDialog({ session, measuredSeconds, effectiveDuration, pending, error, onDurationChange, onCancel, onSubmit }: {
  session: StudySession;
  measuredSeconds: number;
  effectiveDuration: string;
  pending: boolean;
  error?: string;
  onDurationChange: (duration: string) => void;
  onCancel: () => void;
  onSubmit: (
    effectiveSeconds: number,
    exerciseResult: ExerciseResultInput | undefined,
    scheduleReviews: boolean
  ) => void;
}) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const [questionsAttemptedInput, setQuestionsAttemptedInput] = useState("");
  const [questionsCorrectInput, setQuestionsCorrectInput] = useState("");
  const [scheduleReviews, setScheduleReviews] = useState(true);
  const effectiveSeconds = parseTimer(effectiveDuration);
  const questionsAttempted = questionsAttemptedInput === "" ? null : Number(questionsAttemptedInput);
  const questionsCorrect = questionsCorrectInput === "" ? null : Number(questionsCorrectInput);
  const hasExerciseInput = questionsAttemptedInput !== "" || questionsCorrectInput !== "";
  const emptyExerciseResult = !hasExerciseInput
    || (questionsAttempted === 0 && (questionsCorrect === null || questionsCorrect === 0));
  const validExerciseResult = emptyExerciseResult || (
    Number.isInteger(questionsAttempted)
    && Number.isInteger(questionsCorrect)
    && questionsAttempted !== null
    && questionsCorrect !== null
    && questionsAttempted > 0
    && questionsCorrect >= 0
    && questionsCorrect <= questionsAttempted
  );
  const exerciseResult = !emptyExerciseResult && validExerciseResult
    ? { questionsAttempted: questionsAttempted!, questionsCorrect: questionsCorrect! }
    : undefined;
  const accuracyPercentage = exerciseResult
    ? (exerciseResult.questionsCorrect / exerciseResult.questionsAttempted) * 100
    : null;

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (typeof dialog.showModal === "function") dialog.showModal();
    else dialog.setAttribute("open", "");
    return () => {
      if (dialog.open && typeof dialog.close === "function") dialog.close();
    };
  }, []);

  return (
    <dialog className="study-session-dialog finish-session-dialog" ref={dialogRef} aria-labelledby="finish-session-title" onCancel={(event) => { event.preventDefault(); if (!pending) onCancel(); }}>
      <form className="study-session-dialog-panel" onSubmit={(event) => { event.preventDefault(); if (effectiveSeconds !== null && validExerciseResult) onSubmit(effectiveSeconds, exerciseResult, session.origin !== "REVIEW" && Boolean(session.content) && scheduleReviews); }}>
        <header>
          <span className="study-session-dialog-mark" aria-hidden="true">✓</span>
          <div>
            <span className="card-kicker">Fechamento do bloco</span>
            <h2 id="finish-session-title">Finalizar sessão</h2>
            <p>Confirme apenas o tempo que realmente representou estudo focado.</p>
          </div>
        </header>
        <div className="finish-session-body">
          <div className="finish-session-measured">
            <span>Tempo medido</span>
            <strong>{formatTimer(measuredSeconds)}</strong>
            <small>Pausas já foram descontadas pelo cronômetro.</small>
          </div>
          <label htmlFor="effective-duration">Duração efetiva
            <input
              id="effective-duration"
              aria-label="Duração efetiva"
              autoFocus
              inputMode="numeric"
              pattern="[0-9]+:[0-5][0-9]:[0-5][0-9]"
              aria-describedby="effective-duration-hint"
              value={effectiveDuration}
              onChange={(event) => onDurationChange(event.target.value)}
              disabled={pending}
              required
            />
            <small id="effective-duration-hint">Use horas:minutos:segundos, por exemplo 00:45:00.</small>
          </label>
          <fieldset className="finish-session-exercises">
            <legend>Exercícios <span>(opcional)</span></legend>
            <p>Registre o desempenho deste bloco; você poderá corrigi-lo depois.</p>
            <div>
              <label htmlFor="questions-attempted">Questões realizadas
                <input id="questions-attempted" type="number" min="0" step="1" inputMode="numeric" value={questionsAttemptedInput} onChange={(event) => setQuestionsAttemptedInput(event.target.value)} disabled={pending} />
              </label>
              <label htmlFor="questions-correct">Questões corretas
                <input id="questions-correct" type="number" min="0" step="1" inputMode="numeric" value={questionsCorrectInput} onChange={(event) => setQuestionsCorrectInput(event.target.value)} disabled={pending} />
              </label>
            </div>
            {accuracyPercentage !== null && (
              <output>{exerciseResult!.questionsCorrect} de {exerciseResult!.questionsAttempted} · {accuracyPercentage.toLocaleString("pt-BR", { minimumFractionDigits: 1, maximumFractionDigits: 1 })}% de acerto</output>
            )}
          </fieldset>
          {session.content && session.origin !== "REVIEW" && (
            <label className="finish-session-review-option">
              <input
                type="checkbox"
                aria-label="Agendar revisões deste conteúdo"
                aria-describedby="schedule-reviews-hint"
                checked={scheduleReviews}
                onChange={(event) => setScheduleReviews(event.target.checked)}
                disabled={pending}
              />
              <span>
                <strong>Agendar revisões deste conteúdo</strong>
                <small id="schedule-reviews-hint">Cria lembretes em 1, 7, 30, 60, 90 e 120 dias, ancorados neste estudo.</small>
              </span>
            </label>
          )}
          <div className="finish-session-credit">
            <span className="finish-session-credit-mark" aria-hidden="true">{session.origin === "REVIEW" ? "R" : String(session.cycle?.stagePosition ?? 1).padStart(2, "0")}</span>
            <div>
              <span>{session.origin === "REVIEW" ? "Crédito ao ciclo ativo" : session.cycle ? "Crédito previsto" : "Registro da sessão"}</span>
              <strong>{session.origin === "REVIEW" ? `Revisão · ${session.subject.name}` : session.cycle ? `Etapa ${String(session.cycle.stagePosition).padStart(2, "0")} · ${session.subject.name}` : `Sessão livre · ${session.subject.name}`}</strong>
              <small>{session.origin === "REVIEW" ? "O tempo será distribuído nas etapas desta matéria do ciclo ativo, se houver." : session.cycle ? "O tempo será aplicado até o restante da etapa desta matéria." : "O tempo ficará nas métricas, sem alterar uma etapa do ciclo."}</small>
            </div>
          </div>
        </div>
        {effectiveSeconds === null && <p className="form-error" role="alert">Informe a duração no formato horas:minutos:segundos.</p>}
        {!validExerciseResult && <p className="form-error" role="alert">Informe números inteiros e não deixe os acertos superarem as questões realizadas.</p>}
        {error && <p className="form-error" role="alert">{error}</p>}
        <footer>
          <button className="secondary-button" type="button" onClick={onCancel} disabled={pending}>Continuar estudando</button>
          <button className="primary-button" type="submit" disabled={pending || effectiveSeconds === null || !validExerciseResult}>{pending ? "Finalizando…" : "Finalizar sessão"}</button>
        </footer>
      </form>
    </dialog>
  );
}

function ExerciseResultDialog({ session, pending, error, onCancel, onSubmit }: {
  session: StudySession;
  pending: boolean;
  error?: string;
  onCancel: () => void;
  onSubmit: (exerciseResult: ExerciseResultInput) => void;
}) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const [questionsAttemptedInput, setQuestionsAttemptedInput] = useState(
    String(session.exerciseResult?.questionsAttempted ?? "")
  );
  const [questionsCorrectInput, setQuestionsCorrectInput] = useState(
    String(session.exerciseResult?.questionsCorrect ?? "")
  );
  const questionsAttempted = questionsAttemptedInput === "" ? null : Number(questionsAttemptedInput);
  const questionsCorrect = questionsCorrectInput === "" ? null : Number(questionsCorrectInput);
  const valid = Number.isInteger(questionsAttempted)
    && Number.isInteger(questionsCorrect)
    && questionsAttempted !== null
    && questionsCorrect !== null
    && questionsAttempted >= 0
    && questionsCorrect >= 0
    && questionsCorrect <= questionsAttempted
    && (questionsAttempted > 0 || questionsCorrect === 0);
  const accuracy = valid && questionsAttempted! > 0
    ? questionsCorrect! / questionsAttempted! * 100
    : null;

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (typeof dialog.showModal === "function") dialog.showModal();
    else dialog.setAttribute("open", "");
    return () => {
      if (dialog.open && typeof dialog.close === "function") dialog.close();
    };
  }, []);

  return (
    <dialog className="study-session-dialog exercise-result-dialog" ref={dialogRef} aria-labelledby="exercise-result-title" onCancel={(event) => { event.preventDefault(); if (!pending) onCancel(); }}>
      <form className="study-session-dialog-panel" onSubmit={(event) => {
        event.preventDefault();
        if (valid) onSubmit({ questionsAttempted: questionsAttempted!, questionsCorrect: questionsCorrect! });
      }}>
        <header>
          <span className="study-session-dialog-mark" aria-hidden="true">%</span>
          <div>
            <span className="card-kicker">Correção da ficha</span>
            <h2 id="exercise-result-title">Editar exercícios</h2>
            <p>{session.subject.name} · {session.content?.name ?? "Sem conteúdo específico"}</p>
          </div>
        </header>
        <div className="exercise-result-fields">
          <label htmlFor="edit-questions-attempted">Questões realizadas
            <input id="edit-questions-attempted" type="number" min="0" step="1" inputMode="numeric" autoFocus value={questionsAttemptedInput} onChange={(event) => setQuestionsAttemptedInput(event.target.value)} disabled={pending} />
          </label>
          <label htmlFor="edit-questions-correct">Questões corretas
            <input id="edit-questions-correct" type="number" min="0" step="1" inputMode="numeric" value={questionsCorrectInput} onChange={(event) => setQuestionsCorrectInput(event.target.value)} disabled={pending} />
          </label>
          {accuracy !== null && <output>{questionsCorrect} de {questionsAttempted} · {formatAccuracy(accuracy)}% de acerto</output>}
          {questionsAttempted === 0 && questionsCorrect === 0 && <p>Salvar com zero remove o resultado desta sessão.</p>}
        </div>
        {!valid && <p className="form-error" role="alert">Informe números inteiros e não deixe os acertos superarem as questões realizadas.</p>}
        {error && <p className="form-error" role="alert">{error}</p>}
        <footer>
          <button className="secondary-button" type="button" onClick={onCancel} disabled={pending}>Cancelar</button>
          <button className="primary-button" type="submit" disabled={pending || !valid}>{pending ? "Salvando…" : "Salvar exercícios"}</button>
        </footer>
      </form>
    </dialog>
  );
}

function ManualStudySessionDialog({
  subjects,
  contents,
  selectedSubjectId,
  selectedContentId,
  startedAtLocal,
  effectiveDuration,
  notes,
  loadingSubjects,
  loadingContents,
  pending,
  error,
  onSubjectChange,
  onContentChange,
  onStartedAtChange,
  onDurationChange,
  onNotesChange,
  onCancel,
  onSubmit
}: {
  subjects: Subject[];
  contents: StudyContent[];
  selectedSubjectId: string;
  selectedContentId: string;
  startedAtLocal: string;
  effectiveDuration: string;
  notes: string;
  loadingSubjects: boolean;
  loadingContents: boolean;
  pending: boolean;
  error?: string;
  onSubjectChange: (subjectId: string) => void;
  onContentChange: (contentId: string) => void;
  onStartedAtChange: (value: string) => void;
  onDurationChange: (value: string) => void;
  onNotesChange: (value: string) => void;
  onCancel: () => void;
  onSubmit: (effectiveSeconds: number) => void;
}) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const effectiveSeconds = parseTimer(effectiveDuration);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (typeof dialog.showModal === "function") dialog.showModal();
    else dialog.setAttribute("open", "");
    return () => {
      if (dialog.open && typeof dialog.close === "function") dialog.close();
    };
  }, []);

  return (
    <dialog className="study-session-dialog manual-session-dialog" ref={dialogRef} aria-labelledby="manual-session-title" onCancel={(event) => { event.preventDefault(); if (!pending) onCancel(); }}>
      <form className="study-session-dialog-panel" onSubmit={(event) => { event.preventDefault(); if (effectiveSeconds && selectedSubjectId) onSubmit(effectiveSeconds); }}>
        <header>
          <span className="study-session-dialog-mark manual-session-mark" aria-hidden="true">＋</span>
          <div>
            <span className="card-kicker">Ficha de estudo</span>
            <h2 id="manual-session-title">Registrar estudo concluído</h2>
            <p>Inclua um bloco feito sem cronômetro. O horário será interpretado no seu fuso configurado.</p>
          </div>
        </header>
        <div className="manual-session-fields">
          <div className="manual-session-timing">
            <label htmlFor="manual-started-at">Data e hora
              <input id="manual-started-at" type="datetime-local" value={startedAtLocal} onChange={(event) => onStartedAtChange(event.target.value)} disabled={pending} required />
            </label>
            <label htmlFor="manual-duration">Duração efetiva
              <input id="manual-duration" aria-label="Duração efetiva" inputMode="numeric" pattern="[0-9]+:[0-5][0-9]:[0-5][0-9]" aria-describedby="manual-duration-hint" value={effectiveDuration} onChange={(event) => onDurationChange(event.target.value)} disabled={pending} placeholder="00:45:00" required />
              <small id="manual-duration-hint">Use horas:minutos:segundos.</small>
            </label>
          </div>
          <div className="manual-session-context">
            <label htmlFor="manual-subject">Matéria
              <select id="manual-subject" value={selectedSubjectId} onChange={(event) => onSubjectChange(event.target.value)} disabled={loadingSubjects || pending} required>
                {loadingSubjects && <option value="">Carregando matérias…</option>}
                {!loadingSubjects && subjects.length === 0 && <option value="">Nenhuma matéria ativa</option>}
                {subjects.map((subject) => <option value={subject.id} key={subject.id}>{subject.name}</option>)}
              </select>
            </label>
            <label htmlFor="manual-content">Conteúdo (opcional)
              <select id="manual-content" value={selectedContentId} onChange={(event) => onContentChange(event.target.value)} disabled={!selectedSubjectId || loadingContents || pending}>
                <option value="">Sem conteúdo específico</option>
                {contents.map((content) => <option value={content.id} key={content.id}>{content.name}</option>)}
              </select>
            </label>
          </div>
          <label htmlFor="manual-notes">Anotações (opcional)
            <textarea id="manual-notes" maxLength={4000} rows={4} value={notes} onChange={(event) => onNotesChange(event.target.value)} disabled={pending} placeholder="O que foi estudado, dúvidas ou próximos passos" />
          </label>
          <div className="manual-session-credit-note">
            <span aria-hidden="true">✓</span>
            <p><strong>Um registro, dois efeitos.</strong> O tempo entra no histórico e avança as etapas compatíveis da volta ativa.</p>
          </div>
        </div>
        {effectiveDuration && !effectiveSeconds && <p className="form-error" role="alert">Informe uma duração maior que zero no formato horas:minutos:segundos.</p>}
        {error && <p className="form-error" role="alert">{error}</p>}
        <footer>
          <button className="secondary-button" type="button" onClick={onCancel} disabled={pending}>Cancelar</button>
          <button className="primary-button" type="submit" disabled={pending || !selectedSubjectId || !startedAtLocal || !effectiveSeconds}>{pending ? "Salvando…" : "Salvar registro"}</button>
        </footer>
      </form>
    </dialog>
  );
}

function StudySessionHistory({ sessions, pending, error, onEditExercises }: {
  sessions: StudySession[];
  pending: boolean;
  error: boolean;
  onEditExercises: (session: StudySession) => void;
}) {
  return (
    <section className="study-history" aria-label="Histórico recente">
      <header>
        <div><span className="card-kicker">Caderno de registros</span><h2>Histórico recente</h2></div>
        <span>{sessions.length} {sessions.length === 1 ? "sessão" : "sessões"}</span>
      </header>
      {pending && <p className="study-history-state">Abrindo seus registros…</p>}
      {error && <p className="form-error" role="alert">Não foi possível carregar o histórico.</p>}
      {!pending && !error && sessions.length === 0 && <p className="study-history-state">Os estudos concluídos aparecerão aqui.</p>}
      {sessions.length > 0 && (
        <ol>
          {sessions.map((session) => (
            <li key={session.id}>
              <span className="study-history-duration">{formatCycleMinutes(Math.round((session.effectiveSeconds ?? 0) / 60))}</span>
              <div className="study-history-copy">
                <strong>{session.subject.name}</strong>
                <small>{session.content?.name ?? "Sem conteúdo específico"}</small>
                {session.exerciseResult && <span className="study-history-exercises">{session.exerciseResult.questionsCorrect} de {session.exerciseResult.questionsAttempted} questões · {formatAccuracy(session.exerciseResult.accuracyPercentage)}%</span>}
              </div>
              <div className="study-history-meta">
                <time dateTime={session.startedAt}>{new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit" }).format(new Date(session.startedAt))}</time>
                <button className="text-button" type="button" aria-label={`Editar exercícios de ${session.subject.name}`} onClick={() => onEditExercises(session)}>Editar exercícios</button>
              </div>
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}

function ExerciseSummaryPanel({ summary, pending, error }: {
  summary?: ExerciseSummary;
  pending: boolean;
  error: boolean;
}) {
  const empty = !summary || (summary.subjects.length === 0 && summary.contents.length === 0);
  return (
    <section className="exercise-summary" aria-label="Resumo de exercícios">
      <header>
        <div><span className="card-kicker">Desempenho acumulado</span><h2>Resumo de exercícios</h2></div>
        <span>acertos sobre realizadas</span>
      </header>
      {pending && <p className="study-history-state">Somando seus resultados…</p>}
      {error && <p className="form-error" role="alert">Não foi possível carregar o resumo de exercícios.</p>}
      {!pending && !error && empty && <p className="study-history-state">Finalize uma sessão com questões para começar o resumo.</p>}
      {!pending && !error && summary && !empty && (
        <div className="exercise-summary-grid">
          {summary.subjects.map((subject) => (
            <article key={subject.subjectId}>
              <span>Matéria</span>
              <strong>{subject.subjectName}</strong>
              <b>{subject.questionsCorrect} / {subject.questionsAttempted}</b>
              <small>{formatAccuracy(subject.accuracyPercentage)}% de acerto</small>
            </article>
          ))}
          {summary.contents.map((content) => (
            <article className="is-content" key={content.contentId}>
              <span>Conteúdo · {content.subjectName}</span>
              <strong>{content.contentName}</strong>
              <b>{content.questionsCorrect} / {content.questionsAttempted}</b>
              <small>{formatAccuracy(content.accuracyPercentage)}% de acerto</small>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

function StartStudySessionDialog({
  origin,
  cycle,
  stage,
  subjects,
  contents,
  selectedSubjectId,
  selectedContentId,
  loadingSubjects,
  loadingContents,
  pending,
  error,
  onSubjectChange,
  onContentChange,
  onCancel,
  onSubmit
}: {
  origin: "CYCLE" | "FREE";
  cycle?: StudyCycle;
  stage?: StudyCycle["stages"][number];
  subjects: Subject[];
  contents: StudyContent[];
  selectedSubjectId: string;
  selectedContentId: string;
  loadingSubjects: boolean;
  loadingContents: boolean;
  pending: boolean;
  error?: string;
  onSubjectChange: (subjectId: string) => void;
  onContentChange: (contentId: string) => void;
  onCancel: () => void;
  onSubmit: () => void;
}) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const title = origin === "CYCLE" ? "Iniciar etapa atual" : "Iniciar sessão livre";

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (typeof dialog.showModal === "function") dialog.showModal();
    else dialog.setAttribute("open", "");
    return () => {
      if (dialog.open && typeof dialog.close === "function") dialog.close();
    };
  }, []);

  return (
    <dialog className="study-session-dialog" ref={dialogRef} aria-labelledby="study-session-dialog-title" onCancel={(event) => { event.preventDefault(); if (!pending) onCancel(); }}>
      <form className="study-session-dialog-panel" onSubmit={(event) => { event.preventDefault(); onSubmit(); }}>
        <header>
          <span className="study-session-dialog-mark" aria-hidden="true">▶</span>
          <div>
            <span className="card-kicker">{origin === "CYCLE" ? `${cycle?.name} · volta ${cycle?.currentRun?.number}` : "Estudo fora da ordem"}</span>
            <h2 id="study-session-dialog-title">{title}</h2>
            <p>{origin === "CYCLE" ? "A matéria vem da etapa atual; detalhe o conteúdo somente se for útil." : "Escolha a matéria. O conteúdo continua opcional."}</p>
          </div>
        </header>
        <div className="study-session-fields">
          {origin === "CYCLE" ? (
            <div className="study-session-fixed-subject"><span>Matéria</span><strong>{stage?.subjectName}</strong><small>Meta {formatCycleMinutes(stage?.targetMinutes ?? 0)}</small></div>
          ) : (
            <label>Matéria
              <select value={selectedSubjectId} onChange={(event) => onSubjectChange(event.target.value)} disabled={loadingSubjects} required>
                {loadingSubjects && <option value="">Carregando matérias…</option>}
                {!loadingSubjects && subjects.length === 0 && <option value="">Nenhuma matéria ativa</option>}
                {subjects.map((subject) => <option value={subject.id} key={subject.id}>{subject.name}</option>)}
              </select>
            </label>
          )}
          <label>Conteúdo (opcional)
            <select value={selectedContentId} onChange={(event) => onContentChange(event.target.value)} disabled={!selectedSubjectId || loadingContents}>
              <option value="">Sem conteúdo específico</option>
              {contents.map((content) => <option value={content.id} key={content.id}>{content.name}</option>)}
            </select>
          </label>
        </div>
        {error && <p className="form-error" role="alert">{error}</p>}
        <footer>
          <button className="secondary-button" type="button" onClick={onCancel} disabled={pending}>Cancelar</button>
          <button className="primary-button" type="submit" disabled={pending || !selectedSubjectId}>{pending ? "Iniciando…" : "Iniciar cronômetro"}</button>
        </footer>
      </form>
    </dialog>
  );
}

function ProtectedStudyCyclesPage() {
  const queryClient = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [showSuggestion, setShowSuggestion] = useState(false);
  const [cycleName, setCycleName] = useState("");
  const [suggestionName, setSuggestionName] = useState("");
  const [suggestedSubjects, setSuggestedSubjects] = useState<SuggestedSubjectDraft[]>([]);
  const [suggestionInitialized, setSuggestionInitialized] = useState(false);
  const [selectedCycleId, setSelectedCycleId] = useState<string>();
  const [draftName, setDraftName] = useState("");
  const [draftStages, setDraftStages] = useState<EditableCycleStage[]>([]);
  const [nextStageKey, setNextStageKey] = useState(1);
  const [pendingCycle, setPendingCycle] = useState<StudyCycle>();
  const [regenerationCycle, setRegenerationCycle] = useState<StudyCycle>();
  const [savedAsCustomized, setSavedAsCustomized] = useState(false);
  const [sessionComposer, setSessionComposer] = useState<"CYCLE" | "FREE">();
  const [sessionSubjectId, setSessionSubjectId] = useState("");
  const [sessionContentId, setSessionContentId] = useState("");
  const [finishingSession, setFinishingSession] = useState<StudySession>();
  const [effectiveDuration, setEffectiveDuration] = useState("");
  const [finishMeasuredSeconds, setFinishMeasuredSeconds] = useState(0);
  const [showRunLedger, setShowRunLedger] = useState(false);
  const [showManualSession, setShowManualSession] = useState(false);
  const [showStudyHistory, setShowStudyHistory] = useState(false);
  const [editingExerciseSession, setEditingExerciseSession] = useState<StudySession>();
  const [manualSubjectId, setManualSubjectId] = useState("");
  const [manualContentId, setManualContentId] = useState("");
  const [manualStartedAt, setManualStartedAt] = useState("");
  const [manualDuration, setManualDuration] = useState("");
  const [manualNotes, setManualNotes] = useState("");
  const auth = useQuery({ queryKey: ["auth-snapshot"], queryFn: loadAuthSnapshot });
  const cycles = useQuery({
    queryKey: ["study-cycles"],
    queryFn: listStudyCycles,
    enabled: auth.data?.state === "authenticated",
    staleTime: 30_000
  });
  const activeCycleId = cycles.data?.find((cycle) => cycle.status === "ACTIVE")?.id;
  const runHistory = useQuery({
    queryKey: ["study-cycle-runs", activeCycleId],
    queryFn: () => listStudyCycleRuns(activeCycleId!),
    enabled: showRunLedger && Boolean(activeCycleId),
    staleTime: 30_000
  });
  const studyHistory = useQuery({
    queryKey: ["study-session", "history"],
    queryFn: listStudySessionHistory,
    enabled: showStudyHistory || showManualSession,
    staleTime: 30_000
  });
  const exerciseSummary = useQuery({
    queryKey: ["study-session", "exercise-summary"],
    queryFn: loadExerciseSummary,
    enabled: showStudyHistory,
    staleTime: 30_000
  });
  const subjects = useQuery({
    queryKey: ["subjects", "active"],
    queryFn: () => listSubjects("active"),
    enabled: auth.data?.state === "authenticated" && (Boolean(selectedCycleId) || showSuggestion || sessionComposer === "FREE" || showManualSession),
    staleTime: 30_000
  });
  const currentSession = useQuery({
    queryKey: ["study-session", "current"],
    queryFn: loadCurrentStudySession,
    enabled: auth.data?.state === "authenticated",
    staleTime: 5_000
  });
  const sessionContents = useQuery({
    queryKey: ["contents", sessionSubjectId, "active"],
    queryFn: () => listContents(sessionSubjectId, "active"),
    enabled: Boolean(sessionComposer && sessionSubjectId),
    staleTime: 30_000
  });
  const manualContents = useQuery({
    queryKey: ["contents", manualSubjectId, "active"],
    queryFn: () => listContents(manualSubjectId, "active"),
    enabled: showManualSession && Boolean(manualSubjectId),
    staleTime: 30_000
  });
  const liveSessionSeconds = useLiveSessionSeconds(currentSession.data);

  useEffect(() => {
    if (!showSuggestion || suggestionInitialized || !subjects.data) return;
    setSuggestedSubjects(subjects.data.slice(0, 30).map((subject) => ({
      key: `suggestion-${subject.id}`,
      subjectId: subject.id,
      questionCount: 1,
      weight: 1,
      difficulty: "MEDIUM"
    })));
    setSuggestionInitialized(true);
  }, [showSuggestion, suggestionInitialized, subjects.data]);

  useEffect(() => {
    if (sessionComposer !== "FREE" || sessionSubjectId || !subjects.data?.length) return;
    setSessionSubjectId(subjects.data[0].id);
  }, [sessionComposer, sessionSubjectId, subjects.data]);

  useEffect(() => {
    if (!showManualSession || manualSubjectId || !subjects.data?.length) return;
    setManualSubjectId(subjects.data[0].id);
  }, [showManualSession, manualSubjectId, subjects.data]);

  function editCycle(cycle: StudyCycle) {
    setSavedAsCustomized(false);
    setSelectedCycleId(cycle.id);
    setDraftName(cycle.name);
    setDraftStages(cycle.stages.map((stage) => ({
      key: stage.id,
      subjectId: stage.subjectId,
      targetMinutes: stage.targetMinutes
    })));
  }

  const createMutation = useMutation({
    mutationFn: () => createStudyCycle(cycleName),
    onSuccess: (created) => {
      queryClient.setQueryData<StudyCycle[]>(["study-cycles"], (current = []) =>
        [...current, created].sort((first, second) =>
          first.name.localeCompare(second.name, "pt-BR", { sensitivity: "base" }))
      );
      setCycleName("");
      setShowCreate(false);
      editCycle(created);
    }
  });
  const suggestionMutation = useMutation({
    mutationFn: () => createSuggestedStudyCycle(
      suggestionName,
      suggestedSubjects.map(({ subjectId, questionCount, weight, difficulty }) => ({
        subjectId,
        questionCount,
        weight,
        difficulty
      }))
    ),
    onSuccess: (created) => {
      queryClient.setQueryData<StudyCycle[]>(["study-cycles"], (current = []) =>
        [...current, created].sort((first, second) =>
          first.name.localeCompare(second.name, "pt-BR", { sensitivity: "base" }))
      );
      closeSuggestion();
    }
  });
  const updateMutation = useMutation({
    mutationFn: () => updateStudyCycle(
      selectedCycleId!,
      draftName,
      draftStages.map(({ subjectId, targetMinutes }) => ({ subjectId, targetMinutes }))
    ),
    onSuccess: (updated) => {
      const becameCustomized = (
        cycles.data?.find((cycle) => cycle.id === updated.id)?.mode === "SUGGESTED"
        && updated.mode === "CUSTOM"
      );
      queryClient.setQueryData<StudyCycle[]>(["study-cycles"], (current = []) =>
        current.map((cycle) => cycle.id === updated.id ? updated : cycle)
      );
      editCycle(updated);
      setSavedAsCustomized(becameCustomized);
    }
  });
  const regenerationMutation = useMutation({
    mutationFn: ({ cycle, name, drafts }: { cycle: StudyCycle; name: string; drafts: SuggestedSubjectDraft[] }) =>
      regenerateStudyCycleSuggestion(
        cycle.id,
        name,
        drafts.map(({ subjectId, questionCount, weight, difficulty }) => ({ subjectId, questionCount, weight, difficulty }))
      ),
    onSuccess: (regenerated) => {
      queryClient.setQueryData<StudyCycle[]>(["study-cycles"], (current = []) =>
        current.map((cycle) => cycle.id === regenerated.id ? regenerated : cycle)
      );
      editCycle(regenerated);
      setRegenerationCycle(undefined);
    }
  });
  const activationMutation = useMutation({
    mutationFn: ({ cycle, currentRunAction }: { cycle: StudyCycle; currentRunAction?: CycleSwitchAction }) =>
      activateStudyCycle(cycle.id, currentRunAction),
    onSuccess: (activated, variables) => {
      queryClient.setQueryData<StudyCycle[]>(["study-cycles"], (current = []) =>
        current.map((cycle) => {
          if (cycle.id === activated.id) return activated;
          if (cycle.status !== "ACTIVE") return cycle;
          return {
            ...cycle,
            status: "INACTIVE",
            currentRun: variables.currentRunAction === "PAUSE" && cycle.currentRun
              ? { ...cycle.currentRun, status: "PAUSED" }
              : null
          };
        })
      );
      setPendingCycle(undefined);
      setSelectedCycleId(undefined);
    }
  });
  const startSessionMutation = useMutation({
    mutationFn: (input: StartStudySessionInput) => startStudySession(input),
    onSuccess: (started) => {
      queryClient.setQueryData(["study-session", "current"], started);
      setSessionComposer(undefined);
      setSessionContentId("");
    }
  });
  const timerMutation = useMutation({
    mutationFn: ({ id, action }: { id: string; action: "pause" | "resume" }) =>
      action === "pause" ? pauseStudySession(id) : resumeStudySession(id),
    onSuccess: (updated) => queryClient.setQueryData(["study-session", "current"], updated)
  });
  const finishSessionMutation = useMutation({
    mutationFn: ({ session, effectiveSeconds, exerciseResult, scheduleReviews }: { session: StudySession; effectiveSeconds: number; exerciseResult?: ExerciseResultInput; scheduleReviews: boolean }) =>
      finishStudySession(session.id, effectiveSeconds, session.version, exerciseResult, scheduleReviews),
    onSuccess: (finished) => {
      queryClient.setQueryData(["study-session", "current"], null);
      queryClient.setQueryData<StudySession[]>(["study-session", "history"], (current = []) => [
        finished,
        ...current.filter((session) => session.id !== finished.id)
      ]);
      void queryClient.invalidateQueries({ queryKey: ["study-cycles"] });
      void queryClient.invalidateQueries({ queryKey: ["study-cycle-runs"] });
      void queryClient.invalidateQueries({ queryKey: ["study-session", "exercise-summary"] });
      void queryClient.invalidateQueries({ queryKey: ["reviews"] });
      setFinishingSession(undefined);
      setEffectiveDuration("");
      setFinishMeasuredSeconds(0);
    }
  });
  const exerciseResultMutation = useMutation({
    mutationFn: ({ session, input }: { session: StudySession; input: ExerciseResultInput }) =>
      updateExerciseResult(session.id, input),
    onSuccess: (updated) => {
      queryClient.setQueryData<StudySession[]>(["study-session", "history"], (current = []) =>
        current.map((session) => session.id === updated.id ? updated : session)
      );
      void queryClient.invalidateQueries({ queryKey: ["study-session", "exercise-summary"] });
      setEditingExerciseSession(undefined);
    }
  });
  const manualSessionMutation = useMutation({
    mutationFn: (effectiveSeconds: number) => createManualStudySession({
      startedAtLocal: manualStartedAt.length === 16 ? `${manualStartedAt}:00` : manualStartedAt,
      effectiveSeconds,
      subjectId: manualSubjectId,
      ...(manualContentId ? { contentId: manualContentId } : {}),
      ...(manualNotes.trim() ? { notes: manualNotes.trim() } : {})
    }),
    onSuccess: (created) => {
      queryClient.setQueryData<StudySession[]>(["study-session", "history"], (current = []) => [
        created,
        ...current.filter((session) => session.id !== created.id)
      ]);
      void queryClient.invalidateQueries({ queryKey: ["study-cycles"] });
      void queryClient.invalidateQueries({ queryKey: ["study-cycle-runs"] });
      setShowStudyHistory(true);
      setShowManualSession(false);
      setManualSubjectId("");
      setManualContentId("");
      setManualStartedAt("");
      setManualDuration("");
      setManualNotes("");
    }
  });

  function submitCycle(event: FormEvent) {
    event.preventDefault();
    createMutation.mutate();
  }

  function openSuggestion() {
    setShowCreate(false);
    setSelectedCycleId(undefined);
    setShowSuggestion(true);
    setSuggestionInitialized(false);
    suggestionMutation.reset();
  }

  function closeSuggestion() {
    setShowSuggestion(false);
    setSuggestionName("");
    setSuggestedSubjects([]);
    setSuggestionInitialized(false);
  }

  function submitSuggestion(event: FormEvent) {
    event.preventDefault();
    suggestionMutation.mutate();
  }

  function updateSuggestedSubject(index: number, patch: Partial<Omit<SuggestedSubjectDraft, "key">>) {
    setSuggestedSubjects((current) => current.map((subject, subjectIndex) =>
      subjectIndex === index ? { ...subject, ...patch } : subject
    ));
  }

  function addSuggestedSubject() {
    const usedIds = new Set(suggestedSubjects.map((subject) => subject.subjectId));
    const available = subjects.data?.find((subject) => !usedIds.has(subject.id));
    if (!available) return;
    setSuggestedSubjects((current) => [...current, {
      key: `suggestion-${available.id}`,
      subjectId: available.id,
      questionCount: 1,
      weight: 1,
      difficulty: "MEDIUM"
    }]);
  }

  function submitCycleStages(event: FormEvent) {
    event.preventDefault();
    updateMutation.mutate();
  }

  function addStage() {
    const firstSubject = subjects.data?.[0];
    if (!firstSubject) return;
    setDraftStages((current) => [...current, {
      key: `new-stage-${nextStageKey}`,
      subjectId: firstSubject.id,
      targetMinutes: 30
    }]);
    setNextStageKey((current) => current + 1);
  }

  function updateStage(index: number, patch: Partial<Omit<EditableCycleStage, "key">>) {
    setDraftStages((current) => current.map((stage, stageIndex) =>
      stageIndex === index ? { ...stage, ...patch } : stage
    ));
  }

  function moveStage(index: number, direction: -1 | 1) {
    const target = index + direction;
    if (target < 0 || target >= draftStages.length) return;
    setDraftStages((current) => {
      const reordered = [...current];
      [reordered[index], reordered[target]] = [reordered[target], reordered[index]];
      return reordered;
    });
  }

  function subjectName(subjectId: string) {
    return subjects.data?.find((subject) => subject.id === subjectId)?.name
      ?? cycles.data?.find((cycle) => cycle.id === selectedCycleId)?.stages.find((stage) => stage.subjectId === subjectId)?.subjectName
      ?? "Matéria";
  }

  const totalDraftMinutes = draftStages.reduce((total, stage) => total + (Number(stage.targetMinutes) || 0), 0);
  const subjectTotals = Array.from(draftStages.reduce((totals, stage) => {
    const total = totals.get(stage.subjectId);
    if (total) {
      total.totalMinutes += Number(stage.targetMinutes) || 0;
      total.appearances += 1;
    } else {
      totals.set(stage.subjectId, {
        subjectId: stage.subjectId,
        subjectName: subjectName(stage.subjectId),
        totalMinutes: Number(stage.targetMinutes) || 0,
        appearances: 1
      });
    }
    return totals;
  }, new Map<string, { subjectId: string; subjectName: string; totalMinutes: number; appearances: number }>()).values())
    .map((total) => ({
      ...total,
      percentage: totalDraftMinutes > 0 ? Math.round((total.totalMinutes / totalDraftMinutes) * 100) : 0
    }));
  const stagesAreValid = draftStages.every((stage) => stage.targetMinutes > 0 && stage.targetMinutes % 5 === 0);
  const activeCycle = cycles.data?.find((cycle) => cycle.status === "ACTIVE");
  const activeStage = activeCycle?.stages.find((stage) =>
    stage.position === (activeCycle.currentRun?.currentStagePosition ?? 1)
  );
  const activeStageMeasuredSeconds = (activeStage?.creditedSeconds ?? 0) + (currentSession.data?.origin === "CYCLE"
    && currentSession.data.cycle?.stageId === activeStage?.id
    ? liveSessionSeconds
    : 0);
  const activeStageTargetSeconds = (activeStage?.targetMinutes ?? 0) * 60;
  const activeStageProgress = activeStageTargetSeconds > 0
    ? Math.min(100, Math.round((activeStageMeasuredSeconds / activeStageTargetSeconds) * 100))
    : 0;
  const activeStageRemainingMinutes = Math.max(0, Math.ceil((activeStageTargetSeconds - activeStageMeasuredSeconds) / 60));
  const otherCycles = cycles.data?.filter((cycle) => cycle.status !== "ACTIVE") ?? [];
  const selectedCycle = cycles.data?.find((cycle) => cycle.id === selectedCycleId);
  const activeCycleWouldBeEmpty = selectedCycle?.status === "ACTIVE" && draftStages.length === 0;
  const suggestedTotalMinutes = Math.min(30 * 60, Math.max(10 * 60, suggestedSubjects.length * 2 * 60));
  const suggestionIsValid = Boolean(
    suggestionName.trim()
    && suggestedSubjects.length > 0
    && suggestedSubjects.every((subject) => subject.questionCount > 0 && subject.weight > 0)
  );

  function requestCycleActivation(cycle: StudyCycle) {
    if (activeCycle && activeCycle.id !== cycle.id) {
      activationMutation.reset();
      setPendingCycle(cycle);
      return;
    }
    activationMutation.mutate({ cycle });
  }

  function openCycleSession() {
    const stage = activeStage;
    if (!activeCycle || !stage || currentSession.data) return;
    setSessionSubjectId(stage.subjectId);
    setSessionContentId("");
    startSessionMutation.reset();
    setSessionComposer("CYCLE");
  }

  function openFreeSession() {
    if (currentSession.data) return;
    setSessionSubjectId(subjects.data?.[0]?.id ?? "");
    setSessionContentId("");
    startSessionMutation.reset();
    setSessionComposer("FREE");
  }

  function closeSessionComposer() {
    setSessionComposer(undefined);
    setSessionSubjectId("");
    setSessionContentId("");
    startSessionMutation.reset();
  }

  function submitStudySession() {
    if (sessionComposer === "CYCLE" && activeCycle) {
      startSessionMutation.mutate({
        origin: "CYCLE",
        cycleId: activeCycle.id,
        ...(sessionContentId ? { contentId: sessionContentId } : {})
      });
      return;
    }
    if (sessionComposer === "FREE" && sessionSubjectId) {
      startSessionMutation.mutate({
        origin: "FREE",
        subjectId: sessionSubjectId,
        ...(sessionContentId ? { contentId: sessionContentId } : {})
      });
    }
  }

  function openFinishSession() {
    if (!currentSession.data) return;
    finishSessionMutation.reset();
    setEffectiveDuration(formatTimer(liveSessionSeconds));
    setFinishMeasuredSeconds(liveSessionSeconds);
    setFinishingSession(currentSession.data);
  }

  function closeFinishSession() {
    if (finishSessionMutation.isPending) return;
    setFinishingSession(undefined);
    setEffectiveDuration("");
    setFinishMeasuredSeconds(0);
    finishSessionMutation.reset();
  }

  function openManualSession() {
    manualSessionMutation.reset();
    setShowManualSession(true);
  }

  function closeManualSession() {
    if (manualSessionMutation.isPending) return;
    setShowManualSession(false);
    setManualSubjectId("");
    setManualContentId("");
    setManualStartedAt("");
    setManualDuration("");
    setManualNotes("");
    manualSessionMutation.reset();
  }

  if (auth.isPending) {
    return <AppShell authenticated={false}><main className="content"><p>Verificando acesso…</p></main></AppShell>;
  }
  if (auth.isError || auth.data?.state !== "authenticated") {
    return <Navigate to="/" replace />;
  }

  return (
    <AppShell authenticated activeSection="cycles" topbarMeta="Planejamento personalizado">
      <main className="content subjects-content cycle-content">
        <div className="page-heading subjects-heading">
          <div>
            <span className="eyebrow">Sua ordem de estudo</span>
            <h1>Ciclos de estudo</h1>
            <p>Deixe o sistema distribuir seu tempo ou monte a sequência manualmente.</p>
          </div>
          <div className="cycle-heading-actions">
            <button className="secondary-button" type="button" onClick={openFreeSession} disabled={Boolean(currentSession.data)}>Sessão livre</button>
            <button className="secondary-button" type="button" onClick={() => { closeSuggestion(); setShowCreate(true); }} disabled={showCreate}>Novo ciclo</button>
            <button className="primary-button" type="button" onClick={openSuggestion} disabled={showSuggestion}>Gerar ciclo sugerido</button>
          </div>
        </div>
        <section className="study-capture-bar" aria-label="Registrar estudo concluído">
          <span className="study-capture-mark" aria-hidden="true">＋</span>
          <div>
            <strong>Estudou sem o cronômetro?</strong>
            <p>Registre a data e o tempo líquido; o ciclo e o histórico são atualizados juntos.</p>
          </div>
          <div className="study-capture-actions">
            <button className="text-button" type="button" onClick={() => setShowStudyHistory((visible) => !visible)}>Histórico</button>
            <button className="secondary-button" type="button" onClick={openManualSession}>Registrar estudo</button>
          </div>
        </section>
        {currentSession.data && (
          <StudySessionDesk
            session={currentSession.data}
            seconds={liveSessionSeconds}
            pending={timerMutation.isPending}
            error={timerMutation.isError
              ? timerMutation.error instanceof ApiError ? timerMutation.error.message : "Não foi possível sincronizar o cronômetro."
              : undefined}
            onPause={() => timerMutation.mutate({ id: currentSession.data!.id, action: "pause" })}
            onResume={() => timerMutation.mutate({ id: currentSession.data!.id, action: "resume" })}
            onFinish={openFinishSession}
          />
        )}
        {currentSession.isError && (
          <p className="form-error" role="alert">Não foi possível recuperar o cronômetro aberto.</p>
        )}
        {showStudyHistory && (
          <>
            <ExerciseSummaryPanel
              summary={exerciseSummary.data}
              pending={exerciseSummary.isPending}
              error={exerciseSummary.isError}
            />
            <StudySessionHistory
              sessions={studyHistory.data ?? []}
              pending={studyHistory.isPending}
              error={studyHistory.isError}
              onEditExercises={(session) => { exerciseResultMutation.reset(); setEditingExerciseSession(session); }}
            />
          </>
        )}
        {showSuggestion && (
          <section className="cycle-suggestion-composer" aria-labelledby="suggestion-title">
            <header className="cycle-suggestion-header">
              <div>
                <span className="card-kicker">Planejamento explicado</span>
                <h2 id="suggestion-title">Transforme o edital em um ciclo</h2>
                <p>Informe a incidência, o peso e a dificuldade. O sistema preserva uma hora mínima por matéria e intercala as aparições.</p>
              </div>
              <div className="cycle-suggestion-total" aria-label={`Duração estimada: ${formatCycleMinutes(suggestedTotalMinutes)}`}>
                <span>Duração estimada</span>
                <strong>{formatCycleMinutes(suggestedTotalMinutes)}</strong>
                <small>{suggestedSubjects.length} {suggestedSubjects.length === 1 ? "matéria" : "matérias"}</small>
              </div>
            </header>
            <div className="cycle-formula-strip cycle-formula-preview" aria-label="Regras da sugestão">
              <span><strong>Duração</strong>2h por matéria · mínimo 10h · máximo 30h</span>
              <span><strong>Prioridade</strong>questões × peso × dificuldade</span>
            </div>
            <form className="cycle-suggestion-form" onSubmit={submitSuggestion}>
              <label className="cycle-name-field">Nome da sugestão<input autoFocus required maxLength={120} value={suggestionName} onChange={(event) => setSuggestionName(event.target.value)} /></label>
              {subjects.isPending && <p className="cycle-subject-state" aria-live="polite">Abrindo suas matérias…</p>}
              {subjects.isError && <p className="form-error" role="alert">Não foi possível carregar as matérias ativas.</p>}
              {subjects.data?.length === 0 && (
                <p className="cycle-subject-state">Cadastre ao menos uma matéria ativa antes de gerar uma sugestão.</p>
              )}
              <div className="cycle-suggestion-inputs">
                {suggestedSubjects.map((draft, index) => {
                  const selectedSubject = subjects.data?.find((subject) => subject.id === draft.subjectId);
                  const selectedName = selectedSubject?.name ?? `Matéria ${index + 1}`;
                  return (
                    <fieldset className="cycle-suggestion-subject" key={draft.key}>
                      <legend>{String(index + 1).padStart(2, "0")} · {selectedName}</legend>
                      <label>Matéria
                        <select value={draft.subjectId} onChange={(event) => updateSuggestedSubject(index, { subjectId: event.target.value })}>
                          {subjects.data?.map((subject) => (
                            <option
                              value={subject.id}
                              key={subject.id}
                              disabled={suggestedSubjects.some((item, itemIndex) => itemIndex !== index && item.subjectId === subject.id)}
                            >
                              {subject.name}
                            </option>
                          ))}
                        </select>
                      </label>
                      <label>Questões de {selectedName}
                        <input type="number" min={1} required value={draft.questionCount} onChange={(event) => updateSuggestedSubject(index, { questionCount: Number(event.target.value) })} />
                      </label>
                      <label>Peso de {selectedName}
                        <input type="number" min={1} required value={draft.weight} onChange={(event) => updateSuggestedSubject(index, { weight: Number(event.target.value) })} />
                      </label>
                      <label>Dificuldade de {selectedName}
                        <select value={draft.difficulty} onChange={(event) => updateSuggestedSubject(index, { difficulty: event.target.value as StudyCycleDifficulty })}>
                          <option value="EASY">Fácil · fator 1,00</option>
                          <option value="MEDIUM">Média · fator 1,25</option>
                          <option value="HARD">Difícil · fator 1,50</option>
                        </select>
                      </label>
                      <button className="suggestion-remove-subject" type="button" aria-label={`Remover ${selectedName} da sugestão`} onClick={() => setSuggestedSubjects((current) => current.filter((_, subjectIndex) => subjectIndex !== index))}>Remover</button>
                    </fieldset>
                  );
                })}
              </div>
              {subjects.data && suggestedSubjects.length < Math.min(subjects.data.length, 30) && (
                <button className="cycle-add-stage" type="button" onClick={addSuggestedSubject}>+ Adicionar matéria</button>
              )}
              {suggestionMutation.isError && <p className="form-error" role="alert">{suggestionMutation.error instanceof ApiError ? suggestionMutation.error.message : "Não foi possível gerar o ciclo sugerido."}</p>}
              <div className="cycle-editor-actions">
                <button className="secondary-button" type="button" onClick={closeSuggestion}>Cancelar</button>
                <button className="primary-button" type="submit" disabled={suggestionMutation.isPending || !suggestionIsValid}>{suggestionMutation.isPending ? "Calculando…" : "Gerar planejamento"}</button>
              </div>
            </form>
          </section>
        )}
        {showCreate && (
          <section className="subject-composer cycle-composer" aria-labelledby="new-cycle-title">
            <div>
              <span className="card-kicker">Novo rascunho</span>
              <h2 id="new-cycle-title">Nomeie seu ciclo</h2>
              <p>Você poderá montar e reorganizar as atividades logo depois.</p>
            </div>
            <form className="subject-form" onSubmit={submitCycle}>
              <label>Nome do ciclo<input autoFocus required maxLength={120} value={cycleName} onChange={(event) => setCycleName(event.target.value)} /></label>
              {createMutation.isError && <p className="form-error" role="alert">{createMutation.error instanceof ApiError ? createMutation.error.message : "Não foi possível criar o ciclo."}</p>}
              <div className="form-actions">
                <button className="secondary-button" type="button" onClick={() => { setShowCreate(false); setCycleName(""); }}>Cancelar</button>
                <button className="primary-button" type="submit" disabled={createMutation.isPending}>{createMutation.isPending ? "Criando…" : "Criar rascunho"}</button>
              </div>
            </form>
          </section>
        )}
        {cycles.isPending && (
          <section className="subjects-loading" aria-busy="true" aria-live="polite">
            {[0, 1].map((item) => <span className="subject-row-skeleton skeleton" key={item} />)}
            <span className="sr-only">Carregando ciclos</span>
          </section>
        )}
        {cycles.isError && (
          <section className="subjects-error" role="alert">
            <span className="subject-bookmark error-bookmark" aria-hidden="true"><CyclesIcon /></span>
            <div><h2>Seus ciclos não puderam ser abertos</h2><p>Verifique a conexão e tente novamente.</p></div>
            <button className="secondary-button" type="button" onClick={() => void cycles.refetch()}>Tentar novamente</button>
          </section>
        )}
        {cycles.data?.length === 0 && (
          <section className="subjects-empty">
            <span className="empty-bookmark" aria-hidden="true"><CyclesIcon /></span>
            <h2>Seu primeiro ciclo começa aqui</h2>
            <p>Crie um rascunho e organize as matérias na ordem que funciona para você.</p>
          </section>
        )}
        {activeCycle && activeCycle.currentRun && (
          <section className="cycle-active-card" aria-label="Ciclo ativo">
            <header className="cycle-active-head">
              <div className="cycle-active-mark" aria-hidden="true"><CyclesIcon /></div>
              <div className="cycle-active-copy">
                <span className="cycle-active-status"><span aria-hidden="true" />Recebendo seus estudos</span>
                <h2>{activeCycle.name}</h2>
                <p>Volta {activeCycle.currentRun.number}</p>
              </div>
              <div className="cycle-active-total">
                <span>Duração da volta</span>
                <strong>{formatCycleMinutes(activeCycle.totalMinutes)}</strong>
              </div>
              <button className="secondary-button cycle-active-edit" type="button" aria-label={`Editar ${activeCycle.name}`} onClick={() => editCycle(activeCycle)}>Editar ciclo</button>
            </header>
            {activeStage && (
              <section className="cycle-current-stage" aria-label={`Etapa atual: ${activeStage.subjectName}`}>
                <span className="cycle-current-stage-index" aria-hidden="true">{String(activeStage.position).padStart(2, "0")}</span>
                <div className="cycle-current-stage-copy">
                  <span className="card-kicker">Etapa atual</span>
                  <h3>{activeStage.subjectName}</h3>
                  <p>Meta {formatCycleMinutes(activeStage.targetMinutes)}</p>
                </div>
                <div className="cycle-current-stage-numbers">
                  <strong>{formatCycleMinutes(Math.floor(activeStageMeasuredSeconds / 60))} realizados</strong>
                  <span>{formatCycleMinutes(activeStageRemainingMinutes)} restantes</span>
                </div>
                <button className="primary-button" type="button" aria-label={`Iniciar ${activeStage.subjectName}`} onClick={openCycleSession} disabled={Boolean(currentSession.data)}>
                  {currentSession.data ? "Cronômetro em andamento" : "Iniciar etapa"}
                </button>
                <span className="cycle-current-stage-track" aria-hidden="true"><span style={{ width: `${activeStageProgress}%` }} /></span>
              </section>
            )}
            <div className="cycle-flow-intro">
              <div><span className="card-kicker">Mapa da volta</span><strong>Escolha livremente o que estudar</strong></div>
              <div className="cycle-flow-note">
                <p>A ordem é uma sugestão. O crédito pode adiantar etapas da mesma matéria.</p>
                <button
                  className="secondary-button cycle-ledger-toggle"
                  type="button"
                  aria-expanded={showRunLedger}
                  onClick={() => setShowRunLedger((current) => !current)}
                >
                  {showRunLedger ? "Fechar caderno de voltas" : "Abrir caderno de voltas"}
                </button>
              </div>
            </div>
            <ol className="cycle-flow-map" aria-label="Fluxo completo da volta">
              {activeCycle.stages.map((stage) => (
                <li className={`cycle-flow-item ${stage.position === activeCycle.currentRun?.currentStagePosition ? "current" : ""}`} key={stage.id}>
                  <span className="cycle-flow-index" aria-hidden="true">{String(stage.position).padStart(2, "0")}</span>
                  <div><strong>{stage.subjectName}</strong><span>Atividade {stage.position}</span></div>
                  {stage.position === activeCycle.currentRun?.currentStagePosition && <span className="cycle-suggestion">Sugestão</span>}
                  <strong className="cycle-flow-duration">{formatCycleMinutes(stage.targetMinutes)}</strong>
                </li>
              ))}
            </ol>
            {showRunLedger && (
              <section className="cycle-run-ledger" aria-label="Caderno de voltas">
                <header>
                  <div>
                    <span className="card-kicker">Registro preservado</span>
                    <h3>Caderno de voltas</h3>
                  </div>
                  <p>Metas e créditos ficam guardados como eram em cada volta.</p>
                </header>
                {runHistory.isPending && <p className="cycle-ledger-state" aria-live="polite">Abrindo o caderno…</p>}
                {runHistory.isError && (
                  <div className="cycle-ledger-state" role="alert">
                    <span>Não foi possível abrir as voltas.</span>
                    <button className="secondary-button" type="button" onClick={() => void runHistory.refetch()}>Tentar novamente</button>
                  </div>
                )}
                {runHistory.data && (
                  <ol className="cycle-run-list">
                    {runHistory.data.map((run) => (
                      <li className={`cycle-run-entry ${run.status === "IN_PROGRESS" ? "current" : ""}`} key={run.id}>
                        <span className="cycle-run-marker" aria-hidden="true">{String(run.number).padStart(2, "0")}</span>
                        <article>
                          <header>
                            <div><strong>Volta {run.number}</strong><span>{formatRunDate(run.startedAt)}</span></div>
                            <span className={`cycle-run-status ${run.status.toLowerCase()}`}>
                              {run.status === "COMPLETED" ? "Concluída" : run.status === "IN_PROGRESS" ? "Em andamento" : run.status === "PAUSED" ? "Pausada" : "Encerrada"}
                            </span>
                          </header>
                          <ul>
                            {run.stages.map((stage) => (
                              <li key={stage.id}>
                                <div><strong>{stage.subjectName}</strong><span>Etapa {stage.position}</span></div>
                                <span>{formatCycleMinutes(Math.floor(stage.creditedSeconds / 60))} de {formatCycleMinutes(Math.floor(stage.targetSeconds / 60))}</span>
                              </li>
                            ))}
                          </ul>
                        </article>
                      </li>
                    ))}
                  </ol>
                )}
              </section>
            )}
            <CycleSuggestionExplanation cycle={activeCycle} />
          </section>
        )}
        {cycles.data && cycles.data.length > 0 && !activeCycle && (
          <section className="cycle-no-active" aria-label="Nenhum ciclo ativo">
            <span className="cycle-card-mark" aria-hidden="true"><CyclesIcon /></span>
            <div><strong>Nenhum ciclo está recebendo seus estudos</strong><p>Ative um planejamento abaixo para definir o foco atual.</p></div>
          </section>
        )}
        {activationMutation.isError && !pendingCycle && (
          <p className="form-error cycle-activation-error" role="alert">
            {activationMutation.error instanceof ApiError ? activationMutation.error.message : "Não foi possível ativar o ciclo."}
          </p>
        )}
        {otherCycles.length > 0 && (
          <section className="cycle-draft-list" aria-label="Seus outros ciclos">
            {otherCycles.map((cycle) => (
              <article className="cycle-draft-card" key={cycle.id}>
                <span className="cycle-card-mark" aria-hidden="true"><CyclesIcon /></span>
                <div>
                  <span className="card-kicker">{cycle.mode === "SUGGESTED" ? "Sugestão explicada" : cycle.currentRun ? "Pausado" : cycle.status === "DRAFT" ? "Rascunho" : "Sem volta aberta"}</span>
                  <h2>{cycle.name}</h2>
                  <p>{cycle.currentRun ? `Volta ${cycle.currentRun.number} pausada · ${cycle.stages.length} ${cycle.stages.length === 1 ? "atividade" : "atividades"}` : `${cycle.stages.length} ${cycle.stages.length === 1 ? "atividade" : "atividades"}`}</p>
                </div>
                <strong className="cycle-total">{formatCycleMinutes(cycle.totalMinutes)}</strong>
                <CycleSuggestionExplanation cycle={cycle} />
                <div className="cycle-card-actions">
                  <button className="secondary-button" type="button" aria-label={`Editar ${cycle.name}`} onClick={() => editCycle(cycle)}>Editar</button>
                  <button
                    className="primary-button cycle-activate-button"
                    type="button"
                    aria-label={`${cycle.currentRun ? "Retomar" : "Ativar"} ${cycle.name}`}
                    onClick={() => requestCycleActivation(cycle)}
                    disabled={!cycle.activatable || activationMutation.isPending}
                  >
                    {activationMutation.isPending && activationMutation.variables?.cycle.id === cycle.id ? "Trocando…" : cycle.currentRun ? "Retomar" : "Ativar"}
                  </button>
                </div>
              </article>
            ))}
          </section>
        )}
        {selectedCycleId && (
          <section className="cycle-workbench" aria-labelledby="cycle-editor-title">
            <header className="cycle-workbench-header">
              <div>
                <span className="card-kicker">Editor do ciclo</span>
                <h2 id="cycle-editor-title">Monte a sequência</h2>
                <p>Repita matérias quando quiser e ajuste a ordem ao seu ritmo.</p>
              </div>
              <div className="cycle-running-total" aria-label={`Duração total do ciclo: ${formatCycleMinutes(totalDraftMinutes)}`}>
                <span>Total do ciclo</span>
                <strong>{formatCycleMinutes(totalDraftMinutes)}</strong>
              </div>
            </header>
            <form className="cycle-editor-form" onSubmit={submitCycleStages}>
              {selectedCycle?.mode === "SUGGESTED" && (
                <aside className="cycle-mode-notice" role="status" aria-label="Mudança de modo">
                  <span className="cycle-mode-mark" aria-hidden="true">!</span>
                  <div>
                    <strong>Esta sugestão está prestes a virar o seu planejamento.</strong>
                    <p>Ao salvar, este ciclo passará a personalizado. Novas entradas de sugestão não substituirão suas atividades sem confirmação.</p>
                  </div>
                </aside>
              )}
              <label className="cycle-name-field">Nome do ciclo<input required maxLength={120} value={draftName} onChange={(event) => setDraftName(event.target.value)} /></label>
              {subjects.isPending && <p className="cycle-subject-state" aria-live="polite">Abrindo suas matérias…</p>}
              {subjects.isError && <p className="form-error" role="alert">Não foi possível carregar as matérias ativas.</p>}
              {subjects.data?.length === 0 && (
                <p className="cycle-subject-state">Crie ou restaure uma matéria ativa antes de adicionar atividades.</p>
              )}
              <div className="cycle-stage-list">
                {draftStages.map((stage, index) => {
                  const currentSubjectName = subjectName(stage.subjectId);
                  return (
                    <article className="cycle-stage-card" key={stage.key}>
                      <div className="cycle-stage-index" aria-hidden="true">{String(index + 1).padStart(2, "0")}</div>
                      <div className="cycle-stage-fields">
                        <label>Matéria da atividade {index + 1}
                          <select value={stage.subjectId} onChange={(event) => updateStage(index, { subjectId: event.target.value })}>
                            {subjects.data?.map((subject) => <option value={subject.id} key={subject.id}>{subject.name}</option>)}
                          </select>
                        </label>
                        <label>Duração da atividade {index + 1} em minutos
                          <input type="number" required min={5} step={5} value={stage.targetMinutes} onChange={(event) => updateStage(index, { targetMinutes: Number(event.target.value) })} />
                        </label>
                        {stage.targetMinutes > 180 && <p className="cycle-long-warning" role="alert">Bloco longo: considere dividir esta matéria em mais aparições.</p>}
                      </div>
                      <div className="cycle-stage-actions" aria-label={`Ordenar ou remover ${currentSubjectName}`}>
                        <button type="button" aria-label={`Mover ${currentSubjectName} para cima`} onClick={() => moveStage(index, -1)} disabled={index === 0}>↑</button>
                        <button type="button" aria-label={`Mover ${currentSubjectName} para baixo`} onClick={() => moveStage(index, 1)} disabled={index === draftStages.length - 1}>↓</button>
                        <button className="remove-stage-button" type="button" aria-label={`Remover ${currentSubjectName}`} onClick={() => setDraftStages((current) => current.filter((_, stageIndex) => stageIndex !== index))}>Remover</button>
                      </div>
                    </article>
                  );
                })}
              </div>
              {draftStages.length === 0 && (
                <p className="cycle-empty-hint">
                  {activeCycleWouldBeEmpty
                    ? "O ciclo ativo precisa manter pelo menos uma atividade."
                    : "Adicione pelo menos uma atividade para que este ciclo possa ser ativado depois."}
                </p>
              )}
              <button className="cycle-add-stage" type="button" aria-label="Adicionar atividade" onClick={addStage} disabled={!subjects.data?.length}>+ Adicionar atividade</button>
              {updateMutation.isError && <p className="form-error" role="alert">{updateMutation.error instanceof ApiError ? updateMutation.error.message : "Não foi possível salvar o ciclo."}</p>}
              {updateMutation.isSuccess && (
                <p className="form-success" role="status">
                  {savedAsCustomized ? "Ciclo salvo como personalizado." : "Ciclo salvo."}
                </p>
              )}
              <div className="cycle-editor-actions">
                <button className="secondary-button" type="button" onClick={() => setSelectedCycleId(undefined)}>Fechar editor</button>
                <button className="secondary-button" type="button" onClick={() => selectedCycle && setRegenerationCycle(selectedCycle)}>Gerar nova sugestão</button>
                <button className="primary-button" type="submit" disabled={updateMutation.isPending || !draftName.trim() || !stagesAreValid || activeCycleWouldBeEmpty}>{updateMutation.isPending ? "Salvando…" : "Salvar ciclo"}</button>
              </div>
            </form>
            <section className="cycle-subject-totals" aria-labelledby="cycle-subject-totals-title">
              <header className="cycle-subject-totals-header">
                <div>
                  <span className="card-kicker">Fechamento do ciclo</span>
                  <h3 id="cycle-subject-totals-title">Total por matéria</h3>
                  <p>Confira o tempo acumulado e a participação de cada matéria.</p>
                </div>
                <strong className="cycle-subject-count">{subjectTotals.length} {subjectTotals.length === 1 ? "matéria" : "matérias"}</strong>
              </header>
              {subjectTotals.length > 0 ? (
                <ul className="cycle-subject-total-list">
                  {subjectTotals.map((total) => (
                    <li className="cycle-subject-total-row" key={total.subjectId}>
                      <div className="cycle-subject-total-copy">
                        <strong>{total.subjectName}</strong>
                        <span>{total.appearances} {total.appearances === 1 ? "aparição" : "aparições"} · {total.percentage}% do ciclo</span>
                      </div>
                      <strong className="cycle-subject-total-value" aria-label={`Total de ${total.subjectName}: ${formatCycleMinutes(total.totalMinutes)}`}>
                        {formatCycleMinutes(total.totalMinutes)}
                      </strong>
                      <span className="cycle-subject-share-track" aria-hidden="true">
                        <span style={{ width: `${total.percentage}%` }} />
                      </span>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="cycle-subject-totals-empty">Adicione atividades para ver a distribuição do ciclo por matéria.</p>
              )}
            </section>
          </section>
        )}
        {pendingCycle && activeCycle && (
          <CycleSwitchDialog
            currentCycle={activeCycle}
            targetCycle={pendingCycle}
            pending={activationMutation.isPending}
            error={activationMutation.isError
              ? activationMutation.error instanceof ApiError ? activationMutation.error.message : "Não foi possível trocar de ciclo."
              : undefined}
            onCancel={() => { activationMutation.reset(); setPendingCycle(undefined); }}
            onChoose={(currentRunAction) => activationMutation.mutate({ cycle: pendingCycle, currentRunAction })}
          />
        )}
        {regenerationCycle && (
          <RegenerationDialog
            cycle={regenerationCycle}
            subjects={subjects.data ?? []}
            pending={regenerationMutation.isPending}
            error={regenerationMutation.isError
              ? regenerationMutation.error instanceof ApiError ? regenerationMutation.error.message : "Não foi possível regenerar a sugestão."
              : undefined}
            onCancel={() => { regenerationMutation.reset(); setRegenerationCycle(undefined); }}
            onConfirm={(name, drafts) => regenerationMutation.mutate({ cycle: regenerationCycle, name, drafts })}
          />
        )}
        {sessionComposer && (
          <StartStudySessionDialog
            origin={sessionComposer}
            cycle={activeCycle}
            stage={activeStage}
            subjects={subjects.data ?? []}
            contents={sessionContents.data ?? []}
            selectedSubjectId={sessionSubjectId}
            selectedContentId={sessionContentId}
            loadingSubjects={subjects.isPending}
            loadingContents={sessionContents.isPending}
            pending={startSessionMutation.isPending}
            error={startSessionMutation.isError
              ? startSessionMutation.error instanceof ApiError ? startSessionMutation.error.message : "Não foi possível iniciar o cronômetro."
              : undefined}
            onSubjectChange={(subjectId) => { setSessionSubjectId(subjectId); setSessionContentId(""); }}
            onContentChange={setSessionContentId}
            onCancel={closeSessionComposer}
            onSubmit={submitStudySession}
          />
        )}
        {finishingSession && (
          <FinishStudySessionDialog
            session={finishingSession}
            measuredSeconds={finishMeasuredSeconds}
            effectiveDuration={effectiveDuration}
            pending={finishSessionMutation.isPending}
            error={finishSessionMutation.isError
              ? finishSessionMutation.error instanceof ApiError ? finishSessionMutation.error.message : "Não foi possível finalizar a sessão."
              : undefined}
            onDurationChange={setEffectiveDuration}
            onCancel={closeFinishSession}
            onSubmit={(effectiveSeconds, exerciseResult, scheduleReviews) => finishSessionMutation.mutate({ session: finishingSession, effectiveSeconds, exerciseResult, scheduleReviews })}
          />
        )}
        {editingExerciseSession && (
          <ExerciseResultDialog
            session={editingExerciseSession}
            pending={exerciseResultMutation.isPending}
            error={exerciseResultMutation.isError
              ? exerciseResultMutation.error instanceof ApiError ? exerciseResultMutation.error.message : "Não foi possível atualizar os exercícios."
              : undefined}
            onCancel={() => { if (!exerciseResultMutation.isPending) setEditingExerciseSession(undefined); }}
            onSubmit={(input) => exerciseResultMutation.mutate({ session: editingExerciseSession, input })}
          />
        )}
        {showManualSession && (
          <ManualStudySessionDialog
            subjects={subjects.data ?? []}
            contents={manualContents.data ?? []}
            selectedSubjectId={manualSubjectId}
            selectedContentId={manualContentId}
            startedAtLocal={manualStartedAt}
            effectiveDuration={manualDuration}
            notes={manualNotes}
            loadingSubjects={subjects.isPending}
            loadingContents={manualContents.isPending}
            pending={manualSessionMutation.isPending}
            error={manualSessionMutation.isError
              ? manualSessionMutation.error instanceof ApiError ? manualSessionMutation.error.message : "Não foi possível registrar o estudo concluído."
              : undefined}
            onSubjectChange={(subjectId) => { setManualSubjectId(subjectId); setManualContentId(""); }}
            onContentChange={setManualContentId}
            onStartedAtChange={setManualStartedAt}
            onDurationChange={setManualDuration}
            onNotesChange={setManualNotes}
            onCancel={closeManualSession}
            onSubmit={(effectiveSeconds) => manualSessionMutation.mutate(effectiveSeconds)}
          />
        )}
      </main>
    </AppShell>
  );
}

function ProtectedSubjectsPage() {
  const queryClient = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [name, setName] = useState("");
  const [status, setStatus] = useState<SubjectStatus>("active");
  const auth = useQuery({ queryKey: ["auth-snapshot"], queryFn: loadAuthSnapshot });
  const subjects = useQuery({
    queryKey: ["subjects", status],
    queryFn: () => listSubjects(status),
    enabled: auth.data?.state === "authenticated",
    staleTime: 30_000
  });
  const createMutation = useMutation({
    mutationFn: () => createSubject(name),
    onSuccess: (created) => {
      queryClient.setQueryData<Subject[]>(["subjects", "active"], (current = []) =>
        [...current, created].sort((first, second) =>
          first.name.localeCompare(second.name, "pt-BR", { sensitivity: "base" }))
      );
      setName("");
      setShowCreate(false);
    }
  });

  function submitSubject(event: FormEvent) {
    event.preventDefault();
    createMutation.mutate();
  }

  if (auth.isPending) {
    return <AppShell authenticated={false}><main className="content"><p>Verificando acesso…</p></main></AppShell>;
  }
  if (auth.isError || auth.data?.state !== "authenticated") {
    return <Navigate to="/" replace />;
  }

  return (
    <AppShell authenticated activeSection="subjects" topbarMeta="Catálogo de matérias">
      <main className="content subjects-content">
        <div className="page-heading subjects-heading">
          <div>
            <span className="eyebrow">Seu catálogo de estudo</span>
            <h1>Minhas matérias</h1>
            <p>Organize as áreas que fazem parte da sua preparação.</p>
          </div>
          <button className="primary-button" type="button" onClick={() => { setStatus("active"); setShowCreate(true); }}>Nova matéria</button>
        </div>
        {showCreate && (
          <section className="subject-composer" aria-labelledby="new-subject-title">
            <div>
              <span className="card-kicker">Novo marcador</span>
              <h2 id="new-subject-title">Adicione uma matéria</h2>
              <p>Use o nome pelo qual você reconhece essa área no seu edital.</p>
            </div>
            <form className="subject-form" onSubmit={submitSubject}>
              <label>Nome da matéria<input autoFocus required maxLength={120} value={name} onChange={(event) => setName(event.target.value)} /></label>
              {createMutation.isError && <p className="form-error" role="alert">{createMutation.error instanceof ApiError ? createMutation.error.message : "Não foi possível criar a matéria."}</p>}
              <div className="form-actions">
                <button className="secondary-button" type="button" onClick={() => { setShowCreate(false); setName(""); }}>Cancelar</button>
                <button className="primary-button" type="submit" disabled={createMutation.isPending}>{createMutation.isPending ? "Adicionando…" : "Adicionar matéria"}</button>
              </div>
            </form>
          </section>
        )}
        <div className="catalog-tabs" role="tablist" aria-label="Estado das matérias">
          <button type="button" role="tab" aria-selected={status === "active"} className={status === "active" ? "selected" : ""} onClick={() => { setStatus("active"); setShowCreate(false); setName(""); }}>Ativas</button>
          <button type="button" role="tab" aria-selected={status === "archived"} className={status === "archived" ? "selected" : ""} onClick={() => { setStatus("archived"); setShowCreate(false); setName(""); }}>Arquivadas</button>
        </div>
        {subjects.isPending && (
          <section className="subjects-loading" aria-busy="true" aria-live="polite">
            {[0, 1, 2].map((item) => <span className="subject-row-skeleton skeleton" key={item} />)}
            <span className="sr-only">Carregando matérias</span>
          </section>
        )}
        {subjects.isError && (
          <section className="subjects-error" role="alert">
            <span className="subject-bookmark error-bookmark" aria-hidden="true"><SubjectsIcon /></span>
            <div><h2>O catálogo não pôde ser aberto</h2><p>Verifique a conexão com sua instalação e tente novamente.</p></div>
            <button className="secondary-button" type="button" onClick={() => void subjects.refetch()}>Tentar novamente</button>
          </section>
        )}
        {subjects.data?.length === 0 && !showCreate && (
          <section className="subjects-empty">
            <span className="empty-bookmark" aria-hidden="true"><SubjectsIcon /></span>
            <h2>{status === "active" ? "Seu catálogo começa aqui" : "Nenhuma matéria arquivada"}</h2>
            <p>{status === "active" ? "Cadastre a primeira matéria para preparar seus próximos ciclos de estudo." : "Quando você arquivar uma matéria, poderá encontrá-la e restaurá-la aqui."}</p>
          </section>
        )}
        {subjects.data && subjects.data.length > 0 && (
          <section className="subject-list" aria-label={status === "active" ? "Matérias ativas" : "Matérias arquivadas"}>
            {subjects.data.map((subject) => <SubjectRow key={subject.id} subject={subject} />)}
          </section>
        )}
      </main>
    </AppShell>
  );
}

function SubjectRow({ subject }: { subject: Subject }) {
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState(false);
  const [name, setName] = useState(subject.name);
  const [confirmingArchive, setConfirmingArchive] = useState(false);
  const updateMutation = useMutation({
    mutationFn: () => updateSubject(subject.id, name),
    onSuccess: (updated) => {
      queryClient.setQueryData<Subject[]>(["subjects", subject.archived ? "archived" : "active"], (current = []) =>
        current.map((item) => item.id === updated.id ? updated : item)
          .sort((first, second) => first.name.localeCompare(second.name, "pt-BR", { sensitivity: "base" }))
      );
      setEditing(false);
    }
  });
  const stateMutation = useMutation({
    mutationFn: () => subject.archived ? restoreSubject(subject.id) : archiveSubject(subject.id),
    onSuccess: (updated) => {
      const sourceStatus = subject.archived ? "archived" : "active";
      const destinationStatus = updated.archived ? "archived" : "active";
      queryClient.setQueryData<Subject[]>(["subjects", sourceStatus], (current = []) =>
        current.filter((item) => item.id !== updated.id)
      );
      queryClient.setQueryData<Subject[]>(["subjects", destinationStatus], (current = []) =>
        [...current.filter((item) => item.id !== updated.id), updated]
          .sort((first, second) => first.name.localeCompare(second.name, "pt-BR", { sensitivity: "base" }))
      );
      setConfirmingArchive(false);
    }
  });

  function submit(event: FormEvent) {
    event.preventDefault();
    updateMutation.mutate();
  }

  return (
    <article className="subject-row">
      <span className="subject-bookmark" aria-hidden="true"><SubjectsIcon /></span>
      {editing ? (
        <form className="subject-edit-form" onSubmit={submit}>
          <label>Editar nome da matéria<input autoFocus required maxLength={120} value={name} onChange={(event) => setName(event.target.value)} /></label>
          {updateMutation.isError && <p className="form-error" role="alert">{updateMutation.error instanceof ApiError ? updateMutation.error.message : "Não foi possível atualizar a matéria."}</p>}
          <div className="row-actions">
            <button className="text-button" type="button" onClick={() => { setEditing(false); setName(subject.name); }}>Cancelar</button>
            <button className="primary-button compact-button" type="submit" disabled={updateMutation.isPending}>{updateMutation.isPending ? "Salvando…" : "Salvar alteração"}</button>
          </div>
        </form>
      ) : (
        <>
          <div className="subject-row-copy">
            <h2>{subject.name}</h2>
            <p>{subject.archived ? "Arquivada — fora dos seletores de estudo" : "Ativa no catálogo"}</p>
          </div>
          <div className="row-actions">
            <Link className="text-button row-link" to={`/materias/${subject.id}/conteudos`}>Ver conteúdos</Link>
            <button className="text-button" type="button" aria-label={`Editar ${subject.name}`} onClick={() => setEditing(true)}>Editar</button>
            {subject.archived ? (
              <button className="secondary-button compact-button" type="button" aria-label={`Restaurar ${subject.name}`} onClick={() => stateMutation.mutate()} disabled={stateMutation.isPending}>{stateMutation.isPending ? "Restaurando…" : "Restaurar"}</button>
            ) : confirmingArchive ? (
              <span className="archive-confirmation">
                <span>Arquivar?</span>
                <button className="text-button" type="button" onClick={() => setConfirmingArchive(false)}>Cancelar</button>
                <button className="danger-button" type="button" aria-label="Confirmar arquivamento" onClick={() => stateMutation.mutate()} disabled={stateMutation.isPending}>{stateMutation.isPending ? "Arquivando…" : "Confirmar"}</button>
              </span>
            ) : (
              <button className="text-button archive-action" type="button" aria-label={`Arquivar ${subject.name}`} onClick={() => setConfirmingArchive(true)}>Arquivar</button>
            )}
          </div>
          {stateMutation.isError && <p className="form-error row-error" role="alert">{stateMutation.error instanceof ApiError ? stateMutation.error.message : "Não foi possível alterar o estado da matéria."}</p>}
        </>
      )}
    </article>
  );
}

function ProtectedContentsPage() {
  const { subjectId = "" } = useParams();
  const queryClient = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [status, setStatus] = useState<ContentStatus>("active");
  const { register, handleSubmit, reset, formState: { errors } } = useForm<{ name: string }>({
    defaultValues: { name: "" }
  });
  const auth = useQuery({ queryKey: ["auth-snapshot"], queryFn: loadAuthSnapshot });
  const subject = useQuery({
    queryKey: ["subject", subjectId],
    queryFn: () => getSubject(subjectId),
    enabled: auth.data?.state === "authenticated" && Boolean(subjectId)
  });
  const contents = useQuery({
    queryKey: ["contents", subjectId, status],
    queryFn: () => listContents(subjectId, status),
    enabled: subject.isSuccess,
    staleTime: 30_000
  });
  const createMutation = useMutation({
    mutationFn: ({ name }: { name: string }) => createContent(subjectId, name),
    onSuccess: (created) => {
      queryClient.setQueryData<StudyContent[]>(["contents", subjectId, "active"], (current = []) =>
        [...current, created].sort((first, second) =>
          first.name.localeCompare(second.name, "pt-BR", { sensitivity: "base" }))
      );
      reset();
      setShowCreate(false);
    }
  });

  if (auth.isPending) {
    return <AppShell authenticated={false}><main className="content"><p>Verificando acesso…</p></main></AppShell>;
  }
  if (auth.isError || auth.data?.state !== "authenticated") {
    return <Navigate to="/" replace />;
  }

  return (
    <AppShell authenticated activeSection="subjects" topbarMeta="Catálogo de conteúdos">
      <main className="content subjects-content content-catalog-content">
        {subject.isPending && (
          <section className="content-context loading-card" aria-busy="true">
            <span className="skeleton skeleton-title" />
            <span className="skeleton skeleton-line" />
          </section>
        )}
        {subject.isError && (
          <section className="subjects-error" role="alert">
            <span className="subject-bookmark error-bookmark" aria-hidden="true"><ContentsIcon /></span>
            <div><h1>Matéria não encontrada</h1><p>Ela pode ter sido removida ou pertencer a outra conta.</p></div>
            <Link className="secondary-button" to="/materias">Voltar para matérias</Link>
          </section>
        )}
        {subject.data && (
          <>
            <Link className="catalog-back-link" to="/materias">← Voltar para matérias</Link>
            <div className="page-heading subjects-heading content-heading">
              <div>
                <span className="eyebrow">Conteúdos da matéria</span>
                <h1>Conteúdos de {subject.data.name}</h1>
                <p>
                  Organize os assuntos que você estuda nesta matéria.
                  {subject.data.archived && <span className="archived-context"> Matéria arquivada.</span>}
                </p>
              </div>
              <button className="primary-button" type="button" onClick={() => setShowCreate(true)} disabled={showCreate}>Novo conteúdo</button>
            </div>
            {showCreate && (
              <section className="subject-composer content-composer" aria-labelledby="new-content-title">
                <div>
                  <span className="card-kicker">Novo tópico</span>
                  <h2 id="new-content-title">Adicione um conteúdo</h2>
                  <p>Use o nome do assunto como ele aparece no seu material ou edital.</p>
                </div>
                <form className="subject-form" onSubmit={handleSubmit((values) => createMutation.mutate(values))}>
                  <label>
                    Nome do conteúdo
                    <input
                      autoFocus
                      maxLength={120}
                      {...register("name", {
                        required: "Informe o nome do conteúdo.",
                        validate: (value) => value.trim().length > 0 || "Informe o nome do conteúdo."
                      })}
                    />
                  </label>
                  {errors.name?.message && <p className="field-error">{errors.name.message}</p>}
                  {createMutation.isError && (
                    <p className="form-error" role="alert">
                      {createMutation.error instanceof ApiError ? createMutation.error.message : "Não foi possível criar o conteúdo."}
                    </p>
                  )}
                  <div className="form-actions">
                    <button className="secondary-button" type="button" onClick={() => { reset(); setShowCreate(false); }}>Cancelar</button>
                    <button className="primary-button" type="submit" disabled={createMutation.isPending}>
                      {createMutation.isPending ? "Adicionando…" : "Adicionar conteúdo"}
                    </button>
                  </div>
                </form>
              </section>
            )}
            <div className="catalog-tabs" role="tablist" aria-label="Estado dos conteúdos">
              <button
                type="button"
                role="tab"
                aria-selected={status === "active"}
                className={status === "active" ? "selected" : ""}
                onClick={() => { setStatus("active"); setShowCreate(false); reset(); }}
              >
                Ativos
              </button>
              <button
                type="button"
                role="tab"
                aria-selected={status === "archived"}
                className={status === "archived" ? "selected" : ""}
                onClick={() => { setStatus("archived"); setShowCreate(false); reset(); }}
              >
                Arquivados
              </button>
            </div>
            {contents.isPending && (
              <section className="subjects-loading" aria-busy="true" aria-live="polite">
                {[0, 1, 2].map((item) => <span className="subject-row-skeleton skeleton" key={item} />)}
                <span className="sr-only">Carregando conteúdos</span>
              </section>
            )}
            {contents.isError && (
              <section className="subjects-error" role="alert">
                <span className="subject-bookmark error-bookmark" aria-hidden="true"><ContentsIcon /></span>
                <div>
                  <h2>Os conteúdos não puderam ser abertos</h2>
                  <p>Verifique a conexão com sua instalação e tente novamente.</p>
                </div>
                <button className="secondary-button" type="button" onClick={() => void contents.refetch()}>Tentar novamente</button>
              </section>
            )}
            {contents.data?.length === 0 && !showCreate && (
              <section className="subjects-empty content-empty">
                <span className="empty-bookmark" aria-hidden="true"><ContentsIcon /></span>
                <h2>{status === "active" ? "Adicione o primeiro conteúdo" : "Nenhum conteúdo arquivado"}</h2>
                <p>
                  {status === "active"
                    ? "Transforme os tópicos desta matéria em unidades claras para seus próximos ciclos de estudo."
                    : "Quando você arquivar um conteúdo, poderá encontrá-lo e restaurá-lo aqui."}
                </p>
                {status === "active" && (
                  <button className="secondary-button" type="button" onClick={() => setShowCreate(true)}>Adicionar conteúdo</button>
                )}
              </section>
            )}
            {contents.data && contents.data.length > 0 && (
              <section className="subject-list content-list" aria-label={status === "active" ? "Conteúdos ativos" : "Conteúdos arquivados"}>
                {contents.data.map((content) => <ContentRow key={content.id} content={content} />)}
              </section>
            )}
          </>
        )}
      </main>
    </AppShell>
  );
}

function ContentRow({ content }: { content: StudyContent }) {
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState(false);
  const [confirmingArchive, setConfirmingArchive] = useState(false);
  const { register, handleSubmit, reset, formState: { errors } } = useForm<{ name: string }>({
    defaultValues: { name: content.name }
  });
  const updateMutation = useMutation({
    mutationFn: ({ name }: { name: string }) => updateContent(content.subjectId, content.id, name),
    onSuccess: (updated) => {
      const status = updated.archived ? "archived" : "active";
      queryClient.setQueryData<StudyContent[]>(["contents", content.subjectId, status], (current = []) =>
        current.map((item) => item.id === updated.id ? updated : item)
          .sort((first, second) => first.name.localeCompare(second.name, "pt-BR", { sensitivity: "base" }))
      );
      reset({ name: updated.name });
      setEditing(false);
    }
  });
  const stateMutation = useMutation({
    mutationFn: () => content.archived
      ? restoreContent(content.subjectId, content.id)
      : archiveContent(content.subjectId, content.id),
    onSuccess: (updated) => {
      const sourceStatus: ContentStatus = content.archived ? "archived" : "active";
      const destinationStatus: ContentStatus = updated.archived ? "archived" : "active";
      queryClient.setQueryData<StudyContent[]>(["contents", content.subjectId, sourceStatus], (current = []) =>
        current.filter((item) => item.id !== updated.id)
      );
      queryClient.setQueryData<StudyContent[]>(["contents", content.subjectId, destinationStatus], (current = []) =>
        [...current.filter((item) => item.id !== updated.id), updated]
          .sort((first, second) => first.name.localeCompare(second.name, "pt-BR", { sensitivity: "base" }))
      );
      setConfirmingArchive(false);
    }
  });

  return (
    <article className="subject-row content-row">
      <span className="subject-bookmark content-bookmark" aria-hidden="true"><ContentsIcon /></span>
      {editing ? (
        <form className="subject-edit-form content-edit-form" onSubmit={handleSubmit((values) => updateMutation.mutate(values))}>
          <label>
            Editar nome do conteúdo
            <input
              autoFocus
              maxLength={120}
              {...register("name", {
                required: "Informe o nome do conteúdo.",
                validate: (value) => value.trim().length > 0 || "Informe o nome do conteúdo."
              })}
            />
          </label>
          {errors.name?.message && <p className="field-error">{errors.name.message}</p>}
          {updateMutation.isError && (
            <p className="form-error" role="alert">
              {updateMutation.error instanceof ApiError ? updateMutation.error.message : "Não foi possível atualizar o conteúdo."}
            </p>
          )}
          <div className="row-actions">
            <button className="text-button" type="button" onClick={() => { reset({ name: content.name }); setEditing(false); }}>Cancelar</button>
            <button className="primary-button compact-button" type="submit" disabled={updateMutation.isPending}>
              {updateMutation.isPending ? "Salvando…" : "Salvar alteração"}
            </button>
          </div>
        </form>
      ) : (
        <>
          <div className="subject-row-copy">
            <h2>{content.name}</h2>
            <p>{content.archived ? "Arquivado — preservado nesta matéria" : "Ativo nesta matéria"}</p>
          </div>
          <div className="row-actions">
            <button className="text-button" type="button" aria-label={`Editar ${content.name}`} onClick={() => setEditing(true)}>Editar</button>
            {content.archived ? (
              <button className="secondary-button compact-button" type="button" aria-label={`Restaurar ${content.name}`} onClick={() => stateMutation.mutate()} disabled={stateMutation.isPending}>
                {stateMutation.isPending ? "Restaurando…" : "Restaurar"}
              </button>
            ) : confirmingArchive ? (
              <span className="archive-confirmation">
                <span>Arquivar?</span>
                <button className="text-button" type="button" onClick={() => setConfirmingArchive(false)}>Cancelar</button>
                <button className="danger-button" type="button" aria-label="Confirmar arquivamento" onClick={() => stateMutation.mutate()} disabled={stateMutation.isPending}>
                  {stateMutation.isPending ? "Arquivando…" : "Confirmar"}
                </button>
              </span>
            ) : (
              <button className="text-button archive-action" type="button" aria-label={`Arquivar ${content.name}`} onClick={() => setConfirmingArchive(true)}>Arquivar</button>
            )}
          </div>
          {stateMutation.isError && (
            <p className="form-error row-error" role="alert">
              {stateMutation.error instanceof ApiError ? stateMutation.error.message : "Não foi possível alterar o estado do conteúdo."}
            </p>
          )}
        </>
      )}
    </article>
  );
}

function formatCivilDate(value: string) {
  const [year, month, day] = value.split("-").map(Number);
  return new Intl.DateTimeFormat("pt-BR", {
    day: "numeric",
    month: "short",
    year: "numeric"
  }).format(new Date(year, month - 1, day));
}

function ReviewCard({ review, onStart, pending, blocked }: {
  review: ReviewOccurrence;
  onStart: (review: ReviewOccurrence) => void;
  pending: boolean;
  blocked: boolean;
}) {
  const timingCopy: Record<ReviewTiming, string> = {
    OVERDUE: "Revisão atrasada",
    TODAY: "Revisar hoje",
    FUTURE: "Revisão futura"
  };
  return (
    <article className={`review-card is-${review.timing.toLowerCase()}`}>
      <span className="review-bookmark" aria-hidden="true">D+{review.intervalDays}</span>
      <div className="review-card-copy">
        <span className="card-kicker">{review.subjectName}</span>
        <h3>{review.contentName}</h3>
        <small>Estudo-base em {formatCivilDate(review.initialStudyDate)}</small>
      </div>
      <div className="review-card-controls">
        <div className="review-card-date">
          <span>{timingCopy[review.timing]}</span>
          <time dateTime={review.dueDate}>{formatCivilDate(review.dueDate)}</time>
        </div>
        {review.timing !== "FUTURE" && (
          <button
            className={review.timing === "TODAY" ? "primary-button review-start-button" : "secondary-button review-start-button"}
            type="button"
            aria-label={`Iniciar revisão de ${review.contentName}`}
            onClick={() => onStart(review)}
            disabled={pending || blocked}
          >
            {pending ? "Iniciando…" : blocked ? "Sessão em andamento" : "Iniciar revisão"}
          </button>
        )}
      </div>
    </article>
  );
}

function ReviewGroup({ title, description, reviews, timing, onStart, pendingId, blocked }: {
  title: string;
  description: string;
  reviews: ReviewOccurrence[];
  timing: ReviewTiming;
  onStart: (review: ReviewOccurrence) => void;
  pendingId?: string;
  blocked: boolean;
}) {
  if (reviews.length === 0) return null;
  return (
    <section className={`review-group is-${timing.toLowerCase()}`} aria-label={title}>
      <header>
        <div>
          <span className="card-kicker">{description}</span>
          <h2>{title}</h2>
        </div>
        <span className="review-group-count">{reviews.length}</span>
      </header>
      <div className="review-list">
        {reviews.map((review) => (
          <ReviewCard
            key={review.occurrenceId}
            review={review}
            onStart={onStart}
            pending={pendingId === review.occurrenceId}
            blocked={blocked}
          />
        ))}
      </div>
    </section>
  );
}

function ProtectedReviewsPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const auth = useQuery({ queryKey: ["auth-snapshot"], queryFn: loadAuthSnapshot });
  const reviewQueue = useQuery({
    queryKey: ["reviews"],
    queryFn: listReviews,
    enabled: auth.data?.state === "authenticated"
  });
  const currentSession = useQuery({
    queryKey: ["study-session", "current"],
    queryFn: loadCurrentStudySession,
    enabled: auth.data?.state === "authenticated",
    staleTime: 5_000
  });
  const startReviewMutation = useMutation({
    mutationFn: (review: ReviewOccurrence) => startReview(review.occurrenceId),
    onSuccess: (started) => {
      queryClient.setQueryData(["study-session", "current"], started);
      navigate("/ciclos");
    }
  });

  if (auth.isPending) {
    return <AppShell authenticated={false}><main className="content"><p>Verificando acesso…</p></main></AppShell>;
  }
  if (auth.isError || auth.data?.state !== "authenticated") {
    return <Navigate to="/" replace />;
  }

  const reviews = reviewQueue.data ?? [];
  const today = reviews.filter((review) => review.timing === "TODAY");
  const overdue = reviews.filter((review) => review.timing === "OVERDUE");
  const future = reviews.filter((review) => review.timing === "FUTURE");
  const reviewActionsBlocked = currentSession.isPending || Boolean(currentSession.data);

  return (
    <AppShell authenticated activeSection="reviews" topbarMeta="Memória em dia">
      <main className="content reviews-content">
        <div className="page-heading reviews-heading">
          <div>
            <span className="eyebrow">Seu horizonte de memória</span>
            <h1>Revisões</h1>
            <p>Retome cada conteúdo no dia certo, preservando a data do primeiro estudo.</p>
          </div>
          <div className="reviews-today-summary" aria-label={`${today.length} revisões para hoje`}>
            <span>Hoje</span>
            <strong>{String(today.length).padStart(2, "0")}</strong>
          </div>
        </div>

        {reviewQueue.isPending && <p className="review-queue-state">Organizando sua fila de revisões…</p>}
        {reviewQueue.isError && (
          <div className="review-queue-state is-error" role="alert">
            <p>Não foi possível consultar suas revisões.</p>
            <button className="secondary-button" type="button" onClick={() => void reviewQueue.refetch()}>Tentar novamente</button>
          </div>
        )}
        {startReviewMutation.isError && (
          <p className="review-start-error" role="alert">
            {startReviewMutation.error instanceof ApiError
              ? startReviewMutation.error.message
              : "Não foi possível iniciar esta revisão."}
          </p>
        )}
        {currentSession.data && reviews.some((review) => review.timing !== "FUTURE") && (
          <p className="review-session-notice">Conclua a sessão em andamento antes de iniciar uma revisão.</p>
        )}
        {!reviewQueue.isPending && !reviewQueue.isError && reviews.length === 0 && (
          <section className="review-empty" aria-label="Fila de revisões vazia">
            <span className="review-empty-bookmark" aria-hidden="true">✓</span>
            <div>
              <h2>Nenhuma revisão agendada</h2>
              <p>Finalize uma sessão com conteúdo e mantenha a opção de revisões marcada.</p>
            </div>
          </section>
        )}
        {!reviewQueue.isPending && !reviewQueue.isError && reviews.length > 0 && (
          <div className="review-groups">
            <ReviewGroup title="Revisões de hoje" description="A prioridade do dia" reviews={today} timing="TODAY" onStart={(review) => startReviewMutation.mutate(review)} pendingId={startReviewMutation.isPending ? startReviewMutation.variables?.occurrenceId : undefined} blocked={reviewActionsBlocked} />
            <ReviewGroup title="Revisões atrasadas" description="Retome sem deslocar o plano" reviews={overdue} timing="OVERDUE" onStart={(review) => startReviewMutation.mutate(review)} pendingId={startReviewMutation.isPending ? startReviewMutation.variables?.occurrenceId : undefined} blocked={reviewActionsBlocked} />
            <ReviewGroup title="Próximas revisões" description="Seu horizonte" reviews={future} timing="FUTURE" onStart={(review) => startReviewMutation.mutate(review)} pendingId={startReviewMutation.isPending ? startReviewMutation.variables?.occurrenceId : undefined} blocked={reviewActionsBlocked} />
          </div>
        )}
      </main>
    </AppShell>
  );
}

function ProtectedAccountPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const auth = useQuery({ queryKey: ["auth-snapshot"], queryFn: loadAuthSnapshot });

  if (auth.isPending) {
    return <AppShell authenticated={false}><main className="content"><p>Verificando acesso…</p></main></AppShell>;
  }
  if (auth.isError || auth.data?.state !== "authenticated") {
    return <Navigate to="/" replace />;
  }

  const snapshot = auth.data;
  return (
    <AppShell authenticated activeSection="account" topbarMeta="Identidade e acesso">
      <main className="content auth-content">
        <div className="page-heading">
          <div>
            <span className="eyebrow">Sua instalação, suas regras</span>
            <h1>Conta e segurança</h1>
            <p>Consulte sua identidade local e mantenha sua credencial atualizada.</p>
          </div>
          <div className="heading-bookmark" aria-hidden="true">02</div>
        </div>
        <AuthenticatedCard
          snapshot={snapshot}
          onLogout={() => {
            queryClient.setQueryData<AuthSnapshot>(["auth-snapshot"], {
              state: "login",
              registrationEnabled: snapshot.registrationEnabled
            });
            navigate("/", { replace: true, state: { notice: "Sessão encerrada com segurança." } });
          }}
        />
      </main>
    </AppShell>
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
        {snapshot?.state === "authenticated" && <Navigate to="/materias" replace />}
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
        <Route path="/materias" element={<ProtectedSubjectsPage />} />
        <Route path="/materias/:subjectId/conteudos" element={<ProtectedContentsPage />} />
        <Route path="/ciclos" element={<ProtectedStudyCyclesPage />} />
        <Route path="/revisoes" element={<ProtectedReviewsPage />} />
        <Route path="/conta" element={<ProtectedAccountPage />} />
        <Route path="/redefinir-senha" element={<PasswordResetPage />} />
      </Routes>
    </BrowserRouter>
  );
}
