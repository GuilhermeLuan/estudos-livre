# Issue #5 — Gerenciar matérias

## Resultado

A feature `subject` oferece catálogo de matérias isolado por usuário, com criação, listagem, consulta, edição, arquivamento e restauração. O frontend autenticado usa `/materias` como entrada principal e mantém credenciais em `/conta`.

## Contrato

- `GET /api/subjects?status=active|archived`
- `GET /api/subjects/{id}`
- `POST /api/subjects`
- `PUT /api/subjects/{id}`
- `POST /api/subjects/{id}/archive`
- `POST /api/subjects/{id}/restore`

Nomes são normalizados nas bordas, devem conter de 1 a 120 caracteres e podem se repetir. Toda consulta ou mutação inclui o proprietário; recurso inexistente ou pertencente a outro usuário retorna o mesmo `404 ProblemDetail`. Arquivamento substitui exclusão física.

## Decisões de implementação

- Flyway cria a tabela e o índice de catálogo por proprietário, estado e nome.
- O repository usa `JdbcClient`; transações ficam nos métodos públicos concretos do service.
- A validação usa Bean Validation no DTO de entrada e os erros seguem RFC 9457.
- Testes atravessam HTTP, segurança, service, repository, migração e PostgreSQL real via Testcontainers.
- A interface segue `.interface-design/system.md`: papel quente, verde de fichário, marcador como assinatura, sombra sutil, grade de 4 px e navegação responsiva.
- TanStack Query mantém caches separados para ativas e arquivadas; mutações movem ou atualizam itens localmente sem recarregar a aplicação.

## Referências oficiais consultadas

- [Spring Framework — JdbcClient](https://docs.spring.io/spring-framework/reference/data-access/jdbc/core.html#jdbc-JdbcClient)
- [Spring Framework — @Transactional](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
- [Spring MVC — Validation](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html)
- [Spring MVC — Error Responses](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)
- [Spring Boot — Testcontainers](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html)
