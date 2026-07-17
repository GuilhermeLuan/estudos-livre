# Fluxo flexível do ciclo de estudos

## Objetivo

O ciclo organiza atividades em uma ordem sugerida, mas não obriga o usuário a estudar uma posição específica. O usuário pode registrar estudo em qualquer atividade, editar o planejamento em andamento e trocar de ciclo em qualquer percentual de progresso.

## Modelo de domínio

- `StudyCycle` é o planejamento editável: nome, ordem das atividades, matérias e metas.
- `StudyCycleStage` é uma ocorrência ordenada de uma matéria. Sua posição orienta a visualização e a sugestão, não restringe lançamentos.
- `StudyCycleRun` é uma volta do planejamento e não possui cursor de etapa atual.
- O progresso futuro de cada atividade será uma projeção das sessões: meta, minutos creditados, minutos restantes e estado derivado.
- A primeira atividade incompleta na ordem pode ser apresentada como sugestão, nunca como obrigação.
- `StudySession` permanecerá a fonte da verdade. Ao escolher uma atividade explicitamente, o crédito começa nela; ao informar apenas a matéria, começa na primeira ocorrência incompleta da matéria.

## Edição da volta em andamento

Ciclos em rascunho, ativos ou inativos podem ser editados. A alteração vale imediatamente para a volta atual.

Quando sessões e créditos forem implementados, a edição preservará as sessões e recalculará cronologicamente a projeção da volta. Atividades novas começam sem crédito; atividades removidas deixam o planejamento sem apagar o histórico; excedentes seguem para as próximas ocorrências da mesma matéria e, sem destino, permanecem somente nas métricas.

Um ciclo ativo não pode ser salvo sem atividades. Voltas concluídas preservam o snapshot do planejamento utilizado.

## Ativação e troca livre

Não existe percentual mínimo nem conclusão obrigatória para ativar outro ciclo. Continua existindo no máximo um ciclo ativo por usuário.

Ao trocar de ciclo, o usuário escolhe o destino da volta atual:

- **Pausar e trocar:** preserva a volta como `PAUSED`; reativar o ciclo retoma a mesma volta.
- **Encerrar volta e trocar:** preserva o histórico, marca a volta como `ABANDONED` e faz a próxima ativação criar uma nova volta.

Uma volta também pode estar `IN_PROGRESS` ou `COMPLETED`. Abandono não representa falha nem penalidade.

A troca e a criação ou retomada da volta acontecem em uma única transação, protegidas por trava do proprietário e por invariantes no PostgreSQL.

## Interface

O ciclo ativo é apresentado como um mapa completo da volta. Cada atividade mostra ordem, matéria e meta; progresso e restante serão acrescentados quando as sessões existirem. O marcador de caderno identifica a sugestão visual sem esconder as demais opções.

Todos os ciclos oferecem edição. Ao ativar outro ciclo, uma confirmação explica as consequências e oferece `Pausar e trocar` como ação principal e `Encerrar volta e trocar` como alternativa destrutiva. Sem ciclo ativo, a ativação permanece direta.

## Entrega incremental

Nesta correção da issue 8:

1. Remover a posição atual persistida e exposta pela API.
2. Representar o estado da volta explicitamente.
3. Permitir edição de ciclos em qualquer estado, bloqueando somente um ciclo ativo vazio.
4. Tornar a escolha da troca explícita no contrato de ativação.
5. Exibir o fluxo integral no frontend e remover linguagem de etapa obrigatória.

Sessões, créditos por atividade, snapshots e recálculo serão implementados nas respectivas fatias posteriores sobre este contrato.

## Estratégia de testes

- Testes de integração pela API comprovam volta sem cursor, edição ativa/inativa, pausa, abandono, retomada, nova volta, concorrência e isolamento entre usuários.
- Testes de frontend comprovam o mapa completo, edição de todos os estados, confirmação da troca e atualização do cache sem recarga.
- Cada comportamento será implementado em um ciclo RED-GREEN antes do próximo tracer bullet.
- A entrega termina com as suítes completas de backend e frontend, build de produção e QA visual em desktop e celular.
