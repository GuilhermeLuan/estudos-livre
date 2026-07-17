# Issue #15 — Registrar exercícios e calcular acerto

## Objetivo

Permitir que uma sessão finalizada registre, opcionalmente, quantas questões foram realizadas e quantas foram acertadas. O resultado deve aparecer no histórico, poder ser corrigido depois sem alterar o tempo da sessão e alimentar agregados por matéria e conteúdo.

## Contrato público

### Finalização da sessão

`POST /api/study-sessions/{sessionId}/finish` passa a aceitar os campos opcionais:

- `questionsAttempted`: inteiro maior ou igual a zero;
- `questionsCorrect`: inteiro maior ou igual a zero e nunca maior que `questionsAttempted`.

Os dois campos ausentes, ou `questionsAttempted = 0`, significam “nenhum resultado de exercícios”. Se um deles for informado com valor positivo, ambos precisam formar um par válido.

### Edição posterior

`PUT /api/study-sessions/{sessionId}/exercise-result` recebe o mesmo par. Somente sessões `FINISHED` do usuário autenticado podem ser alteradas. Enviar zero remove o resultado existente. A operação não toca em início, fim, duração medida, duração efetiva, versão ou créditos da sessão.

### Leitura

- cada item de `GET /api/study-sessions/history` inclui `exerciseResult` opcional;
- `GET /api/study-sessions/exercise-summary` devolve totais por matéria e por conteúdo, sempre isolados por proprietário.

O resultado expõe `questionsAttempted`, `questionsCorrect` e `accuracyPercentage`. O percentual usa uma casa decimal e arredondamento `HALF_UP`: `correct / attempted * 100` (por exemplo, `7/9 = 77,8%`).

## Modelo e persistência

Criar uma tabela `study_session_exercise_results` com uma linha no máximo por sessão:

- `session_id` como chave primária e FK com exclusão em cascata;
- contadores inteiros não negativos;
- restrição `questions_correct <= questions_attempted`;
- restrição `questions_attempted > 0`, pois zero é representado pela ausência da linha;
- `created_at` e `updated_at`.

A consulta de histórico fará `LEFT JOIN` com o resultado. A consulta de agregados somará tentativas e acertos agrupando pelas identidades e nomes de matéria/conteúdo da sessão; conteúdo ausente não cria grupo de conteúdo.

## Sequência TDD

1. **Cálculo puro (RED → GREEN)**
   - escrever testes unitários para ausência em zero, rejeição de negativos, rejeição de acertos acima das tentativas e arredondamento em uma casa;
   - implementar um objeto de domínio/calculador pequeno, sem dependência de HTTP ou banco.
2. **Persistência na finalização (RED → GREEN)**
   - provar por integração que uma sessão finalizada persiste o resultado e o devolve no histórico;
   - provar que campos vazios/zero não criam linha e que pares inválidos retornam `400`;
   - adaptar DTO, serviço, repositório, projeções e resposta com a menor mudança capaz de passar.
3. **Edição posterior (RED → GREEN)**
   - provar atualização, remoção com zero, isolamento por proprietário e imutabilidade dos campos temporais;
   - implementar o `PUT` e o upsert/delete do resultado.
4. **Agregados (RED → GREEN)**
   - provar soma e percentual por matéria e conteúdo, inclusive múltiplas sessões e isolamento entre usuários;
   - implementar a consulta e o endpoint de resumo.
5. **Interface (RED → GREEN)**
   - testar os campos opcionais no diálogo de finalização, validação acessível e payload;
   - testar resultado no histórico, resumo e edição sem mudança do tempo exibido;
   - implementar os componentes e estados estritamente necessários.
6. **Refatoração e regressão**
   - remover duplicações entre finalização e edição, preservar uma única regra de validação/cálculo;
   - rodar testes direcionados após cada ciclo e as suítes completas ao final.

## Direção da interface

Usar o sistema visual existente: papel quente, verde de fichário e amarelo marcador, sem criar uma nova superfície estética. No diálogo de finalização, exercícios formam um bloco secundário claramente opcional abaixo do tempo efetivo; os dois números ficam lado a lado no desktop e empilhados no celular. O percentual calculado aparece como feedback textual, não como medidor decorativo.

No histórico, o resultado será uma linha compacta da ficha da sessão, com ação “Editar exercícios”. O resumo será uma única seção de caderno, com cartões discretos por matéria e conteúdos subordinados, priorizando os totais e a taxa. Diálogos continuam nativos, alvos têm pelo menos 44 px e o breakpoint móvel preserva uma única coluna.

## Verificação de aceite

- testes unitários do cálculo e validação;
- testes de integração de persistência, ausência, edição, remoção, propriedade e agregados;
- testes React do diálogo de finalização, histórico/resumo e edição;
- build TypeScript/Vite;
- inspeção visual desktop e 390 × 844, incluindo estados vazio, preenchido e erro;
- suíte Maven completa e suíte frontend completa sem regressões.
