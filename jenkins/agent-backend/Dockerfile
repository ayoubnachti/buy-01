# Backend build agent: Maven tests (through each service's ./mvnw),
# Docker image builds and deploys. JDK 21 comes from the base image.
# Every stage is multi-arch (amd64 + arm64).

# Source of the static Docker CLI + compose/buildx plugins
FROM docker:29-cli AS docker-cli

FROM jenkins/ssh-agent:jdk21

COPY --from=docker-cli /usr/local/bin/docker /usr/local/bin/docker
COPY --from=docker-cli /usr/local/libexec/docker/cli-plugins /usr/local/libexec/docker/cli-plugins

# Owned by jenkins so the named volume mounted here starts writable
RUN install -d -o jenkins -g jenkins /home/jenkins/.m2

COPY docker-socket-access.sh /usr/local/bin/docker-socket-access
RUN chmod 755 /usr/local/bin/docker-socket-access
ENTRYPOINT ["docker-socket-access"]
