# JIPipe CI base image - one base for everything JIPipe CI.
#
# Built and pushed by the `jipipe_ci_base_image` job in .gitlab-ci.yml when
# this file changes on master. Every CI job runs on this image, so pipelines
# never install packages at runtime and never depend on the Ubuntu or Alpine
# mirrors: a runner DNS outage cannot fail a build.
#
# When changing this file, bump anything version-pinned below deliberately:
# the gh and docker CLI downloads are pinned so a silent upstream change
# cannot break the image build.

FROM maven:3-eclipse-temurin-21-jammy

# Union of the package lists of all former job-specific before_scripts.
# nsis: the distribution packaging scripts build the Win64 installer with
# makensis. git lfs install bakes the LFS filter config for the root user,
# whose HOME the CI jobs inherit.
#
# The institute network filters *.ubuntu.com DNS (observed 2026-11-11: even
# external resolvers such as 8.8.8.8 cannot resolve archive.ubuntu.com, so
# this is a network-level filter, not a resolver outage). Sources therefore
# point at an official German mirror that is reachable from the runners.
RUN sed -i 's|http://archive.ubuntu.com/ubuntu/|https://mirror.netcologne.de/ubuntu/|g; s|http://security.ubuntu.com/ubuntu/|https://mirror.netcologne.de/ubuntu/|g' /etc/apt/sources.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends \
        nsis git git-lfs zip unzip wget curl jq \
    && rm -rf /var/lib/apt/lists/* \
    && git lfs install

# GitHub CLI from the official static build. jammy's gh package is too old
# for the `gh release view --json` calls used by the publish jobs.
RUN curl -fsSL https://github.com/cli/cli/releases/download/v2.40.0/gh_2.40.0_linux_amd64.tar.gz \
        | tar xz -C /usr/local --strip 1

# Docker CLI matching the docker:24-dind service used by docker_build_push.
RUN curl -fsSL https://download.docker.com/linux/static/stable/x86_64/docker-24.0.9.tgz \
        | tar xz -C /usr/local/bin --strip 1 docker/docker
