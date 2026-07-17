# Issue 11 — Cronômetro persistente

## Objetivo

Entregar uma sessão de estudo cronometrada cuja fonte de verdade seja o backend: o estudante inicia pela etapa atual do ciclo ou como sessão livre, pode pausar e retomar, recarrega a aplicação sem perder o tempo líquido e nunca mantém dois cronômetros abertos ao mesmo tempo.

## Limites deste incremento

- Esta issue cria e controla sessões nos estados `ACTIVE` e `PAUSED`.
- A finalização, a correção da duração efetiva e o crédito definitivo no ciclo permanecem na issue 12.
- Enquanto ainda não existem créditos finalizados, a etapa atual da volta é a primeira etapa e seu progresso nesta entrega é o tempo líquido da sessão de origem `CYCLE` vinculada a ela.
- Conteúdo continua opcional e limitado a um item ativo da matéria escolhida.

## Modelo e invariantes

Criar a feature `studysession` e a migração `V11__create_study_sessions.sql` com:

- `study_session`: proprietário, origem (`CYCLE` ou `FREE`), estado, matéria, conteúdo opcional, referências opcionais para ciclo/volta/etapa e instante inicial;
- `study_session_timer_segment`: cada intervalo efetivamente cronometrado, com início e fim; um segmento aberto representa o período `ACTIVE` atual;
- índice parcial único por `owner_id` para estados `ACTIVE` ou `PAUSED`, garantindo a exclusividade inclusive sob concorrência;
- índice parcial único para no máximo um segmento aberto por sessão;
- checks que tornam referências de ciclo obrigatórias apenas para origem `CYCLE` e mantêm estado/segmento coerentes.

O tempo medido será calculado no PostgreSQL como a soma dos segmentos fechados mais o intervalo entre o início do segmento aberto e `CURRENT_TIMESTAMP`. Assim, pausas não contam e a resposta traz `measuredSeconds` e `serverNow` para o frontend projetar o relógio sem se tornar a fonte oficial.

## Contrato HTTP

- `GET /api/study-sessions/current`: sessão aberta do usuário ou `204` quando não existir;
- `POST /api/study-sessions`: inicia uma sessão `CYCLE` com `cycleId` e conteúdo opcional, ou `FREE` com `subjectId` e conteúdo opcional;
- `POST /api/study-sessions/{id}/pause`: fecha o segmento ativo e devolve a sessão pausada;
- `POST /api/study-sessions/{id}/resume`: abre um novo segmento e devolve a sessão ativa.

A resposta inclui identidade da sessão, origem/estado, matéria e conteúdo, referências de ciclo/volta/etapa, meta da etapa, início, tempo medido e horário do servidor. Recursos de outro usuário retornam `404`; sessão concorrente ou transição inválida retorna `409` em `ProblemDetail`.

## Sequência TDD

### 1. Início e recuperação

1. Escrever testes de integração HTTP para iniciar pelo ciclo, derivar a matéria da primeira etapa, aceitar zero ou um conteúdo ativo da mesma matéria e recuperar a mesma sessão em `GET /current`.
2. Adicionar testes para sessão livre e para rejeitar ciclo inativo, matéria/conteúdo de outro usuário, conteúdo de outra matéria e conteúdo arquivado.
3. Implementar a menor fatia vertical: migração, modelos/DTOs, repository, service, controller e tratamento de erros.

### 2. Pausa, retomada e tempo líquido

1. Escrever teste que envelhece o primeiro segmento no banco, pausa, mantém o relógio estável durante o intervalo pausado, retoma e soma somente os dois segmentos ativos.
2. Escrever testes de transições inválidas e propriedade.
3. Implementar fechamento/abertura transacional de segmentos e cálculo de `measuredSeconds` no repository.

### 3. Exclusividade concorrente

1. Escrever teste com largada simultânea de duas requisições do mesmo usuário.
2. Demonstrar que somente uma retorna sucesso e que o banco conserva uma única sessão aberta.
3. Traduzir a violação do índice parcial em conflito de domínio sem depender de uma checagem prévia vulnerável a corrida.

### 4. Interface do ciclo e sessão livre

1. Escrever testes de componente para:
   - foco atual com matéria, meta, progresso e restante;
   - abertura de sessão do ciclo com matéria bloqueada e conteúdo opcional filtrado;
   - sessão livre com matéria selecionável e conteúdo dependente;
   - relógio ativo, pausa, retomada e recuperação usando `measuredSeconds` + `serverNow`.
2. Implementar `study-session-api.ts` e integrar as queries/mutations à página de ciclos.
3. Invalidar/atualizar o cache da sessão após cada transição; o contador local só interpola a resposta do servidor e é resincronizado a cada mutação/refetch.

## Direção de interface

- Intenção: deixar inequívoca a próxima matéria e tornar o cronômetro o foco da tela sem apagar o mapa da volta.
- Hierarquia: etapa atual e tempo restante como foco; ação `Iniciar etapa` como comando primário; `Sessão livre` como alternativa secundária.
- Paleta: verde do fichário para sessão ativa, amarelo de marcador para etapa atual e papel neutro para pausa.
- Assinatura: um cartão de foco com marcador numerado e uma régua de progresso; durante a sessão, ele se transforma em uma mesa de estudo compacta com relógio tabular grande.
- Responsividade: no celular, matéria/meta/relógio empilham, ações ocupam largura útil e nenhum seletor excede o viewport.

## Verificação

- Testes direcionados da nova classe de integração no ciclo RED → GREEN.
- Testes de frontend RED → GREEN e build TypeScript/Vite.
- Suíte completa do backend com PostgreSQL/Testcontainers.
- Suíte completa do frontend.
- QA visual e funcional no navegador em desktop e viewport móvel, incluindo recarga, pausa, retomada e ausência de overflow.
