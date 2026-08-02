# Estuda Livre

Aplicação web self-hosted para organizar estudos. Este monorepo contém o frontend React/TypeScript em `frontend/` e o backend Spring Boot em `backend/`. A imagem de produção incorpora o build React ao JAR do backend, que serve a interface e a API `/api` no mesmo domínio.

## Executar com Docker Compose

Pré-requisito: Docker com Compose v2.

```bash
cp .env.example .env
docker compose up --build -d
```

Acesse <http://localhost:8080>. O Compose inicia apenas `app` e `postgres`; espera o PostgreSQL ficar saudável, aplica as migrações Flyway e só então publica a aplicação como pronta.

No primeiro acesso, a interface solicita a criação da conta inicial. Depois disso, o cadastro inicial é fechado e o acesso passa a exigir e-mail e senha. O cadastro público permanece desabilitado por padrão; defina `APP_REGISTRATION_ENABLED=true` e reinicie a aplicação somente quando quiser permiti-lo. As sessões são armazenadas no PostgreSQL, expiram após `SESSION_TIMEOUT` (30 minutos por padrão) e podem ser encerradas pela própria interface.

### Recuperar uma senha

Não há envio de e-mail nem formulário público que receba endereços. O operador gera um link para uma conta existente usando o mesmo container da aplicação:

```bash
docker compose run --rm app generate-password-reset-link --email=pessoa@example.com
```

Configure `APP_BASE_URL` com a origem pública da instalação, sem barra final, para que o link aponte ao endereço correto. O token expira em 30 minutos, só pode ser usado uma vez e substitui qualquer link anterior da mesma conta. Envie o link à pessoa por um canal confiável; após a redefinição, todas as sessões dessa conta são encerradas.

Se a aplicação estiver atrás de um proxy reverso HTTPS confiável, defina `SERVER_FORWARD_HEADERS_STRATEGY=framework` para que o Spring reconheça `X-Forwarded-Proto` e marque o cookie de sessão como `Secure`. Mantenha o valor `none` quando a aplicação estiver exposta diretamente, sem um proxy que sobrescreva os cabeçalhos encaminhados.

### Teste privado atrás do Cloudflare Tunnel

Para um teste controlado, configure a aplicação com a origem pública do túnel:

```dotenv
APP_BIND_ADDRESS=127.0.0.1
APP_REGISTRATION_ENABLED=true
APP_BASE_URL=https://estudos.example.com
SERVER_FORWARD_HEADERS_STRATEGY=framework
```

Enquanto `APP_REGISTRATION_ENABLED` estiver ativo, qualquer visitante que consiga alcançar a aplicação poderá criar uma conta; a seleção dos amigos é responsabilidade da infraestrutura. O Cloudflare Tunnel sozinho apenas publica o hostname. Para restringir o acesso, crie uma aplicação HTTP self-hosted no Cloudflare Access com uma política `Allow` para as pessoas selecionadas. Sem uma aplicação Access, a publicação do túnel fica acessível na Internet — consulte a [documentação oficial do Cloudflare](https://developers.cloudflare.com/cloudflare-one/access-controls/applications/http-apps/self-hosted-public-app/).

O Compose vincula a porta da aplicação a `127.0.0.1` por padrão. Mantenha esse bind quando o `cloudflared` estiver no mesmo host, para que a porta `8080` não ofereça um caminho direto que contorne o Access. Se o túnel estiver em outra máquina ou rede, use em `APP_BIND_ADDRESS` somente um endereço privado alcançável por ela e bloqueie a porta no firewall para todas as demais origens.

Ative **Protect with Access** no `cloudflared` (ou valide o token do Access na origem) para rejeitar tokens ausentes ou inválidos no caminho do túnel. Essa validação não substitui o bloqueio do acesso direto à porta da origem. Depois que as contas forem criadas, altere `APP_REGISTRATION_ENABLED=false` e reinicie a aplicação: novos cadastros serão bloqueados, mas as contas e os logins existentes continuarão válidos.

Verificações operacionais:

```bash
curl http://localhost:8080/api/status
curl http://localhost:8080/actuator/health/liveness
curl http://localhost:8080/actuator/health/readiness
```

- **Liveness** verifica se o processo Spring continua funcional e não depende do banco.
- **Readiness** inclui o PostgreSQL, pois a aplicação não deve receber tráfego sem sua fonte de dados.

Para encerrar sem apagar os dados:

```bash
docker compose down
```

## Desenvolvimento

Backend (Java 25 e Maven 3.6.3+):

```bash
mvn test
```

Frontend (Node.js 22+):

```bash
cd frontend
npm ci
npm test
npm run dev
```

O Vite encaminha `/api` para `http://localhost:8080`. Os testes de integração do backend usam PostgreSQL real via Testcontainers; Docker precisa estar disponível.
