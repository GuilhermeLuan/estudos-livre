# Issue #16 — Criar e consultar planos de revisão

## Objetivo

Ao finalizar uma sessão associada a um conteúdo, permitir que o estudante confirme um plano de revisão já habilitado por padrão. O plano conserva a data local do estudo inicial e gera uma fila consultável com revisões de hoje, atrasadas e futuras, sem ser alterado por novas finalizações ou pela sessão que o originou.

## Contratos públicos

### Finalização da sessão

`POST /api/study-sessions/{id}/finish` passa a aceitar `scheduleReviews`, booleano opcional. A interface envia `true` por padrão quando a sessão possui conteúdo e não exibe a opção quando não possui.

Quando `scheduleReviews = true` e a sessão possui conteúdo, a operação garante um plano ativo para o par usuário+conteúdo. Uma repetição idempotente da mesma finalização também garante o plano, para que uma resposta perdida possa ser repetida com segurança.

### Consulta da fila

`GET /api/reviews` devolve as ocorrências ativas do usuário, ordenadas por data e agrupáveis por `TODAY`, `OVERDUE` e `FUTURE`. Cada item expõe:

- identificadores do plano e da ocorrência;
- matéria e conteúdo;
- data local do estudo inicial e data prevista;
- intervalo em dias e situação temporal.

O backend classifica as ocorrências usando a data corrente no fuso cadastrado do usuário. Datas são valores civis (`LocalDate`/`DATE`) e o frontend não as converte por UTC.

## Modelo e invariantes

- `review_plan` guarda proprietário, matéria e conteúdo, sessão de origem opcional, data local inicial e estado `ACTIVE`.
- Um índice parcial único permite no máximo um plano `ACTIVE` por usuário+conteúdo.
- A referência à sessão usa `ON DELETE SET NULL`; as datas e o contexto do conteúdo pertencem ao plano e não são recalculados.
- `review_occurrence` guarda plano, intervalo, data prevista e estado `SCHEDULED`; o par plano+intervalo é único.
- Os intervalos canônicos são 1, 7, 30, 60, 90 e 120 dias, sempre somados à data local inicial.
- Inserções usam as restrições do banco como última defesa contra finalizações repetidas ou concorrentes; após conflito de unicidade, a leitura retorna o plano já existente.
- A fila contém somente ocorrências `SCHEDULED` de planos ativos e nunca mistura proprietários.

## Sequência TDD

1. **Intervalos puros (RED → GREEN)**
   - testar que uma data inicial gera exatamente `+1`, `+7`, `+30`, `+60`, `+90` e `+120` dias, inclusive atravessando mês e ano;
   - implementar um calendário de revisão pequeno, sem HTTP ou banco.
2. **Tracer bullet de criação (RED → GREEN)**
   - finalizar pela API uma sessão com conteúdo e `scheduleReviews = true`;
   - consultar `/api/reviews` e observar as seis ocorrências ancoradas à data local da sessão;
   - criar migração, repositório, serviço, DTOs e endpoint mínimos para passar.
3. **Opt-in, unicidade e concorrência (RED → GREEN)**
   - provar que `false` e sessão sem conteúdo não criam plano;
   - repetir a finalização e finalizar sessões concorrentes do mesmo conteúdo, observando um plano e seis ocorrências;
   - preservar a idempotência da finalização e traduzir concorrência pela restrição única, sem duplicação.
4. **Consulta temporal e isolamento (RED → GREEN)**
   - provar classificação e ordenação de atrasadas, hoje e futuras no fuso do proprietário;
   - provar isolamento entre usuários e independência após alteração/exclusão da sessão de origem;
   - manter datas como `LocalDate` até o JSON.
5. **Interface de confirmação (RED → GREEN)**
   - testar a opção visível e marcada por padrão com conteúdo, ausente sem conteúdo, e o payload enviado;
   - inserir o controle no diálogo de finalização usando o papel inset e o marcador já definidos.
6. **Fila de revisões (RED → GREEN)**
   - testar loading, erro, vazio e as três seções temporais;
   - adicionar rota e navegação `Revisões`, consumindo datas civis sem conversão UTC;
   - implementar a fila responsiva e acessível.
7. **Refatoração e regressão**
   - remover duplicações sem alterar os contratos;
   - executar testes direcionados após cada ciclo e as suítes completas ao final.

## Direção da interface

- **Intenção:** concurseiro encerrando um bloco e, depois, abrindo a fila para saber o que revisar agora sem recalcular datas mentalmente.
- **Hierarquia:** no fechamento, a duração efetiva continua focal e o agendamento aparece como decisão secundária marcada; na fila, “Hoje” é o foco, atrasadas têm urgência contida e futuras formam o horizonte.
- **Paleta:** papel e tinta para a estrutura, fichário verde para ações/estado em dia, marcador amarelo para hoje e `late` apenas para atraso.
- **Profundidade:** cartões elevados discretos sobre o papel, linhas temporais em superfícies inset; sem nova estratégia de sombras.
- **Tipografia:** títulos editoriais e datas/números tabulares; quatro níveis de tinta para separar matéria, conteúdo, data e intervalo.
- **Espaçamento:** grade de 4 px, cartões de 16–20 px e controles com pelo menos 44 px.
- **Assinatura:** cada ocorrência usa um marcador de página cuja posição visual acompanha o horizonte (atrasada, hoje, futura), repetindo a silhueta do produto sem virar uma grade genérica de status.

## Verificação de aceite

- testes unitários do calendário de intervalos;
- integrações de criação, opt-out, ausência de conteúdo, unicidade, concorrência, consulta temporal, fuso, propriedade e independência da sessão;
- testes React do diálogo e da fila em loading, vazio, erro e preenchido;
- build TypeScript/Vite e suíte Vitest;
- inspeção visual em desktop e 390 × 844;
- suíte Maven completa sem regressões.
