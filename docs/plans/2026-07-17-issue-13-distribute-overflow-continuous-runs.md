# Issue #13 — distribuir excedente e manter voltas contínuas

## Objetivo

Ao finalizar uma sessão, distribuir sua duração efetiva entre as etapas incompletas da mesma matéria na volta atual, em ordem. O crédito nunca atravessa para outra matéria nem para a próxima volta. Quando todas as etapas forem concluídas, encerrar a volta atual, preservar seu retrato histórico e iniciar uma nova volta sem créditos herdados.

## Invariantes de domínio

- A duração efetiva da sessão continua sendo a métrica integral da sessão.
- O crédito percorre somente etapas da mesma matéria, ordenadas por posição.
- Etapas já completas são ignoradas.
- Cada etapa recebe no máximo o tempo que falta para sua meta.
- O excedente depois da última etapa elegível não vira crédito em outra matéria.
- O excedente depois de concluir a volta não entra na volta seguinte.
- A etapa corrente da volta é sempre a primeira etapa globalmente incompleta; etapas antecipadamente preenchidas são puladas.
- Uma volta só é concluída quando todas as suas etapas estão completas.
- Uma volta concluída mantém datas, metas, nomes das matérias e progresso como retrato independente de futuras edições do ciclo.

## Contratos públicos

### Finalização da sessão

Manter `POST /api/study-sessions/{id}/finish`. A resposta poderá conter vários créditos, um para cada etapa da mesma matéria alcançada pela duração efetiva.

### Ciclo atual

Acrescentar `currentStagePosition` à volta atual retornada pelo ciclo. A posição será derivada da primeira etapa incompleta da volta, evitando que o frontend presuma que a primeira etapa do ciclo é sempre a corrente.

### Histórico de voltas

Adicionar `GET /api/study-cycles/{id}/runs`, retornando as voltas em ordem decrescente, com:

- identificação, número, estado, início e término;
- retrato ordenado das etapas;
- matéria e nome preservado;
- meta e crédito em segundos;
- indicação de conclusão de cada etapa.

## Desenho da implementação

1. Criar um distribuidor puro de créditos que recebe as etapas da volta, a matéria da sessão e a duração efetiva. Ele devolve alocações por etapa e o excedente não alocado.
2. Bloquear e carregar todas as etapas da volta durante a finalização, aplicar as alocações e persistir um crédito por etapa.
3. Após os créditos, localizar a primeira etapa globalmente incompleta.
4. Se nenhuma etapa permanecer incompleta, marcar a volta com `COMPLETED` e `ended_at`, criar a próxima volta e gerar seu retrato zerado. O excedente é descartado da projeção antes disso.
5. Expor a posição corrente no contrato do ciclo e os retratos no endpoint de histórico.
6. Atualizar a tela do ciclo para destacar a posição corrente real e oferecer um registro cronológico consultável das voltas.

## Sequência TDD

### Ciclo 1 — distribuição pura

- Vermelho: teste que preenche duas etapas da mesma matéria separadas por uma etapa de outra matéria.
- Verde: implementar a menor distribuição sequencial suficiente.
- Ampliar: testar excedente e salto de etapa já completa.

### Ciclo 2 — persistência e posição corrente

- Vermelho: integração finalizando uma sessão longa em um ciclo intercalado.
- Verde: persistir múltiplos créditos, sem tocar outra matéria, e retornar a primeira etapa globalmente incompleta.

### Ciclo 3 — transição contínua de volta

- Vermelho: integração que conclui a última etapa com excedente.
- Verde: concluir a volta, criar a próxima zerada e preservar o retrato da anterior.

### Ciclo 4 — consulta e interface

- Vermelho: teste do frontend em que a etapa corrente não é a primeira e o histórico é aberto.
- Verde: consumir `currentStagePosition`, carregar o histórico sob demanda e renderizar o registro de voltas.

## Direção de interface

Usar o vocabulário visual já estabelecido de papel, fichário e marca-texto. O componente de histórico será um “caderno de voltas”: uma coluna cronológica com lombada discreta, marcadores numerados e progresso em tinta/verde do fichário. A volta atual recebe contorno de marca-página; voltas concluídas usam preenchimento contido. Evitar abas genéricas, grade de cartões e gráficos multicoloridos.

Responsividade:

- desktop: registro alinhado ao conteúdo do ciclo, sem competir com a etapa corrente;
- tablet: itens em uma única coluna;
- celular: resumo e etapas empilhados, controles com área de toque confortável e sem rolagem horizontal.

## Verificação

- testes unitários do distribuidor;
- integrações de distribuição, isolamento por matéria, excedente, conclusão e criação de nova volta;
- teste do endpoint de histórico e de seu retrato;
- testes do frontend e build de produção;
- inspeção visual e funcional em desktop e celular;
- suíte completa do backend e verificação de diferenças do Git.
