# Nexus App Dashboard

Este repositorio contem o template base do dashboard Nexus, com:

- `front-end/`: aplicacao Angular.
- `back-end/nexusapp/`: API Spring Boot.
- PostgreSQL como banco de dados local.

Use este guia para abrir o dashboard em uma maquina de desenvolvimento.

## Pre-requisitos

Antes de comecar, instale:

- Git
- Node.js 22 ou superior
- npm
- Java JDK 17
- Docker Desktop

Para conferir as versoes:

```bash
node -v
npm -v
java -version
docker --version
```

## 1. Baixar o projeto

Clone o repositorio e entre na pasta do projeto:

```bash
git clone https://github.com/Raphaelgg99/nexus-app.git
cd nexus-app
```

Se voce recebeu o projeto como `.zip`, extraia o arquivo e abra a pasta extraida no terminal.

## 2. Configurar o backend

Entre na pasta do backend:

```bash
cd back-end/nexusapp
```

Crie um arquivo chamado `.env` dentro de `back-end/nexusapp/` com o conteudo abaixo:

```env
SERVER_PORT=8080

POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=nexus
POSTGRES_USER=nexus
POSTGRES_PASSWORD=nexus
POSTGRES_SSL_MODE=disable
POSTGRES_JDBC_PARAMS=

JWT_SECRET=troque-esse-jwt-secret
SECRET_KEY=troque-essa-secret-key
JWT_EXPIRATION=86400000

INITIAL_ADMIN_USERNAME=admin
INITIAL_ADMIN_PASSWORD=admin123

OPENAI_ACCESS_TOKEN=troque-se-for-usar-openai
NANOBANANA_ACCESS_TOKEN=troque-se-for-usar-nanobanana
N8N_CAMPAIGN_WEBHOOK_URL=

FRONTEND_URL=http://localhost:4200
```

Esses valores sao suficientes para ambiente local. Em projetos reais, troque as senhas e secrets.

## 3. Subir o banco de dados

Ainda dentro de `back-end/nexusapp/`, suba o PostgreSQL com Docker:

```bash
docker compose up -d
```

Para verificar se o container esta rodando:

```bash
docker ps
```

O banco ficara disponivel em:

- Host: `localhost`
- Porta: `5432`
- Database: `nexus`
- Usuario: `nexus`
- Senha: `nexus`

## 4. Rodar o backend

Ainda dentro de `back-end/nexusapp/`, execute:

### Windows

```bash
mvnw.cmd spring-boot:run
```

### macOS/Linux

```bash
./mvnw spring-boot:run
```

Quando o backend subir, a API ficara em:

```text
http://localhost:8080
```

O Swagger da API fica em:

```text
http://localhost:8080/swagger-ui/index.html
```

## 5. Rodar o frontend

Abra outro terminal na raiz do projeto e entre na pasta do frontend:

```bash
cd front-end
```

Instale as dependencias:

```bash
npm install
```

Suba o Angular:

```bash
npm start
```

Abra no navegador:

```text
http://localhost:4200
```

O frontend em desenvolvimento chama automaticamente o backend em `http://localhost:8080`.

## 6. Login inicial

Se o backend subiu com o `.env` acima, o primeiro usuario admin sera criado automaticamente:

```text
Usuario: admin
Senha: admin123
```

Depois do primeiro acesso, altere essas credenciais se estiver usando o dashboard como base para outro projeto.

## Fluxo resumido

Em um terminal:

```bash
cd back-end/nexusapp
docker compose up -d
mvnw.cmd spring-boot:run
```

Em outro terminal:

```bash
cd front-end
npm install
npm start
```

Depois acesse:

```text
http://localhost:4200
```

## Comandos uteis

Parar o banco:

```bash
cd back-end/nexusapp
docker compose down
```

Parar o banco e apagar os dados locais:

```bash
cd back-end/nexusapp
docker compose down -v
```

Gerar build do frontend:

```bash
cd front-end
npm run build
```

Rodar testes do backend:

```bash
cd back-end/nexusapp
mvnw.cmd test
```

Rodar testes do frontend:

```bash
cd front-end
npm test
```

## Problemas comuns

### Porta 5432 ja esta em uso

Altere `POSTGRES_PORT` no arquivo `.env` e suba o banco novamente:

```env
POSTGRES_PORT=5433
```

Depois rode:

```bash
docker compose up -d
```

### Porta 8080 ja esta em uso

Altere `SERVER_PORT` no `.env`. Se mudar a porta do backend, ajuste tambem `front-end/src/app/services/api-url.ts`.

### Frontend nao consegue acessar o backend

Confira se:

- O backend esta rodando em `http://localhost:8080`.
- O frontend esta rodando em `http://localhost:4200`.
- A variavel `FRONTEND_URL` no `.env` esta como `http://localhost:4200`.

### Erro de variaveis ausentes no backend

Garanta que o arquivo `.env` existe em:

```text
back-end/nexusapp/.env
```

E que ele possui todas as variaveis listadas na secao de configuracao do backend.
