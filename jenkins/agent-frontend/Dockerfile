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
