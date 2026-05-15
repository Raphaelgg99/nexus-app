# Deploy em VPS

## Requisitos no servidor

- Docker Engine
- Docker Compose plugin
- Acesso SSH a partir do GitHub Actions

## Secrets do GitHub

Configure estes secrets no repositório:

- `VPS_HOST`
- `VPS_USER`
- `VPS_SSH_KEY`
- `VPS_PORT` opcional, padrão `22`
- `VPS_APP_DIR` opcional, padrão `/opt/nexus-app`
- `GHCR_USERNAME` opcional, necessário se o pacote GHCR estiver privado
- `GHCR_TOKEN` opcional, token com permissão `read:packages`
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`
- `DB_SSL_MODE`
- `DB_JDBC_PARAMS` opcional. Use `&prepareThreshold=0` no Supabase Transaction Pooler.
- `FRONTEND_URL`
- `JWT_SECRET`
- `SECRET_KEY`
- `JWT_EXPIRATION`
- `INITIAL_ADMIN_USERNAME`
- `INITIAL_ADMIN_PASSWORD`
- `OPENAI_ACCESS_TOKEN`
- `NANOBANANA_ACCESS_TOKEN`

## Observações

O `docker-compose.production.yml` não cria Postgres. O backend conecta diretamente no Supabase usando as variáveis acima.

Se você usar Nginx Proxy Manager, Traefik ou Caddy na VPS, mude `HTTP_PORT` para uma porta interna, como `8088`, e aponte o proxy para ela.
