# Deploy em VPS

## Requisitos no servidor

- Docker Engine
- Docker Compose plugin
- Acesso SSH a partir do GitHub Actions

## Secrets e Variables do GitHub

Configure estes secrets no repositório:

- `VPS_SSH_KEY`
- `GHCR_USERNAME` opcional, necessário se o pacote GHCR estiver privado
- `GHCR_TOKEN` opcional, token com permissão `read:packages`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `SECRET_KEY`
- `JWT_EXPIRATION`
- `INITIAL_ADMIN_USERNAME`
- `INITIAL_ADMIN_PASSWORD`
- `OPENAI_ACCESS_TOKEN`
- `NANOBANANA_ACCESS_TOKEN`

Configure estas variables no repositório:

- `VPS_HOST`
- `VPS_USER`
- `VPS_PORT` opcional, padrão `22`
- `VPS_APP_DIR` opcional, padrão `nexus-app` no home do usuário SSH
- `HTTP_PORT` opcional, padrão `8088`
- `SITE_DOMAIN` opcional, padrão `nexusivbedtech.sbs`
- `FRONTEND_URL`
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_SSL_MODE`
- `DB_JDBC_PARAMS` opcional. Use `&prepareThreshold=0` no Supabase Transaction Pooler.

## Observações

O `docker-compose.production.yml` não cria Postgres. O backend conecta diretamente no Supabase usando as variáveis acima.

O deploy do Nexus não publica HTTPS diretamente. A aplicação fica em `HTTP_PORT`, por padrão `8088`, e o Caddy existente da VPS deve rotear `https://nexusivbedtech.sbs` para `http://127.0.0.1:8088`.
