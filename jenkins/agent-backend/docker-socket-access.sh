#!/bin/sh
# Runs as root before sshd starts.
# Jenkins reaches this agent over SSH, and SSH sessions only get the groups
# listed in /etc/group, so compose's `group_add` never reaches the jenkins
# user. Instead, add jenkins to whichever group owns the mounted Docker
# socket. Its GID differs per host (Ubuntu server, Docker Desktop, ...).
set -e
sock=/var/run/docker.sock
if [ -S "$sock" ]; then
  gid=$(stat -c %g "$sock")
  group=$(getent group "$gid" | cut -d: -f1)
  if [ -z "$group" ]; then
    groupadd -g "$gid" docker-host
    group=docker-host
  fi
  usermod -aG "$group" jenkins
fi
exec setup-sshd "$@"
