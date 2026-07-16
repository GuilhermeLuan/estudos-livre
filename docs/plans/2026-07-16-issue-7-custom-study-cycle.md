# Issue #7 — Criar ciclo personalizado

## Resultado

A feature `studycycle` permitirá criar vários ciclos personalizados como rascunhos, editar o nome e montar uma sequência ordenada de etapas. Cada etapa referencia uma matéria ativa do próprio usuário e define uma meta positiva em múltiplos de cinco minutos.

## Contrato

- `GET /api/study-cycles`
- `GET /api/study-cycles/{id}`
- `POST /api/study-cycles`
- `PUT /api/study-cycles/{id}`

A criação aceita o nome e inicia um rascunho vazio. A atualização substitui atomicamente a composição do rascunho, preservando a ordem recebida. A resposta contém o total calculado, se o ciclo já pode ser ativado e, por etapa, matéria, posição, duração e alerta de bloco acima de três horas.

## Decisões de implementação

- Flyway cria `study_cycle` e `study_cycle_stage`, com modo `CUSTOM`, estado `DRAFT`, posição única por ciclo e restrições de duração no PostgreSQL.
- O repository usa `JdbcClient`; o service concreto delimita a transação que salva ciclo e etapas.
- A feature `studycycle` consulta o service público de `subject` para validar matéria ativa e pertencente ao usuário, sem acessar repository de outra feature.
- Rascunhos vazios são permitidos, mas retornam `activatable: false`; a ativação será implementada pela issue #8.
- Etapas acima de 180 minutos são válidas e retornam `longBlockWarning: true`.
- Recurso inexistente ou pertencente a outro usuário retorna o mesmo `404 ProblemDetail`.
- Testes de integração atravessam MockMvc, segurança, services, repositories, Flyway e PostgreSQL via Testcontainers.

## Interface

- A rota `/ciclos` combina uma estante compacta de rascunhos com um editor focal da sequência.
- O editor permite adicionar, remover e mover etapas para cima ou para baixo usando controles nativos e acessíveis.
- O total é recalculado imediatamente e funciona como assinatura visual do planejamento.
- O alerta de bloco longo usa o marcador amarelo do sistema, comunica a recomendação e não bloqueia o salvamento.
- A tela preserva papel quente, verde de fichário, bookmark, sombras sutis, grade de 4 px, alvos de 44 px e layout responsivo.
- Há estados de carregamento, vazio, erro com nova tentativa, validação e salvamento pendente.

## Ciclos TDD

1. Criar e listar vários rascunhos isolados por usuário.
2. Salvar etapas ordenadas e recalcular o total.
3. Validar duração positiva em múltiplos de cinco e aceitar blocos acima de três horas com alerta.
4. Rejeitar matérias arquivadas ou pertencentes a outro usuário e ocultar ciclos alheios.
5. Cobrir no frontend criação, composição, reordenação, alerta e estados críticos.

## Fora de escopo

- Ativar, trocar ou retomar ciclos (#8).
- Gerar ciclos sugeridos (#9).
- Editar ciclo sugerido, regenerar sugestões ou redistribuir voltas em andamento.
- Criar voltas, creditar sessões ou calcular progresso.

## Referências oficiais consultadas

- [Spring Framework — JdbcClient](https://docs.spring.io/spring-framework/reference/data-access/jdbc/core.html#jdbc-JdbcClient)
- [Spring Framework — @Transactional](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
- [Spring MVC — Validation](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html)
- [Spring MVC — Error Responses](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)
- [Spring Boot — Testing Spring Boot Applications](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html)
- [Spring Boot — Testcontainers](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html)
