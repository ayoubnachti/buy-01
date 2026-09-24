# Environment Configuration & HTTPS

## Setup

1. Copy `.env.example` to `.env` in the project root (next to `compose.yml`):
   ```bash
   cp .env.example .env
   ```
2. Fill in real values in `.env` — at minimum:
   - `JWT_SECRET` — generate a real random value, e.g. `openssl rand -base64 32`.
     **Must be identical for `api-gateway` and `user-service`** — the gateway
     only validates tokens, user-service is what issues them.
   - `SSL_KEYSTORE_PASSWORD` — must match whatever `-storepass` you used
     when generating `keystore.p12` (see below).
   - `CLOUDINARY_URL` — media-service's storage credential.
3. `.env` is already in `.gitignore` — never commit it. `.env.example` has
   no real secrets and is safe to commit as the template.

Docker Compose automatically reads a `.env` file sitting next to
`compose.yml` for `${VAR}` substitution — nothing else needs to be run to
load it.

## HTTPS (self-signed, local dev)

`api-gateway` now serves over HTTPS on port `8443` using a self-signed
certificate — this is the only service with TLS; every other service
stays on plain HTTP behind it, since nothing outside the Docker network
can reach them directly (only `gateway:8443` and `discovery:8761` are
exposed to the host).

### Generating the keystore (already done once — regenerate if needed)

```bash
keytool -genkeypair \
  -alias gateway \
  -keyalg RSA \
  -keysize 2048 \
  -storetype PKCS12 \
  -keystore keystore.p12 \
  -validity 3650 \
  -dname "CN=localhost, OU=Dev, O=Vendify" \
  -storepass changeit \
  -keypass changeit
```

Notes on the flags:
- `-storepass` / `-keypass` must be **identical** for a PKCS12 keystore —
  the format doesn't support a separate per-entry key password the way
  older JKS keystores did. `keytool` will warn (or reject) if they differ.
- `-dname "CN=localhost, ..."` sets all the identity fields non-interactively
  in one go — omit it and `keytool` prompts for each field one at a time,
  plus a yes/no confirmation, which is fine by hand but not reproducible
  in a script or a teammate following these instructions blind.
- If you're on **Windows using Git Bash**, this command has an advantage
  worth knowing about: `openssl`'s equivalent needs a `-subj` value starting
  with `/` (e.g. `/CN=localhost`), which Git Bash's automatic Unix-path
  conversion mangles before `openssl` ever sees it. `keytool`'s `-dname`
  value has no leading `/`, so it isn't affected by that conversion at all.

Update `SSL_KEYSTORE_PASSWORD` in `.env` to match `-storepass` if you
change it from `changeit`, and `SSL_KEYSTORE_PATH` if you move the file
somewhere other than the default `classpath:keystore.p12`.

Place `keystore.p12` at `api-gateway/src/main/resources/keystore.p12` —
Maven packages it into the built JAR from there, no separate volume
mount needed for the default classpath-based setup.

