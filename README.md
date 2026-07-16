# Estuda Livre

Aplicação web self-hosted para organizar estudos. Este monorepo contém o frontend React/TypeScript em `frontend/` e o backend Spring Boot em `backend/`. A imagem de produção incorpora o build React ao JAR do backend, que serve a interface e a API `/api` no mesmo domínio.

## Executar com Docker Compose

Pré-requisito: Docker com Compose v2.

```bash
cp .env.example .env
docker compose up --build -d
```

Acesse <http://localhost:8080>. O Compose inicia apenas `app` e `postgres`; espera o PostgreSQL ficar saudável, aplica as migrações Flyway e só então publica a aplicação como pronta.

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

Backend (Java 21 e Maven 3.6.3+):

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
