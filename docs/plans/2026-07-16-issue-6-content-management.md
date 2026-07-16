# Issue #6 — Gerenciar conteúdos de uma matéria

## Resumo

- Considerar a issue #5 concluída pela PR #27 e implementar a #6 sobre a `main` atualizada.
- Manter `Content` dentro da feature `subject`, sempre associado a uma matéria e, indiretamente, ao proprietário dela.
- Disponibilizar uma página própria em `/materias/:subjectId/conteudos`, preservando o sistema visual existente.

## Alterações principais

### Persistência e backend

- Criar a migração `V6` com a tabela `content`: `id`, `subject_id`, `name`, `archived_at`, `created_at` e `updated_at`. A FK não terá exclusão em cascata.
- Normalizar nomes removendo espaços externos, condensando espaços internos e comparando sem diferenciar maiúsculas/minúsculas; acentos continuam significativos. O nome exibido terá de 1 a 120 caracteres.
- Garantir concorrência com índice único parcial por `(subject_id, lower(name))` apenas para conteúdos ativos. Duplicatas arquivadas serão permitidas; restaurar um nome conflitante retornará `409`.
- Toda consulta ou mutação verificará simultaneamente `subject_id` e o proprietário da matéria. Matéria ou conteúdo inexistente, pertencente a outro usuário ou associado a outra matéria retornará o mesmo `404`.
- Arquivamento e restauração serão idempotentes e atualizarão `updated_at` somente quando houver mudança de estado.

### API pública

- `GET /api/subjects/{subjectId}/contents?status=active|archived`
- `GET /api/subjects/{subjectId}/contents/{contentId}`
- `POST /api/subjects/{subjectId}/contents`
- `PUT /api/subjects/{subjectId}/contents/{contentId}`
- `POST /api/subjects/{subjectId}/contents/{contentId}/archive`
- `POST /api/subjects/{subjectId}/contents/{contentId}/restore`
- Entrada: `{ "name": string }`.
- Resposta: `id`, `subjectId`, `name`, `archived`, `createdAt` e `updatedAt`.
- A listagem usará `active` por padrão. Nome/status inválido retornará `400`, conflito normalizado retornará `409` em `ProblemDetail`.

### Interface

- Adicionar em cada matéria a ação “Ver conteúdos”, levando à página contextual da matéria.
- Exibir nome e estado da matéria, retorno para “Matérias”, ação principal “Novo conteúdo” e abas “Ativos”/“Arquivados”.
- Suportar criação, edição inline, confirmação de arquivamento e restauração sem recarregar a aplicação.
- Usar caches TanStack Query separados por `subjectId` e estado; mutações atualizarão ou moverão o item entre os caches.
- Adotar React Hook Form nos novos formulários, com validação de nome alinhada à API.
- Tratar autenticação, matéria inexistente, carregamento com skeleton, catálogo vazio, falha com retry, conflito de nome e estados pendentes das mutações.
- Manter papel quente, verde de fichário, marcador/bookmark, grade de 4 px, controles de pelo menos 44 px e layout responsivo. Matérias arquivadas continuarão consultáveis e seus conteúdos gerenciáveis, mas ficarão fora dos seletores padrão de estudo.

## Testes e validação

- Integração Spring/MockMvc/Testcontainers: criação e listagem por matéria, normalização, validação, edição, arquivamento, restauração e persistência no PostgreSQL.
- Cobrir duplicidade por caixa/espaços, nomes iguais em matérias diferentes, criação após arquivamento e conflito ao restaurar.
- Cobrir isolamento entre usuários em todos os endpoints e impedir o uso de conteúdo sob um `subjectId` diferente.
- Testes React para carregamento, vazio, erro/retry, criação, edição, arquivamento, restauração, conflito `409` e navegação a partir da matéria.
- Playwright para a jornada matéria → conteúdos → criação e para ausência de overflow em viewport móvel.
- Executar `mvn verify`, `npm test`, `npm run build`, `npm run test:responsive` e `docker build --tag estuda-livre:issue-6 .`.

## Premissas

- Não haverá hierarquia de subtópicos, ordenação manual, importação, métricas ou integração com ciclos/sessões nesta issue.
- Conteúdos arquivados permanecem preservados para referências futuras; não será criado endpoint de exclusão.
- Seletores futuros deverão exigir matéria e conteúdo ativos, mas essa integração será feita nas issues que introduzirem ciclos e sessões.
