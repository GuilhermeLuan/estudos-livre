# Issue 8 — Ativar, trocar e retomar ciclos

## Objetivo

Permitir que cada usuário escolha inequivocamente o ciclo que recebe seus estudos, troque de foco sem perder a volta incompleta e retome exatamente a mesma posição ao reativar um ciclo.

## Contrato público

- `POST /api/study-cycles/{id}/activate` ativa um ciclo pertencente ao usuário autenticado.
- A resposta é o ciclo ativado, incluindo `currentRun` com identificador, número da volta, posição atual e instante de início.
- A listagem e a consulta de ciclos também expõem `currentRun`, inclusive quando o ciclo está inativo, para que a interface possa distinguir `Ativar` de `Retomar`.
- Ciclos sem etapas não podem ser ativados e retornam `409 Conflict` em `ProblemDetail`.
- Recursos de outro usuário continuam indistinguíveis de recursos inexistentes e retornam `404`.

## Modelo e invariantes

- `study_cycle_run` representa uma volta persistida de um ciclo.
- A primeira ativação cria a volta 1 na posição 1.
- Uma troca apenas altera o status dos ciclos; a volta incompleta do ciclo anterior permanece aberta.
- Uma reativação reutiliza a volta aberta, sem alterar seu identificador, número ou posição.
- A ativação trava a linha do proprietário antes da troca. Isso serializa ativações concorrentes do mesmo usuário.
- Um índice parcial único em `study_cycle(owner_id) WHERE status = 'ACTIVE'` mantém a regra de no máximo um ciclo ativo também no banco.
- A desativação do ciclo anterior, a criação ou reutilização da volta e a ativação do alvo pertencem à mesma transação.

## Fluxo de dados

1. O controller resolve o usuário autenticado e delega a ativação.
2. O service valida propriedade e presença de etapas.
3. O repository trava o proprietário, desativa qualquer ciclo ativo, encontra ou cria a volta aberta e ativa o alvo.
4. O service recarrega o ciclo e sua volta para formar a resposta.
5. O frontend substitui o ciclo retornado no cache e marca todos os demais como inativos, refletindo a troca sem recarregar a aplicação.

## Interface

### Direção

- **Pessoa:** concurseiro alternando planejamentos sem querer perder o ponto de estudo.
- **Tarefa:** identificar o ciclo que recebe o próximo estudo e trocar ou retomar o foco com segurança.
- **Sensação:** calma de caderno organizado, com estado inequívoco e sem aparência de painel genérico.

### Sistema aplicado

- **Domínio:** ciclos, voltas, etapas, marcador, foco, retomada.
- **Mundo de cor:** papel, tinta, fichário verde, marca-texto amarelo e terracota de alerta.
- **Assinatura:** o marcador aparece no card focal, no selo ativo, na posição da volta, na ação principal e no ciclo selecionado.
- **Padrões rejeitados:** grade uniforme de cards; status indicado só por cor; modal obrigatório para toda troca.
- **Composição:** um card focal apresenta o ciclo ativo; os demais ciclos formam uma lista secundária com ações `Ativar` ou `Retomar`.

### Checkpoint de componentes

- **Hierarchy:** o ciclo ativo é o único focal point, vencendo por posição, espaço e contraste.
- **Palette:** apenas os tokens existentes de papel, fichário, marcador e atraso.
- **Depth:** sombras sutis para o card focal; divisões quietas na lista.
- **Surfaces:** `paper` no canvas, `raised` no foco e `inset` nos controles/estados secundários.
- **Typography:** serif editorial no nome/volta; sans-serif nos controles e metadados.
- **Spacing:** base de 4 px, com 24 px no card focal e alvos interativos mínimos de 44 px.

## TDD em fatias verticais

1. **RED → GREEN:** a primeira ativação cria a volta 1, posição 1, e retorna o ciclo ativo.
2. **RED → GREEN:** ativar outro ciclo desativa o anterior na mesma operação.
3. **RED → GREEN:** reativar o primeiro ciclo reutiliza a mesma volta e posição.
4. **RED → GREEN:** um usuário não ativa nem observa a volta de outro.
5. **RED → GREEN:** ativações concorrentes no PostgreSQL terminam com exatamente um ciclo ativo.
6. **RED → GREEN:** a UI mostra o ciclo ativo e troca o foco pela API sem recarregar.
7. **Refactor:** remover duplicação somente com todas as fatias verdes.

## Validação

- Testes de integração atravessam HTTP, segurança, service, repository, Flyway e PostgreSQL Testcontainers.
- Testes de componente verificam texto, ação, estado do cache e erro observável.
- `./mvnw test`, `npm test`, `npm run build` e verificação visual em larguras desktop e mobile.

