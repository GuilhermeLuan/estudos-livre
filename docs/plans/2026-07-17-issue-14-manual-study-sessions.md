# Issue #14 — registrar sessões manuais

## Objetivo

Permitir que o estudante registre um bloco já concluído sem iniciar o cronômetro. A sessão nasce como `FINISHED`, preserva a data local informada como um instante UTC conforme o fuso IANA do proprietário e participa do histórico e da projeção da volta ativa como qualquer outro estudo.

## Contratos públicos

### Criar lançamento manual

`POST /api/study-sessions/manual` recebe:

- `startedAtLocal`: data e hora local sem offset;
- `effectiveSeconds`: duração líquida positiva;
- `subjectId` e `contentId` opcional;
- `notes` opcional.

A resposta usa o contrato de `StudySessionResponse`, com `origin = MANUAL`, `status = FINISHED`, `measuredSeconds = effectiveSeconds`, `finishedAt = startedAt + effectiveSeconds` e os créditos aplicados.

### Consultar histórico

`GET /api/study-sessions/history` retorna as sessões concluídas do proprietário, da mais recente para a mais antiga. O contrato expõe anotações e créditos, permitindo que frontend e futuras métricas usem a mesma fonte pública.

## Invariantes

- A data local é convertida com o `timeZone` IANA do usuário autenticado antes da persistência.
- A duração precisa ser maior que zero; matéria é obrigatória; conteúdo, quando presente, deve estar ativo e pertencer à matéria e ao usuário.
- Sessões manuais não possuem segmentos de cronômetro e são persistidas diretamente como `FINISHED`.
- Se houver volta ativa, o distribuidor da issue #13 aplica o tempo às etapas incompletas da matéria, em ordem, sem atravessar para outra matéria ou para a volta seguinte.
- Uma matéria ausente da volta continua integralmente registrada, mas não gera crédito.
- Histórico, créditos e eventual nova volta são persistidos na mesma transação.
- Consultas nunca expõem sessões de outro proprietário.

## Ciclos TDD

1. **Tracer bullet — criação e histórico com fuso**
   - RED: integração HTTP cria um lançamento em um fuso diferente de UTC e observa sessão finalizada, instante convertido, duração, anotações e presença imediata no histórico.
   - GREEN: migração, request, persistência direta, endpoint de criação e endpoint de histórico mínimos.
2. **Validação e propriedade**
   - RED/GREEN incremental para duração não positiva, data inválida, matéria de outro usuário e conteúdo de outra matéria/proprietário.
   - Manter validações Bean Validation como 400 e propriedade como 404, seguindo os contratos atuais.
3. **Progresso da volta ativa**
   - RED: lançamento da matéria da volta distribui crédito e atualiza a posição/progresso observável pela API de ciclos.
   - GREEN: localizar a volta ativa, reutilizar `StudyCreditDistributor`, persistir alocações e conservar a transição contínua de voltas.
   - RED/GREEN: matéria fora do ciclo permanece sem créditos.
4. **Registro e histórico no frontend**
   - RED: teste de componente abre o formulário, envia data local/duração/matéria/conteúdo/anotações e mostra o registro salvo no histórico.
   - GREEN: cliente HTTP, modal nativo, histórico recente e invalidação das queries de histórico e ciclos.
   - Cobrir pendência, erro, vazio e responsividade sem criar controles não semânticos.

## Direção de interface

- **Domínio:** caderno de estudos, ficha de registro, relógio líquido, matéria, conteúdo, volta e crédito.
- **Mundo de cor:** papel, tinta, fichário verde, marca-texto amarelo, papel inset e correção terracota.
- **Assinatura:** uma ficha cronológica com marcador de duração liga o lançamento ao caderno de voltas.
- **Rejeições:** modal SaaS genérico vira ficha editorial; grade de cartões vira registro cronológico; múltiplas cores viram um único destaque de marcador para tempo/crédito.
- **Intenção:** estudante voltando ao sistema depois de estudar fora dele; precisa registrar em poucos campos, com confiança e sem parecer que está reconstruindo um cronômetro.
- **Hierarquia:** duração e data são a decisão principal; matéria contextualiza; conteúdo e anotações permanecem secundários; salvar é o único comando primário.
- **Profundidade:** modal nativo elevado e campos no papel inset, conforme o sistema existente.
- **Tipografia e espaço:** títulos editoriais, números tabulares, grade de 4 px e controles de pelo menos 44 px.

## Verificação

- Demonstrar cada teste novo em RED antes da implementação mínima correspondente.
- Rodar a classe de integração de sessões após cada ciclo e a suíte completa do backend ao concluir.
- Rodar teste de componente focado, Vitest completo e build Vite.
- Inspecionar criação, vazio, erro e histórico em desktop e mobile; confirmar foco, fechamento e ausência de rolagem horizontal.
