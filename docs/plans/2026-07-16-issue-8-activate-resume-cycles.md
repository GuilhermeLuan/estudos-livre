# Issue 8 — Ativar, trocar e retomar ciclos

## Objetivo

Permitir que cada usuário escolha inequivocamente o ciclo que recebe seus estudos, navegue livremente por todas as atividades e troque de foco pausando ou encerrando a volta atual.

## Contrato público

- `POST /api/study-cycles/{id}/activate` ativa um ciclo pertencente ao usuário autenticado. Ao trocar de ciclo, recebe `currentRunAction` com `PAUSE` ou `ABANDON`.
- A resposta é o ciclo ativado, incluindo `currentRun` com identificador, número, estado e instante de início, sem cursor de posição atual.
- A listagem e a consulta de ciclos também expõem `currentRun`, inclusive quando o ciclo está inativo, para que a interface possa distinguir `Ativar` de `Retomar`.
- Ciclos sem etapas não podem ser ativados e retornam `409 Conflict` em `ProblemDetail`.
- Trocar de ciclo sem escolher o destino da volta atual retorna `409 Conflict`.
- Ciclos em rascunho, ativos ou inativos são editáveis; um ciclo ativo não pode ser salvo vazio.
- Recursos de outro usuário continuam indistinguíveis de recursos inexistentes e retornam `404`.

## Modelo e invariantes

- `study_cycle_run` representa uma volta persistida de um ciclo.
- A primeira ativação cria a volta 1 em `IN_PROGRESS`, sem etapa atual obrigatória.
- Pausar uma troca preserva a volta como `PAUSED`; reativá-la reutiliza o identificador e o número.
- Encerrar uma troca marca a volta como `ABANDONED`; reativar o ciclo cria a volta seguinte.
- A ordem das atividades orienta a sugestão visual, mas qualquer atividade pode receber estudo.
- A ativação trava a linha do proprietário antes da troca. Isso serializa ativações concorrentes do mesmo usuário.
- Um índice parcial único em `study_cycle(owner_id) WHERE status = 'ACTIVE'` mantém a regra de no máximo um ciclo ativo também no banco.
- A desativação do ciclo anterior, a criação ou reutilização da volta e a ativação do alvo pertencem à mesma transação.

## Fluxo de dados

1. O controller resolve o usuário autenticado e delega a ativação.
2. O service valida propriedade e presença de etapas.
3. O repository trava o proprietário, pausa ou abandona a volta atual, desativa o ciclo anterior, retoma ou cria a volta do alvo e o ativa.
4. O service recarrega o ciclo e sua volta para formar a resposta.
5. O frontend substitui o ciclo retornado no cache e marca todos os demais como inativos, refletindo a troca sem recarregar a aplicação.

## Interface

### Direção

- **Pessoa:** concurseiro escolhendo livremente o que estudar e alternando planejamentos sem perder histórico.
- **Tarefa:** visualizar o fluxo completo, editar o planejamento e trocar de ciclo com uma decisão explícita.
- **Sensação:** calma de caderno organizado, com estado inequívoco e sem aparência de painel genérico.

### Sistema aplicado

- **Domínio:** ciclos, voltas, etapas, marcador, foco, retomada.
- **Mundo de cor:** papel, tinta, fichário verde, marca-texto amarelo e terracota de alerta.
- **Assinatura:** o marcador aparece no card focal e num mapa vertical das atividades, indicando sugestão sem bloquear escolhas.
- **Padrões rejeitados:** grade uniforme de cards; status indicado só por cor; uma única “etapa atual” obrigatória.
- **Composição:** um card focal apresenta todo o fluxo ativo; os demais ciclos formam uma lista secundária e a troca usa confirmação somente quando há outro ciclo ativo.

### Checkpoint de componentes

- **Hierarchy:** o ciclo ativo é o único focal point, vencendo por posição, espaço e contraste.
- **Palette:** apenas os tokens existentes de papel, fichário, marcador e atraso.
- **Depth:** sombras sutis para o card focal; divisões quietas na lista.
- **Surfaces:** `paper` no canvas, `raised` no foco e `inset` nos controles/estados secundários.
- **Typography:** serif editorial no nome/volta; sans-serif nos controles e metadados.
- **Spacing:** base de 4 px, com 24 px no card focal e alvos interativos mínimos de 44 px.

## TDD em fatias verticais

1. **RED → GREEN:** a primeira ativação cria uma volta sem cursor e retorna `IN_PROGRESS`.
2. **RED → GREEN:** ciclos ativos e inativos podem ser editados imediatamente.
3. **RED → GREEN:** o ciclo ativo não pode ser salvo sem atividades.
4. **RED → GREEN:** trocar exige escolher o destino da volta atual.
5. **RED → GREEN:** pausar preserva e retoma a mesma volta; abandonar cria a volta seguinte no retorno.
6. **RED → GREEN:** ativações concorrentes no PostgreSQL terminam com exatamente um ciclo ativo.
7. **RED → GREEN:** a UI mostra todo o fluxo, permite edição e confirma a troca sem recarregar.
8. **Refactor:** remover duplicação somente com todas as fatias verdes.

## Validação

- Testes de integração atravessam HTTP, segurança, service, repository, Flyway e PostgreSQL Testcontainers.
- Testes de componente verificam texto, ação, estado do cache e erro observável.
- `mvn -pl backend test`, `npm test`, `npm run build` e verificação visual em larguras desktop e mobile.
