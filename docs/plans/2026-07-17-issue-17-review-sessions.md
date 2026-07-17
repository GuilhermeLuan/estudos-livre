# Issue #17 — Executar revisões como sessões de estudo

## Objetivo

Permitir que uma ocorrência de revisão vencida ou prevista para hoje seja executada com o mesmo cronômetro e fluxo de encerramento das demais sessões. A sessão nasce com assunto e conteúdo derivados do plano, não pode ser reatribuída pelo cliente e, ao terminar, consolida ocorrências atrasadas sem deslocar nenhuma data futura.

## Contrato público

### Iniciar revisão

`POST /api/reviews/{occurrenceId}/start`

- exige autenticação;
- aceita apenas uma ocorrência agendada pertencente ao usuário e com vencimento até a data civil de hoje no fuso do usuário;
- usa a ocorrência solicitada para identificar o plano, mas seleciona a ocorrência agendada vencida mais recente desse plano;
- cria uma `study_session` com `origin = REVIEW`, assunto e conteúdo do plano e cronômetro iniciado;
- responde `201` com o mesmo `StudySessionResponse` usado pelas demais sessões;
- não aceita assunto, conteúdo, ciclo ou etapa no corpo da requisição;
- devolve conflito se a revisão já estiver em execução ou já tiver sido resolvida.

O endpoint existente `POST /api/study-sessions/{id}/finish` encerra a revisão. Pausa, retomada e consulta da sessão atual continuam usando os endpoints de sessão já existentes.

### Consultar fila

`GET /api/reviews` continua retornando apenas ocorrências `SCHEDULED`. Depois do encerramento, ocorrências concluídas ou puladas desaparecem da fila, enquanto as futuras permanecem com as datas originais.

## Modelo e invariantes

Uma migração `V16` deverá:

- permitir `REVIEW` no `study_session.origin`;
- adicionar `study_session.review_occurrence_id`, com chave estrangeira e unicidade;
- garantir que apenas sessões `REVIEW` tenham ocorrência de revisão e que elas não carreguem o contexto fixo de ciclo usado por sessões `CYCLE`;
- ampliar o estado de `review_occurrence` para `SCHEDULED`, `COMPLETED` e `SKIPPED`;
- adicionar `resolved_at` e `completed_session_id`;
- garantir por restrições que:
  - `SCHEDULED` não possua resolução nem sessão concluída;
  - `COMPLETED` possua resolução e sessão concluída;
  - `SKIPPED` possua resolução e não possua sessão concluída.

Invariantes de negócio:

1. Uma ocorrência pode originar no máximo uma sessão.
2. Assunto e conteúdo vêm exclusivamente do plano.
3. A ocorrência vencida mais recente é a concluída; ocorrências agendadas anteriores do mesmo plano tornam-se `SKIPPED`.
4. Ocorrências futuras mantêm status e data.
5. Repetição ou concorrência no encerramento não duplica conclusão nem créditos.
6. A duração efetiva é distribuída nas etapas do ciclo ativo no instante do encerramento, usando o distribuidor já empregado pelas sessões do ciclo.
7. A sessão de revisão não fica presa a um ciclo ou execução capturados no início; se não houver ciclo ativo ao encerrar, a revisão ainda é concluída sem crédito de ciclo.

## Sequência TDD

Cada fatia seguirá RED → GREEN → refatoração com a suíte verde.

1. **Fluxo vertical mínimo**
   - teste de integração inicia uma ocorrência de hoje;
   - verifica `origin = REVIEW`, assunto/conteúdo fixos e cronômetro ativo;
   - pausa, retoma e encerra pelo fluxo público existente;
   - implementação mínima de migração, repositório, serviço e endpoint.
2. **Crédito no ciclo ativo**
   - teste cria ciclo ativo, conclui uma revisão com duração efetiva conhecida e verifica os créditos por etapa;
   - reutilizar a distribuição de créditos existente, sem duplicar a regra.
3. **Consolidação de atrasadas**
   - teste prepara várias ocorrências vencidas e uma futura;
   - iniciar por uma vencida antiga deve selecionar a mais recente;
   - encerrar deve concluir a mais recente, pular as anteriores e preservar a futura sem alterar sua data.
4. **Idempotência e concorrência**
   - testes concorrentes de início e encerramento;
   - provar uma única sessão, uma única conclusão e um único conjunto de créditos.
5. **Limites de autorização e disponibilidade**
   - rejeitar ocorrência de outro usuário, futura, inexistente ou já resolvida.
6. **Frontend**
   - teste de componente exige ação “Iniciar revisão” apenas em hoje/atrasadas;
   - iniciar chama a API, registra a sessão atual e leva à mesa de estudos;
   - sessão `REVIEW` usa o cronômetro e o diálogo de encerramento existentes;
   - ao encerrar, invalidar a fila de revisões.
7. **Gate final**
   - testes completos de frontend e backend;
   - build de produção;
   - inspeção desktop e mobile dos estados hoje, atrasada, carregando, erro e sessão em andamento.

## Direção de interface

A fila mantém a linguagem de caderno e fichário já definida na #16. A ação não vira um painel genérico de cartões:

- ocorrências de hoje recebem botão primário verde-fichário;
- atrasadas usam ação mais contida, acompanhada do marcador terracota;
- futuras não exibem ação;
- o marcador lateral continua sendo a assinatura visual da página;
- enquanto já houver uma sessão ativa, as ações de revisão ficam indisponíveis e explicam que é preciso concluí-la primeiro;
- em telas de até 760 px, cada ação ocupa a largura útil e conserva alvo mínimo de 44 px;
- a mesa de estudos identifica a origem como “Revisão”, mantendo assunto e conteúdo somente para leitura.

## Verificação de aceite

- iniciar revisão cria sessão `REVIEW` com assunto/conteúdo corretos;
- cronômetro, pausa, retomada e encerramento funcionam pelo fluxo existente;
- duração efetiva credita o ciclo ativo uma única vez;
- vencidas antigas tornam-se `SKIPPED`, a vencida mais recente `COMPLETED` e futuras seguem `SCHEDULED` sem mudança de data;
- nenhuma ocorrência pode ser executada duas vezes, inclusive sob concorrência;
- fila, navegação e mesa funcionam sem overflow em desktop e mobile;
- exclusão/edição da sessão original do plano continua sem afetar o plano de revisão.
