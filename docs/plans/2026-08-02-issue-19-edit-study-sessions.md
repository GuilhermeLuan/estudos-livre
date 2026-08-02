# Issue #19 — Editar sessões e reconstruir projeções

## Objetivo

Permitir que o usuário corrija ou exclua qualquer sessão concluída sem perder a
coerência dos dados derivados. A `StudySession` continua sendo a fonte da
verdade; métricas de tempo e exercícios são consultadas a partir dela, enquanto
créditos e estados das voltas são projeções reconstruídas em ordem cronológica.

## Alternativas consideradas

1. **Ajustar somente a diferença do registro editado.** É a menor alteração,
   mas falha quando a correção muda matéria, data ou a volta que recebe as
   sessões posteriores.
2. **Reconstruir todas as projeções do usuário.** É simples de explicar, porém
   bloqueia ciclos não relacionados e aumenta desnecessariamente o custo e o
   risco transacional.
3. **Reconstruir o segmento afetado do mesmo ciclo.** É a opção adotada: remove
   os créditos do trecho, zera suas etapas e reaplica as sessões elegíveis pela
   data e pelo identificador, preservando os snapshots das voltas e isolando os
   demais ciclos.

## Contrato público

### Editar sessão concluída

`PUT /api/study-sessions/{id}` recebe:

- `expectedVersion`: inteiro não negativo;
- `startedAtLocal`: data e hora civil no fuso cadastrado do usuário;
- `effectiveSeconds`: inteiro positivo;
- `subjectId`: matéria ativa pertencente ao usuário;
- `contentId`: conteúdo ativo opcional da matéria;
- `notes`: texto opcional de até 4.000 caracteres;
- `questionsAttempted` e `questionsCorrect`: o mesmo par opcional já usado no
  encerramento e na correção de exercícios.

A resposta é o `StudySessionResponse` existente, já com a nova versão e os
créditos reconstruídos. Origem, segmentos do cronômetro e vínculos históricos de
revisão não são editáveis. O instante de término acompanha o novo início e a
duração efetiva. Nas sessões cronometradas, os segmentos preservam a duração
medida original; em uma sessão `MANUAL`, sem cronômetro, `measuredSeconds`
continua sendo por definição a própria duração efetiva corrigida.

O endpoint legado `PUT /api/study-sessions/{id}/exercise-result` passa a exigir
`expectedVersion`, incrementa a versão e usa o mesmo conflito 409. A interface
nova deixa de usá-lo, mas clientes antigos não ganham uma rota capaz de contornar
a concorrência otimista.

### Excluir sessão concluída

`DELETE /api/study-sessions/{id}` recebe `{ "expectedVersion": n }` e responde
`204`. A exclusão é física no escopo do MVP. A FK já existente em
`review_plan.source_session_id` usa `ON DELETE SET NULL`, portanto o plano e suas
ocorrências conservam datas, estados e agenda.

Nos dois endpoints, sessão inexistente ou de outro proprietário responde `404`.
Sessão não concluída ou versão obsoleta responde `ProblemDetail 409`, com tipo
`study-session-conflict`. Validação de campos responde `400`.

### Métricas derivadas

`GET /api/study-sessions/summary` devolve duração efetiva, questões, acertos e
taxa por matéria e por conteúdo. A taxa é ausente quando não há questões. O
endpoint permite observar que edição e exclusão recalculam tanto tempo quanto
desempenho sem manter uma tabela agregada. O endpoint anterior
`/exercise-summary` pode continuar disponível por compatibilidade.

A resposta tem a forma `{ subjects, contents }`. Cada matéria expõe
`subject: { id, name }`, `effectiveSeconds`, `questionsAttempted`,
`questionsCorrect` e `accuracyPercentage`; cada conteúdo expõe também
`content: { id, name }` e a referência da matéria. `accuracyPercentage` é `null`
quando o grupo não possui questões.

## Persistência e reconstrução

- Uma migração acrescenta à sessão a associação interna com o ciclo/volta cuja
  projeção recebeu (ou poderia receber) seu crédito. Ela é preenchida mesmo se a
  matéria estiver fora do snapshot e não houver crédito, permitindo que uma
  correção posterior passe a participar da mesma projeção.
- Para dados existentes, o vínculo é recuperado do contexto `CYCLE` ou dos
  créditos já persistidos. Registros antigos sem informação suficiente
  continuam sem projeção de ciclo, mas suas métricas permanecem corretas.
- A edição avança `version` no mesmo `UPDATE ... WHERE version =
  :expectedVersion`; a exclusão usa a mesma guarda. A operação inteira, inclusive
  reconstrução, ocorre em uma única transação PostgreSQL.
- A reconstrução bloqueia as voltas do ciclo afetado, remove seus
  `study_session_credit`, zera `credited_seconds` e reaplica as sessões concluídas
  do segmento em `started_at, id`.
- Cada sessão distribui tempo somente entre etapas da mesma matéria e dentro de
  uma volta. Excedente que conclui uma volta não atravessa para a próxima; a
  sessão cronologicamente seguinte inicia a volta seguinte.
- Linhas de `study_cycle_run` e `study_cycle_run_stage` existentes nunca são
  apagadas nem recebem um snapshot novo. Voltas novamente alcançadas podem
  voltar a `COMPLETED`; a primeira incompleta fica `IN_PROGRESS` ou `PAUSED`
  conforme o ciclo; voltas posteriores não alcançadas ficam `ABANDONED` com o
  snapshot intacto. Se todas as voltas existentes forem concluídas, cria-se
  somente a próxima volta necessária com o planejamento atual.
- Fronteiras que já eram `ABANDONED` por troca explícita de ciclo delimitam
  segmentos e não são atravessadas pelo recálculo.
- Consultas de métricas por matéria/conteúdo continuam derivadas das sessões e
  dos resultados de exercícios, portanto refletem a edição/exclusão sem tabela
  agregada adicional.

## Invariantes

1. Nenhuma operação observa ou altera sessão, matéria, conteúdo ou ciclo de
   outro usuário.
2. Há no máximo uma volta aberta por ciclo depois da reconstrução.
3. A soma dos créditos de uma etapa é igual ao seu `credited_seconds` e nunca
   supera `target_seconds`.
4. Uma sessão nunca credita outra matéria nem duas voltas.
5. Snapshots históricos não são substituídos pelo planejamento atual.
6. Planos e ocorrências de revisão não são apagados, reagendados ou reatribuídos
   por edição/exclusão da sessão.
7. Uma versão obsoleta não produz alteração parcial.

## Fatias TDD verticais

1. **Tracer bullet de edição.** RED: editar via HTTP data, duração, matéria,
   conteúdo, anotações e exercícios e observar o novo histórico e `/summary`.
   GREEN: DTO, endpoint, guarda de versão e atualização mínima.
2. **Reconstrução de uma volta.** RED: reduzir/mudar a sessão e observar créditos
   antigos removidos e redistribuídos pela API de sessão/ciclo. GREEN: associação
   de projeção e serviço profundo de reconstrução.
3. **Múltiplas voltas.** RED: criar duas voltas, alterar uma sessão antiga e
   observar progresso/estado posteriores e snapshots preservados. GREEN:
   reaplicação cronológica e transições mínimas.
4. **Exclusão e revisão independente.** RED: excluir após confirmação de versão,
   observar `204`, métricas/créditos reconstruídos e plano de revisão intacto.
   GREEN: exclusão transacional e reconstrução pelo estado capturado antes do
   `DELETE`.
5. **Concorrência e propriedade.** RED/GREEN: versão obsoleta retorna 409 sem
   efeitos; outro usuário recebe 404; sessão aberta não pode ser alterada.
6. **Interface.** RED: pelo histórico, abrir ficha preenchida, editar todos os
   campos, confirmar exclusão e observar dados atualizados. GREEN: cliente HTTP,
   diálogo acessível e invalidação de histórico, resumo, ciclos e voltas.
7. **Refatoração e regressão.** Remover duplicação do tratamento de exercícios e
   da distribuição sem alterar interfaces; manter a suíte verde a cada passo.

## Ownership e ordem de integração

- Backend: migração, `studysession`, reconstrução coordenada com `studycycle` e
  integrações PostgreSQL.
- Frontend: `study-session-api.ts`, histórico/diálogos em `App.tsx`, estilos e
  testes React.
- O contrato acima fica congelado antes dos dois escritores. A integração final,
  revisão de isolamento/transações e correções permanecem com o orquestrador.

## Verificação

- Demonstrar cada novo comportamento em RED antes do código mínimo que o torna
  GREEN.
- Backend focado: `mvn -pl backend -Dtest=StudySessionMaintenanceIntegrationTest test`.
- Backend completo: `mvn test`.
- Frontend focado: `npm test -- --run App.test.tsx` em `frontend/`.
- Frontend completo e build: `npm test -- --run` e `npm run build`.
- Inspeção do diff e revisão independente dos contratos, isolamento, versão,
  transação, snapshots, invalidação de cache e acessibilidade da confirmação.

## Compatibilidade e rollback

A migração é somente aditiva; clientes antigos continuam usando os endpoints
existentes. O rollback operacional consiste em não expor as novas ações: dados
anteriores seguem legíveis e as colunas novas são opcionais. A reconstrução não
apaga snapshots, mas é uma operação transacional de maior alcance e por isso deve
ser validada em PostgreSQL real antes de disponibilização.
