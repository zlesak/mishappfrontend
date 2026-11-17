# Running MISH APP

The whole application runs from this directory — UI, backend, database and identity provider.
No other repository is needed.

## Quick start

```bash
cp .env.example .env
docker compose up -d --build
```

Then open <https://localhost> and accept the self-signed certificate warning.

Test accounts come from the imported Keycloak realm:

| User    | Password   | Role    |
|---------|------------|---------|
| `alice` | `password` | teacher |
| `bart`  | `password` | student |

`.env.example` is a development file with throwaway credentials. For anything reachable from a
network start from [`.env.production.example`](.env.production.example) instead — it has no usable
values, so nothing can be left at a default by accident.

## What runs

| Service       | Purpose                                                        |
|---------------|----------------------------------------------------------------|
| `gateway`     | nginx: TLS termination and the single entry point              |
| `app`         | the MISH application — Vaadin UI and backend in one process    |
| `mongo`       | application data and uploaded model files (GridFS)             |
| `keycloak`    | login, roles and the users                                     |
| `keycloak-db` | PostgreSQL behind Keycloak                                     |
| `cert-init`   | one-off: generates the gateway's self-signed certificate       |

Only the gateway publishes ports. MongoDB and Keycloak are reachable from the other containers only.

## Configuration

Every credential is required — `docker compose` refuses to start and names the missing variable
rather than falling back to a default. The application does the same: it has no built-in database
URL or client secret, so a misconfigured deployment fails at start-up instead of silently running
against the wrong database.

`KEYCLOAK_COMMAND` works the same way and is the one variable that is not a credential:

| Environment | Value                                                | Why                                                                                     |
|-------------|------------------------------------------------------|-----------------------------------------------------------------------------------------|
| development | `start-dev --import-realm --http-relative-path=/auth` | Dev mode, and the demo realms in `keycloak/realms` are imported on first start           |
| production  | `start --http-enabled=true --http-relative-path=/auth` | Keeps the checks dev mode turns off, and without `--import-realm` the demo users stay out |

Keycloak is told its public URL explicitly (`KC_HOSTNAME_URL`) and runs with both hostname-strict
settings on, so the issuer, the login redirect and the token endpoint it advertises cannot be
steered by a forged `Host` header. TLS is terminated at the gateway, which is why Keycloak itself
serves plain HTTP on the internal network.

The application container runs as an unprivileged user (`mish`, uid 1001), not as root.

Generate secrets with:

```bash
openssl rand -base64 32
```

## Using the hostname `mish`

The end-to-end tests and the production deployment use a real hostname. To match that locally, add

```
127.0.0.1 mish
```

to your hosts file, then set `EXTERNAL_BASE_URL=https://mish` and `EXTERNAL_HOSTNAME=mish` in `.env`
and restart with `docker compose up -d`.

The gateway also answers to `mish` **inside** the Docker network. That matters because Keycloak
issues tokens under the browser-facing URL, and the application has to reach the same host.

## Common tasks

```bash
docker compose logs -f app
```

```bash
docker compose down
```

Add `-v` to `down` to also drop the database and the generated certificate.

## Using a real certificate

Replace `fullchain.pem` and `privkey.pem` in the `gateway-certs` volume, then restart the gateway.
`cert-init` only generates a certificate when none is present, so it will leave yours alone.

```bash
docker run --rm -v mish_gateway-certs:/certs -v /etc/letsencrypt/live/DOMAIN:/le:ro alpine \
  sh -c "cp /le/fullchain.pem /certs/ && cp /le/privkey.pem /certs/"
docker compose restart gateway
```

## Production

See [docs/deployment.md](../docs/deployment.md) for deploying from GitHub, handling secrets and
running the stack on a Raspberry Pi, including the checklist to work through before going live.
