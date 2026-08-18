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

## Deploy em VPS

Este projeto ja possui um workflow de deploy em `.github/workflows/deploy.yml`.

Quando ha push na branch `main`, o GitHub Actions executa este fluxo:

1. Roda os testes do backend.
2. Gera o build do frontend.
3. Cria as imagens Docker do backend e do frontend.
4. Publica as imagens no GitHub Container Registry, o GHCR.
5. Copia `docker-compose.production.yml` e o arquivo de ambiente para a VPS.
6. Executa `docker compose pull` e `docker compose up -d --remove-orphans` na VPS.

O deploy de producao usa `docker-compose.production.yml`. Esse arquivo nao cria um PostgreSQL dentro da VPS. O backend se conecta em um banco externo, como Supabase Postgres, Neon, RDS ou outro PostgreSQL gerenciado.

### 1. Preparar a VPS

Na VPS, instale Docker e o plugin do Docker Compose.

Em servidores Ubuntu/Debian, um caminho comum e:

```bash
sudo apt update
sudo apt install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

Confira se esta tudo instalado:

```bash
docker --version
docker compose version
```

Crie a pasta onde a aplicacao vai ficar:

```bash
mkdir -p ~/nexus-app
```

Se o usuario SSH nao consegue rodar Docker sem `sudo`, adicione o usuario ao grupo `docker`:

```bash
sudo usermod -aG docker $USER
```

Depois saia e entre novamente na VPS para o grupo ser aplicado.

### 2. Preparar o banco de producao

Crie um banco PostgreSQL de producao. Pode ser Supabase, Neon, AWS RDS ou outro provedor.

Guarde estas informacoes:

- Host do banco
- Porta
- Nome do banco
- Usuario
- Senha
- Modo SSL
- Parametros JDBC extras, se existirem

Exemplo para Supabase usando Transaction Pooler:

```text
DB_HOST=aws-0-sa-east-1.pooler.supabase.com
DB_PORT=6543
DB_NAME=postgres
DB_USERNAME=postgres.PROJECT_REF
DB_PASSWORD=sua-senha
DB_SSL_MODE=require
DB_JDBC_PARAMS=&prepareThreshold=0
```

### 3. Configurar acesso SSH do GitHub Actions

Na sua maquina, gere uma chave SSH para deploy:

```bash
ssh-keygen -t ed25519 -C "github-actions-nexus-deploy" -f nexus_deploy_key
```

Copie a chave publica para a VPS:

```bash
ssh-copy-id -i nexus_deploy_key.pub usuario@IP_DA_VPS
```

Teste o acesso:

```bash
ssh -i nexus_deploy_key usuario@IP_DA_VPS
```

Depois, copie o conteudo da chave privada `nexus_deploy_key`. Ela sera usada no secret `VPS_SSH_KEY`.

### 4. Configurar Secrets no GitHub

No GitHub, abra:

```text
Repository > Settings > Secrets and variables > Actions > Secrets
```

Crie estes secrets:

```text
VPS_SSH_KEY
VPS_HOST
VPS_USER
DB_USERNAME
DB_PASSWORD
JWT_SECRET
SECRET_KEY
INITIAL_ADMIN_USERNAME
INITIAL_ADMIN_PASSWORD
OPENAI_ACCESS_TOKEN
NANOBANANA_ACCESS_TOKEN
```

Secrets opcionais:

```text
VPS_PORT
VPS_APP_DIR
SITE_DOMAIN
FRONTEND_URL
DB_HOST
DB_PORT
DB_NAME
DB_SSL_MODE
DB_JDBC_PARAMS
JWT_EXPIRATION
N8N_CAMPAIGN_WEBHOOK_URL
GHCR_USERNAME
GHCR_TOKEN
```

Use `GHCR_USERNAME` e `GHCR_TOKEN` se as imagens do GHCR estiverem privadas. O token precisa ter permissao `read:packages`.

### 5. Configurar Variables no GitHub

No GitHub, abra:

```text
Repository > Settings > Secrets and variables > Actions > Variables
```

Crie estas variables se nao tiver colocado os mesmos valores como secrets:

```text
VPS_HOST
VPS_USER
VPS_PORT
VPS_APP_DIR
HTTP_PORT
SITE_DOMAIN
FRONTEND_URL
DB_HOST
DB_PORT
DB_NAME
DB_SSL_MODE
DB_JDBC_PARAMS
```

Valores comuns:

```text
VPS_PORT=22
VPS_APP_DIR=nexus-app
HTTP_PORT=8088
SITE_DOMAIN=seudominio.com
FRONTEND_URL=https://seudominio.com
DB_PORT=5432
DB_SSL_MODE=require
JWT_EXPIRATION=86400000
```

### 6. Configurar dominio e proxy reverso

O container do frontend expoe a aplicacao na porta definida por `HTTP_PORT`. Por padrao:

```text
http://IP_DA_VPS:8088
```

Para usar HTTPS com dominio, configure o DNS do dominio apontando para o IP da VPS e use um proxy reverso, como Caddy ou Nginx.

Exemplo de Caddyfile:

```text
seudominio.com {
  reverse_proxy 127.0.0.1:8088
}
```

Depois recarregue o Caddy:

```bash
sudo systemctl reload caddy
```

### 7. Fazer o primeiro deploy

Envie as alteracoes para a branch `main`.

Exemplo:

```bash
git checkout main
git pull origin main
git merge sua-branch
git push origin main
```

O deploy tambem pode ser iniciado manualmente:

```text
GitHub > Actions > Deploy to VPS > Run workflow
```

### 8. Conferir se subiu

Na VPS, entre na pasta da aplicacao:

```bash
cd ~/nexus-app
```

Confira os containers:

```bash
docker compose --env-file .env.production ps
```

Veja os logs do backend:

```bash
docker compose --env-file .env.production logs -f backend
```

Veja os logs do frontend:

```bash
docker compose --env-file .env.production logs -f frontend
```

Teste no navegador:

```text
https://seudominio.com
```

Ou, se ainda nao tiver dominio:

```text
http://IP_DA_VPS:8088
```

### 9. Como atualizar depois

Para atualizar a aplicacao depois do primeiro deploy:

1. Faca as alteracoes no codigo.
2. Abra um pull request para `main`.
3. Depois do merge na `main`, o GitHub Actions faz o deploy automaticamente.

## Checklist para duplicar este template

Quando duplicar este repositorio para outro dashboard, ajuste:

- Nome do repositorio no GitHub.
- Dominio em `SITE_DOMAIN`.
- URL publica em `FRONTEND_URL`.
- Banco PostgreSQL de producao.
- Secrets de JWT e backend.
- Usuario e senha do admin inicial.
- Variaveis opcionais de OpenAI, NanoBanana e N8N, se o novo dashboard usar essas integracoes.
- Caddy/Nginx da VPS apontando para a porta `HTTP_PORT`.

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
