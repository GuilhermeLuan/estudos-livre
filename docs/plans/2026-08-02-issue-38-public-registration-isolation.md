# Issue #38 — Validar cadastro público e isolamento multiusuário para produção

Issue: [GuilhermeLuan/estudos-livre#38](https://github.com/GuilhermeLuan/estudos-livre/issues/38)
Data: 2 de agosto de 2026

## Objetivo

Preparar o Estuda Livre para um teste privado em produção com amigos. O cadastro público existente permanecerá como mecanismo de criação de contas; o acesso à aplicação será limitado externamente pela infraestrutura do Cloudflare Tunnel.

O trabalho não cria outro modelo de autenticação. Ele comprova, pelas APIs públicas e por sessões HTTP reais, que a identidade autenticada delimita todos os dados de estudo de cada conta.

## Contratos e invariantes

- `APP_REGISTRATION_ENABLED=true` permite criar contas por `POST /api/auth/register`.
- Cadastro e login continuam protegidos pelo contrato CSRF da SPA.
- `GET /api/auth/me` deve refletir exclusivamente a conta da sessão enviada.
- O identificador da conta autenticada é a fronteira de ownership para matérias, conteúdos, ciclos, voltas, sessões, resultados de exercícios e revisões.
- Um identificador de outra conta deve ser indistinguível de um recurso inexistente nas operações de leitura e mutação.
- As restrições por conta são independentes: ciclo ativo, sessão aberta e fila de revisões de uma pessoa não bloqueiam outra.
- Fechar o cadastro impede apenas novas contas; contas existentes continuam autenticando normalmente.
- O caminho direto até a origem não pode contornar o Cloudflare Access: o Compose vincula a porta ao loopback por padrão e uma exposição em rede privada exige firewall equivalente.

## Fluxo de dados

Cada cenário de aceitação obtém um token CSRF, cadastra duas contas pela API e autentica cada uma separadamente. Os cookies `SESSION` e `XSRF-TOKEN` de cada cliente são mantidos separados. As operações de domínio são executadas com esses cookies, sem `SecurityMockMvcRequestPostProcessors.user` e sem acesso direto ao banco para comprovar resultados.

Os testes podem usar o banco apenas para limpeza isolada do cenário, seguindo a infraestrutura de integração existente. Resultados devem ser observados por respostas HTTP e pelos recursos recuperados pela API.

## Fatias TDD

1. **Tracer bullet de identidade:** duas contas cadastram, autenticam e leem identidades diferentes usando sessões reais.
2. **Coleções privadas:** recursos criados pela primeira conta não aparecem nas listagens da segunda.
3. **Acesso direto privado:** UUIDs conhecidos de recursos alheios não permitem leitura ou mutação.
4. **Fluxos independentes:** as duas contas conseguem manter estado de estudo independente.
5. **Operação em produção:** documentar cadastro público, URL externa e reconhecimento do HTTPS encaminhado pelo proxy confiável.

A verificação operacional também deve confirmar que a porta publicada usa `APP_BIND_ADDRESS=127.0.0.1` por padrão. `Protect with Access` valida o caminho atendido pelo túnel, mas não substitui o bloqueio de uma rota direta à origem.

Cada fatia segue um ciclo RED→GREEN. Se uma caracterização do comportamento já implementado ficar verde imediatamente, o resultado é registrado e nenhuma alteração especulativa de produção é feita. Falhas de autorização ou persistência retornam ao orquestrador para diagnóstico e correção sensível.

## Ownership e delegação

O Luna Max possuirá somente o novo teste de aceitação e, em tarefa separada, a documentação operacional. Ele não alterará configuração de segurança, migrações, controllers, services ou repositories sem uma nova delegação explícita.

O orquestrador conserva as decisões de contrato, investiga qualquer violação de isolamento, revisa o diff integrado e executa a verificação final.

## Verificação

- Teste de integração direcionado após cada comportamento.
- `mvn test` para backend e integração.
- `npm test` e `npm run build` no frontend.
- Revisão do diff contra a issue #38 e os invariantes de ownership.

## Fora de escopo

- Configurar Cloudflare Tunnel ou Cloudflare Access.
- Códigos de convite, aprovação administrativa ou criação manual de contas.
- Alterar recuperação de senha, duração de sessão ou modelo de autorização.
