# Frontend build agent: Node only. Vitest runs in Node with jsdom,
# so no browser is needed. Every stage is multi-arch (amd64 + arm64).

# Source of the Node 22 runtime (Angular 21 needs Node ^20.19, ^22.12 or ^24)
FROM node:22-bookworm-slim AS node

FROM jenkins/ssh-agent:jdk21

COPY --from=node /usr/local/bin/node /usr/local/bin/node
COPY --from=node /usr/local/lib/node_modules /usr/local/lib/node_modules
RUN ln -s ../lib/node_modules/npm/bin/npm-cli.js /usr/local/bin/npm \
  && ln -s ../lib/node_modules/npm/bin/npx-cli.js /usr/local/bin/npx \
  && install -d -o jenkins -g jenkins /home/jenkins/.npm

# E2E: Chromium and its system libraries, installed here as root because the pipeline runs
# as jenkins and cannot apt-get. Must be the exact @playwright/test version from
# frontend/package-lock.json: each Playwright release expects its own browser build.
# The pipeline sets PLAYWRIGHT_BROWSERS_PATH too, since SSH sessions don't inherit this ENV.
ARG PLAYWRIGHT_VERSION=1.65.0
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
RUN npx -y playwright@${PLAYWRIGHT_VERSION} install --with-deps chromium \
  && chmod -R a+rx /ms-playwright \
  && rm -rf /root/.npm