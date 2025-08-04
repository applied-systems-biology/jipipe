# Dockerfile for JIPipe CLI
FROM ubuntu:24.04

# Install dependencies: Java 21, Java 8, xvfb, wget, tar
RUN apt-get update && apt-get install -y \
    openjdk-21-jre \
    openjdk-8-jdk \
    xvfb \
    wget \
    tar \
    && rm -rf /var/lib/apt/lists/*

# XVFB setup

COPY xvfb-startup.sh .
RUN sed -i 's/\r$//' xvfb-startup.sh
ARG RESOLUTION="1920x1080x24"
ENV XVFB_RES="${RESOLUTION}"
ARG XARGS=""
ENV XVFB_ARGS="${XARGS}"

# Set environment variables
ENV JIPIPE_VERSION=5.3.0
ENV JIPIPE_URL=https://github.com/applied-systems-biology/jipipe/releases/download/pom-jipipe-${JIPIPE_VERSION}/JIPipe-${JIPIPE_VERSION}-Prepackaged-Linux64.tar.gz
ENV JIPIPE_HOME=/opt/jipipe

# Download and extract JIPipe
RUN mkdir -p ${JIPIPE_HOME} \
    && wget -qO- ${JIPIPE_URL} | tar xz -C ${JIPIPE_HOME}

# Ensure binaries are executable
RUN chmod +x ${JIPIPE_HOME}/JIPipe-${JIPIPE_VERSION}/bin/ImageJ-linux64

# Working directory where users can mount input/output
WORKDIR /data

# ENTRYPOINT wrapper using xvfb
ENTRYPOINT ["/bin/bash", "/xvfb-startup.sh", "/opt/jipipe/JIPipe-5.3.0/bin/ImageJ-linux64", "--pass-classpath", "--full-classpath", "--main-class", "org.hkijena.jipipe.cli.JIPipeCLIMain"]

# Default to showing help if no args are passed
CMD []
