FROM ubuntu:24.04

RUN apt-get update && apt-get install -y --no-install-recommends \
    libxrender1 \
    libxtst6 \
    libxi6 \
    libxext6 \
    libx11-6 \
    libgl1 \
    libglib2.0-0 \
    libfontconfig1 \
    libfreetype6 \
    libnspr4 \
    libnss3 \
    libdbus-1-3 \
    libatk1.0-0 \
    libatk-bridge2.0-0 \
    libcups2 \
    libdrm2 \
    libxcb1 \
    libxcomposite1 \
    libxdamage1 \
    libxfixes3 \
    libxrandr2 \
    libgbm1 \
    libpango-1.0-0 \
    libcairo2 \
    libasound2t64 \
    libopencv-dev \
    && rm -rf /var/lib/apt/lists/*

ARG PACKAGE_PATH

COPY ${PACKAGE_PATH} /tmp/jipipe.tar.gz
RUN mkdir -p /opt/jipipe && \
    tar -xzf /tmp/jipipe.tar.gz -C /opt/jipipe --strip-components=1 && \
    rm /tmp/jipipe.tar.gz && \
    chmod +x /opt/jipipe/jipipe-linux-x64

ENTRYPOINT ["/opt/jipipe/jipipe-linux-x64"]
