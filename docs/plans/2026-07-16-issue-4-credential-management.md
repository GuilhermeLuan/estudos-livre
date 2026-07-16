# Issue #4 — Gerenciar cadastro e recuperação de senha

Issue: [GuilhermeLuan/estudos-livre#4](https://github.com/GuilhermeLuan/estudos-livre/issues/4)
Status: implementado
Data: 16 de julho de 2026

## Resumo

Completar os três fluxos de credenciais no backend e na interface React:

- cadastro público controlado por `APP_REGISTRATION_ENABLED`, fechado por padrão;
- troca autenticada de senha, preservando apenas a sessão atual;
- redefinição por link temporário gerado pelo operador, sem SMTP.

O comando reutilizará o mesmo JAR em modo não web, seguindo o suporte oficial do Spring Boot a `WebApplicationType.NONE` e `ApplicationRunner`. Consulte [SpringApplication — Spring Boot](https://docs.spring.io/spring-boot/reference/features/spring-application.html).

## Contratos e persistência

- Estender `GET /api/auth/bootstrap-status` para retornar `registrationRequired` e `registrationEnabled`.
- Criar `POST /api/auth/register` com e-mail, senha e fuso; retornar `201`, `403` quando o cadastro estiver fechado e `409` para e-mail duplicado.
- Criar `POST /api/auth/password/change`, autenticado, com `currentPassword` e `newPassword`; validar a senha atual, atualizar o hash e retornar `204`.
- Criar `POST /api/auth/password/reset`, público, com `token` e `newPassword`; tokens inexistentes, usados ou expirados retornam a mesma resposta genérica.
- Adicionar migração para `password_reset_token`, armazenando somente o SHA-256 do token, usuário, criação e expiração. O token terá 256 bits aleatórios, expirará em 30 minutos e será removido atomicamente ao uso.
- Ao gerar um novo link, remover tokens anteriores daquele usuário. O resgate será protegido contra concorrência por bloqueio da linha do token.
- Centralizar criação, normalização e validação de contas para reutilização entre bootstrap e cadastro público.

## Segurança, comando e interface

- Vincular `app.registration-enabled` a `APP_REGISTRATION_ENABLED=false` e `app.base-url` a `APP_BASE_URL=http://localhost:8080`, usando [configuração externa do Spring Boot](https://docs.spring.io/spring-boot/reference/features/external-config.html).
- Implementar o comando `docker compose run --rm app generate-password-reset-link --email=pessoa@example.com`.
- O comando iniciará o contexto sem servidor HTTP, exigirá um e-mail existente, imprimirá `${APP_BASE_URL}/redefinir-senha?token=...` e encerrará com código diferente de zero em caso de erro.
- Usar `FindByIndexNameSessionRepository` para localizar sessões pelo principal e `deleteById` para invalidá-las, conforme a [API oficial do Spring Session](https://docs.spring.io/spring-session/reference/api.html). Na troca, excluir todas menos a sessão corrente; na redefinição, excluir todas.
- Manter CSRF obrigatório também para cadastro e redefinição; liberar anonimamente apenas os endpoints necessários e a rota `/redefinir-senha`.
- Adicionar React Router com as rotas `/` e `/redefinir-senha`; configurar o Spring MVC para encaminhar essa rota ao `index.html`, já que o fallback automático do Boot cobre somente a página inicial. Consulte [Web — Spring Boot](https://docs.spring.io/spring-boot/reference/web/index.html).
- No login, mostrar cadastro somente quando habilitado. Reutilizar as regras do bootstrap e, após sucesso, retornar ao login.
- Na área autenticada, adicionar formulário de troca de senha e permanecer conectado após o sucesso.
- Na redefinição, solicitar apenas a nova senha: não haverá formulário público que receba e-mail. Após sucesso, redirecionar ao login; token inválido, expirado ou usado terá mensagem indistinguível.

## Testes e validação

- Integração backend com PostgreSQL real para cadastro fechado/aberto, duplicidade, validação e CSRF.
- Troca de senha com senha atual incorreta, login com hash antigo/novo e duas sessões demonstrando que somente a solicitante permanece válida.
- Recuperação cobrindo hash sem token em texto puro, expiração exata de 30 minutos, uso único, concorrência, resposta pública genérica e invalidação de todas as sessões.
- Testar o runner não web e o link produzido com `APP_BASE_URL`; realizar smoke test pelo comando real do Compose.
- Testes React para visibilidade do cadastro, criação de conta, troca autenticada, rota direta de redefinição e estados de sucesso/erro.
- Playwright para responsividade da nova rota e abertura direta do link.
- Executar `mvn verify`, `npm test`, `npm run build` e `npm run test:responsive`.
- Atualizar `.env.example`, Compose e README com as duas configurações, o comando operacional e o procedimento completo de recuperação.

## Premissas validadas

- A issue entregará backend, comando e todos os fluxos React.
- `APP_REGISTRATION_ENABLED` será avaliado na inicialização e permanecerá desabilitado por padrão.
- `APP_BASE_URL` será configurado uma vez pelo operador, com localhost como padrão de desenvolvimento.
- Não haverá SMTP, painel administrativo ou endpoint público para solicitar recuperação por e-mail.
- Antes da implementação, a linha de base estava limpa: 18 testes backend e 6 testes frontend passaram.
