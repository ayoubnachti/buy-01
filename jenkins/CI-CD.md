# CI/CD with Jenkins and SonarQube

Every change goes through a pull request. Jenkins tests it and SonarQube analyses it; if a test or the quality gate fails, the PR can't be merged. Every push to `main` is built, tested, analysed, deployed and reported by email. If a deploy is unhealthy, the previous release is restored automatically.

Live: app at `https://shop.ayoubnachti.dev`, Jenkins at `https://jenkins.ayoubnachti.dev`, SonarQube at `https://sonar.ayoubnachti.dev`.

## Architecture

```
GitHub ──webhook (push, PR)──► Jenkins controller (0 executors, configured by casc.yaml)
   ▲                               │ SSH
   │ commit status          ┌──────┴─────────────────┐
   │ (pr-head check)  agent-backend             agent-frontend
   │                  JDK 21, Docker CLI,       Node 22
   │                  host Docker socket
   │                        │        │                 │
   └────────────────────────┘        ▼                 ▼
                                 SonarQube ◄── analysis + quality gate ──┘
                                 (+ PostgreSQL)
                                     │
                       docker compose ──► app stack on the same host
                                          (Eureka, gateway, 3 services, MongoDB, frontend)
```

- **Jenkins and SonarQube are one compose project** (`jenkins/`), separate from the app's `compose.yml`, so a deploy or rollback can never restart them. Both are CI infrastructure; agents reach SonarQube by name (`http://sonarqube:9000`) on the same Docker network.
- **Configuration as code:** `jenkins/casc.yaml` creates the admin user, security, both agents, credentials, the email settings and the SonarQube server connection. No setup wizard.
- **Secrets** are never in git. They live in `jenkins/.env` (git-ignored) and become Jenkins credentials at startup.
- **Multi-arch images only**, so the same files run on the arm64 server and on x86 laptops.

## Pipeline (`Jenkinsfile`)

The job is a **Multibranch Pipeline** with the GitHub Branch Source: it builds `main` and every pull request (`PR-*`). Other branches are only built once they have a PR.

| Stage | Agent | What it does |
|---|---|---|
| **Prepare** | — | Tells SonarQube whether this is a PR (`sonar.pullrequest.*`) or a branch analysis |
| **Backend tests** / **Frontend tests** | backend / frontend, **in parallel** | Backend: `./mvnw clean test` with JaCoCo coverage for the 5 Spring services (with a throwaway MongoDB). Frontend: `npm ci` + `ng test --coverage` (Vitest). JUnit results are published. |
| **Backend analysis** / **Frontend analysis** | same agent and workspace as the tests | SonarQube analysis per service (6 projects). `sonar.qualitygate.wait=true`: the scanner fails the stage if the **quality gate** fails. |
| **Build images** | backend | `docker compose build`, images tagged with the build number — **`main` only** |
| **Deploy** | backend | `docker compose up -d --wait`: succeeds only when every container's healthcheck reports healthy — **`main` only** |
| *post* | | Email on success and on failure |

Any failing stage stops the pipeline. Builds of the same branch or PR run one at a time.

**Nightly scan:** `main` is also built every night (`H 3 * * *`): tests and analysis run, so SonarQube's dashboards stay current even without pushes; build and deploy are skipped (the code is already deployed).

**Test reports:** backend (Surefire) and frontend (Vitest `junit` reporter) results are published with the `junit` step, even when tests fail. Each build keeps them under *Test Result*; the last 20 builds are kept.

**Rollback:** after a healthy deploy, the images are also tagged `last-good`. If a deploy is unhealthy, the pipeline redeploys `last-good`, marks the build failed, and the email says "(rolled back)". Only images are rolled back; MongoDB data is untouched. Older images are deleted after each healthy deploy.

**Frontend tests use Vitest**, Angular's default runner (Karma is deprecated). The subject's Jasmine/Karma is a suggestion; Vitest runs in Node with jsdom, so the agent needs no browser.

**Distributed builds:** backend and frontend tests and analysis run at the same time on two separate agents, selected by label (`backend`, `frontend`).

## Code quality with SonarQube

### Setup

- **Image:** `mc1arke/sonarqube-with-community-branch-plugin:26.5.0.122743-community` (amd64 + arm64). SonarQube Community Build analyses one branch only; this image adds the **Community Branch Plugin**, which enables branch and **pull-request analysis**. Third-party, not supported by SonarSource, so the version is pinned: the plugin only works with its matching SonarQube version.
- **Database:** PostgreSQL 17 (`sonar-db`), not the embedded H2 (evaluation only, can't be upgraded).
- **Projects:** one per service — `safe-zone-api-gateway`, `-discovery-server`, `-user-service`, `-product-service`, `-media-service`, `-frontend`. Each backend service has its own `pom.xml` without a parent, so separate projects are the natural fit. Projects are created by their first analysis.
- **Coverage:** JaCoCo for the backend (invoked by full Maven coordinates, no pom changes), Vitest v8 coverage (`lcov`) for the frontend.
- **Jenkins connection:** SonarQube Scanner plugin, server `sonarqube` → `http://sonarqube:9000`, authenticated with a **Global Analysis Token** stored as the `sonar-token` credential. The token can run analyses and create projects, nothing else.

### Quality gate `SafeZone` (default for all projects)

Copied from *Sonar way*; conditions apply to **new code** (a PR's changes, or changes on `main` since the previous version):

| Condition | Catches |
|---|---|
| Issues > 0 | any new vulnerability, bug or code smell, at any severity |
| Security Hotspots Reviewed < 100% | security-sensitive code nobody has reviewed |
| Coverage < 80% | new code without enough tests |
| Duplicated Lines > 3% | copy-pasted new code |

"Clean as you code": existing issues don't block builds, but nothing new may be added — and **touching a line makes its existing issues new**. SonarQube ignores the coverage and duplication conditions when a change has fewer than 20 new lines.

**How the pipeline fails:** the scanner waits for SonarQube to compute the gate (`sonar.qualitygate.wait=true`, 5 min timeout) and exits non-zero if it fails, so the analysis stage, the build and the GitHub check turn red, and `main` doesn't deploy.

**PR analysis detail:** the scanner compares the PR with its target branch in the local clone, so PR builds fetch `origin/main` first (`fetchTargetBranch()` in the `Jenkinsfile`). Without it, SonarQube sees 0 new lines and the gate passes on nothing.

### Review and approval process

- **Branch protection on `main`:** changes only through a pull request; the Jenkins check `continuous-integration/jenkins/pr-head` must pass (tests + quality gate); the branch must be up to date with `main` (the PR head is what gets tested); conversations must be resolved; force pushes and deletion are blocked; **nobody can bypass these rules, admins included**.
- **PR template** (`.github/pull_request_template.md`): the review checklist — green check, every SonarQube issue on the PR fixed or justified, hotspots reviewed, tests for new code, no secrets.
- **Every issue is either fixed or justified in SonarQube:** *False positive* or *Accepted*, always with a comment explaining why.
- **Approvals:** currently 0 required, because this is a solo project and GitHub never lets authors approve their own PR. Setting it to 1 is one checkbox in the branch protection rule once a second contributor exists.

### Issues handled

From the first analysis of `main` (48 issues, 4 projects with unreviewed hotspots):

| Finding | Action |
|---|---|
| Hard-coded seed passwords in `user-service` `DataSeeder` (2 Blocker, java:S6437) | **Fixed:** both demo seeders removed; users register through the app |
| `csrf.disable()` in 4 services (hotspots) | **Reviewed Safe:** stateless API, JWT sent in the `Authorization: Bearer` header from `localStorage`, no session or auth cookie, so CSRF doesn't apply |
| Duplicated claim name, `int` overflow pattern, method too complex (`media-service`) | **Fixed** |
| Empty test class, test without assertion | **Fixed:** real tests added for `media-service`; a dead test deleted |
| Accessibility issues in the frontend (`alt` texts, label, keyboard handler) | **Fixed** |
| `autocomplete` on password inputs with a dynamic `[type]` | **False positive:** the analyzer can't evaluate the binding; the token is correct |
| `role="dialog"` instead of `<dialog>` | **Accepted:** Bootstrap's documented ARIA pattern; migrating means restyling the component (follow-up) |

Remaining Medium/Low maintainability issues are tracked debt; the gate prevents new ones.

### Access control

- Admin password changed before SonarQube was exposed; **Force user authentication** on; default project visibility **private**. Anonymous requests get `401`:
  `curl -s -o /dev/null -w '%{http_code}\n' https://sonar.ayoubnachti.dev/api/components/search?qualifiers=TRK`
- Jenkins uses a Global Analysis Token, not the admin account.

## Run it locally

Requirements: Docker with Compose. SonarQube's Elasticsearch needs `vm.max_map_count >= 524288` on the Docker host:

```bash
sysctl vm.max_map_count                                   # Linux: check
sudo sysctl -w vm.max_map_count=524288                    # Linux: set (until reboot)
docker run --rm --privileged alpine sysctl -w vm.max_map_count=524288   # Docker Desktop (Windows/macOS): set in its VM
```

```bash
# 1. App settings (used by the deploy)
cp .env.example .env
#    set JWT_SECRET (openssl rand -base64 32) and SSL_KEYSTORE_PASSWORD

# 2. Jenkins + SonarQube settings
cp jenkins/.env.example jenkins/.env
ssh-keygen -t ed25519 -N "" -C jenkins-agent -f ~/.ssh/jenkins_agent
#    in jenkins/.env set:
#      JENKINS_ADMIN_PASSWORD
#      AGENT_SSH_KEY_B64=$(base64 -w0 ~/.ssh/jenkins_agent)
#      AGENT_SSH_PUBKEY=$(cat ~/.ssh/jenkins_agent.pub)
#      APP_ENV_B64=$(base64 -w0 .env)
#      GIT_USERNAME / GIT_TOKEN   (fine-grained token for the repo: Contents R, Metadata R,
#                                  Pull requests R, Commit statuses R/W)
#      SMTP_USERNAME / SMTP_APP_PASSWORD / NOTIFY_EMAIL  (Gmail app password)
#      SONAR_DB_PASSWORD=$(openssl rand -hex 24)
#      SONAR_TOKEN=placeholder    (replaced in step 4)

# 3. Start SonarQube first
docker compose -f jenkins/docker-compose.yml up -d sonar-db sonarqube
docker compose -f jenkins/docker-compose.yml logs -f sonarqube    # wait for "SonarQube is operational"
```

4. Open `http://localhost:9000`, log in as `admin` / `admin` and set a new password. Accept the third-party plugin notice. Then:
   - *Administration → Configuration → General Settings → Security*: **Force user authentication** on.
   - *Administration → Projects → Management*: default visibility **Private**.
   - *Quality Gates → Sonar way → Copy* → `SafeZone` → **Set as Default**.
   - *My Account → Security*: generate a **Global Analysis Token** and put it in `jenkins/.env` as `SONAR_TOKEN`.

```bash
# 5. Start Jenkins
docker compose -f jenkins/docker-compose.yml up -d --build
```

Open `http://localhost:8080` and log in as `admin`. Both agents appear under *Manage Jenkins → Nodes*; *Manage Jenkins → System* shows the `sonarqube` server.

**Create the job** (once): *New Item* → `safe-zone` → **Multibranch Pipeline**
- Branch Sources: **GitHub** → credentials `git-token` → repository URL
- Behaviours: keep the defaults (branches excluding PR branches; PRs from origin; fork PRs trusted only from users with write access) and add **Filter by name (with wildcards)** → include `main PR-*`
- Build Configuration: by Jenkinsfile, script path `Jenkinsfile`
- Scan Multibranch Pipeline Triggers: periodically, 1 day

Saving scans the repository and builds `main`. Without a public URL there is no webhook, so use **Scan Multibranch Pipeline Now** after pushing.

**On Gitea:** use the **Git** branch source with a Gitea token instead of the GitHub source. `main` builds, tests, analyses and deploys the same way (the `Jenkinsfile` only uses `checkout scm`); PR discovery and the PR status check are GitHub features.

## Server setup

- `jenkins/docker-compose.server.yml` removes the published ports and joins Jenkins and SonarQube to Caddy's `web` network (`https://jenkins.ayoubnachti.dev`, `https://sonar.ayoubnachti.dev`).
- GitHub webhook → `https://jenkins.ayoubnachti.dev/github-webhook/` (push and pull request events).
- The app is served by Caddy at `https://shop.ayoubnachti.dev`. The browser calls `/api/...` on the same domain; the frontend's nginx forwards it to the gateway inside Docker.
- Caddy serves a custom "temporarily down" page when a container is unreachable.
- Updating Jenkins or SonarQube is manual: `git pull`, then `docker compose -f jenkins/docker-compose.yml -f jenkins/docker-compose.server.yml up -d --build`.

## Demo

**The quality gate blocks a PR**
1. On a new branch, add a hard-coded credential and an unused field to a backend class, push, open a PR.
2. The PR build's *Backend analysis* fails with `QUALITY GATE STATUS: FAILED`; the GitHub check is red and **Merge** is disabled.
3. In SonarQube, the project's *Pull Requests* tab shows the new issue and the unreviewed hotspot.
4. Remove the lines and push: the PR rebuilds, the gate passes, the check turns green, merge.

**A failing test stops the pipeline**
1. Break an assertion in any test, push to a PR branch.
2. The tests stage turns red, analysis and deploy are skipped, and a "FAILED" email arrives.

**Automatic rollback** (needs a merge to `main`, so through a PR)
1. In `backend/user-service/src/main/resources/application.properties` set `server.port=8091`. Tests still pass, but the healthcheck on 8081 fails after deploy.
2. *Deploy* fails, the console shows `rolling back to last-good`, and the email says "(rolled back)".
3. `docker ps` shows every service on `:last-good`, all healthy.
4. Revert through another PR.

**Notifications:** every build sends "SUCCESS: ..." (with "deployed" only when a deploy happened) or "FAILED: ...", with links to the changes, test results and console. A failed quality gate fails the build, so it is reported as "FAILED".

## Security notes

- Jenkins is public: sign-up disabled, admin password generated, controller has 0 executors.
- The Docker socket is mounted **only** on `agent-backend`, never on the controller. It gives root-equivalent access to the host, an accepted tradeoff on a single-user server.
- Test values in the `Jenkinsfile` (JWT secret, keystore password) are test-only. Real secrets come from the `ecommerce-env` credential.