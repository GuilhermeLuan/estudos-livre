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
