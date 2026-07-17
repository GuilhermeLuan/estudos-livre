# Issue 10 — Personalizar ou regenerar uma sugestão

## Objetivo

Permitir que o estudante assuma uma sugestão como planejamento próprio sem perder as alterações manualmente feitas, e regenerar as etapas somente depois de compreender e confirmar a substituição.

## Interface pública

- `PUT /api/study-cycles/{cycleId}` continua sendo a edição manual. Ao salvar um ciclo sugerido, o modo passa a `CUSTOM`.
- `PUT /api/study-cycles/{cycleId}/suggestion` recebe nome e entradas de sugestão. Essa é a única operação que recalcula e substitui etapas, retornando o modo a `SUGGESTED`.
- O editor informa a mudança de modo antes do primeiro salvamento manual.
- A regeneração acontece em um fluxo explícito de preparação e confirmação; cancelar encerra o fluxo sem mutação.

## Ciclos TDD

1. **Regeneração persistida**: provar pela API que uma sugestão personalizada só tem etapas substituídas quando o endpoint de regeneração é confirmado; implementar a operação transacional mínima.
2. **Propriedade e validação**: provar que o ciclo e todas as matérias pertencem ao usuário e que uma falha preserva integralmente o planejamento anterior.
3. **Transição manual visível**: provar pela interface que editar uma sugestão mostra, antes de salvar, a passagem para `CUSTOM`; manter o aviso ligado ao modo persistido, não a detalhes internos.
4. **Confirmação de impacto**: provar pela interface que a ação apresenta duração e quantidade de atividades atuais e estimadas antes de permitir a substituição.
5. **Confirmação e cancelamento**: provar que confirmar chama a regeneração e atualiza o ciclo para `SUGGESTED`, enquanto cancelar não envia mutação e preserva o editor.

## Direção de interface

- **Pessoa e tarefa**: concurseiro ajustando um ciclo depois de comparar o edital com sua rotina; deve decidir com calma se mantém o planejamento manual ou volta ao cálculo.
- **Hierarquia**: no editor, a sequência continua sendo o foco. A mudança de modo aparece como aviso contextual; na confirmação, o comparativo de impacto domina.
- **Paleta e profundidade**: manter papel, tinta, fichário verde e marcador amarelo, com superfícies elevadas pelas sombras sutis já existentes.
- **Tipografia e espaçamento**: manter a combinação editorial existente e a grade de 4 px; controles com área mínima de 44 px.
- **Assinatura**: representar a substituição como duas versões do mapa da volta — planejamento atual e nova sugestão — usando a linguagem de fichas e marca-páginas do produto.

## Validação

- Testes de integração do backend para transição, persistência, validação e propriedade.
- Testes de componente do frontend para aviso, impacto, confirmação e cancelamento.
- Build de produção e inspeção visual responsiva da tela de ciclos.
- Suíte completa para regressão antes de seguir para a issue 11.
