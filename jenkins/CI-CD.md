# CI/CD with Jenkins

Every push to `main` is built, tested, deployed and reported by email.
If a test fails, nothing is deployed. If a deploy is unhealthy, the previous release is restored automatically.

Live: app at `https://shop.ayoubnachti.dev`, Jenkins at `https://jenkins.ayoubnachti.dev`.

## Architecture

```
GitHub ──webhook──► Jenkins controller (0 executors, configured by casc.yaml)
                        │ SSH
            ┌───────────┴────────────┐
      agent-backend              agent-frontend
      JDK 21, Docker CLI,        Node 22
      host Docker socket
            │
            └──► docker compose ──► app stack on the same host
                                    (Eureka, gateway, 3 services, MongoDB, frontend)
```

- **Jenkins is its own compose project** (`jenkins/`), separate from the app's `compose.yml`, so a deploy or rollback can never restart Jenkins.
- **Configuration as code:** `jenkins/casc.yaml` creates the admin user, security, both agents, credentials and the email settings. No setup wizard.
- **Secrets** are never in git. They live in `jenkins/.env` (git-ignored) and become Jenkins credentials at startup.
- **Multi-arch images only**, so the same files run on the arm64 server and on x86 laptops.

## Pipeline (`Jenkinsfile`)

| Stage | Agent | What it does |
|---|---|---|
| **Build & test** | both, **in parallel** | Backend: `./mvnw clean test` for the 5 Spring services (with a throwaway MongoDB). Frontend: `npm ci` + `ng test` (Vitest). JUnit results are published. |
| **Build images** | backend | `docker compose build`, images tagged with the build number |
| **Deploy** | backend | `docker compose up -d --wait`: succeeds only when every container's healthcheck reports healthy |
| *post* | | Email on success and on failure |

Any failing stage stops the pipeline. Builds run one at a time.

**Test reports:** backend (Surefire) and frontend (Vitest `junit` reporter) results are published with the `junit` step, even when tests fail. Each build keeps them under *Test Result*, with a trend chart on the job page; the last 20 builds are kept.

**Rollback:** after a healthy deploy, the images are also tagged `last-good`. If a deploy is unhealthy, the pipeline redeploys `last-good`, marks the build failed, and the email says "(rolled back)". Only images are rolled back; MongoDB data is untouched. Older images are deleted after each healthy deploy.

**Frontend tests use Vitest**, Angular's default runner (Karma is deprecated). The subject's Jasmine/Karma is a suggestion; Vitest runs in Node with jsdom, so the agent needs no browser.

**Bonus, distributed builds:** backend and frontend tests run at the same time on two separate agents, selected by label (`backend`, `frontend`).

## Run it locally

Requirements: Docker with Compose.

```bash
# 1. App settings (used by the deploy)
cp .env.example .env
#    set JWT_SECRET (openssl rand -base64 32) and SSL_KEYSTORE_PASSWORD

# 2. Jenkins settings
cp jenkins/.env.example jenkins/.env
ssh-keygen -t ed25519 -N "" -C jenkins-agent -f ~/.ssh/jenkins_agent
#    in jenkins/.env set:
#      JENKINS_ADMIN_PASSWORD
#      AGENT_SSH_KEY_B64=$(base64 -w0 ~/.ssh/jenkins_agent)
#      AGENT_SSH_PUBKEY=$(cat ~/.ssh/jenkins_agent.pub)
#      APP_ENV_B64=$(base64 -w0 .env)
#      GIT_USERNAME / GIT_TOKEN  (read-only token for the repo)
#      SMTP_USERNAME / SMTP_APP_PASSWORD / NOTIFY_EMAIL  (Gmail app password)

# 3. Start Jenkins
docker compose -f jenkins/docker-compose.yml up -d --build
```

Open `http://localhost:8080` and log in as `admin`. Both agents appear under *Manage Jenkins → Nodes*.

**Create the job** (once): *New Item* → `mr-jenk` → **Pipeline**
- Triggers: **GitHub hook trigger for GITScm polling**
- Pipeline: **Pipeline script from SCM** → Git → repository URL → credentials `git-token` → branch `*/main` → script path `Jenkinsfile`

Then **Build Now**. Without a public URL there is no webhook, so use *Build Now* (or tick *Poll SCM*).

## Server setup

- `jenkins/docker-compose.server.yml` removes the published port and joins Jenkins to Caddy's `web` network (`https://jenkins.ayoubnachti.dev`).
- GitHub webhook → `https://jenkins.ayoubnachti.dev/github-webhook/` (push events).
- The app is served by Caddy at `https://shop.ayoubnachti.dev`. The browser calls `/api/...` on the same domain; the frontend's nginx forwards it to the gateway inside Docker.
- Updating Jenkins itself is manual: `git pull`, then `docker compose -f jenkins/docker-compose.yml -f jenkins/docker-compose.server.yml up -d --build`.

## Demo

**A failing test stops the pipeline**
1. Break an assertion in any test, then push.
2. *Build & test* turns red, nothing is built or deployed, and a "FAILED" email arrives.
3. Revert and push to get back to green.

**Automatic rollback**
1. In `backend/user-service/src/main/resources/application.properties` set `server.port=8091`, then push. Tests still pass, but the healthcheck on 8081 fails after deploy.
2. *Deploy* fails, the console shows `rolling back to last-good`, and the email says "(rolled back)".
3. `docker ps` shows every service on `:last-good`, all healthy.
4. `git revert HEAD` and push.

**Notifications:** every build sends "SUCCESS: ... deployed" or "FAILED: ...", with links to the changes, test results and console.

## Security notes

- Jenkins is public: sign-up disabled, admin password generated, controller has 0 executors.
- The Docker socket is mounted **only** on `agent-backend`, never on the controller. It gives root-equivalent access to the host, an accepted tradeoff on a single-user server.
- Test values in the `Jenkinsfile` (JWT secret, keystore password) are test-only. Real secrets come from the `ecommerce-env` credential.
