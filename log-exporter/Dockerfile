FROM debian:bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends \
    bash coreutils zip unzip awscli openssl ca-certificates tzdata util-linux \
    && rm -rf /var/lib/apt/lists/*
COPY log-exporter/export-logs.sh /app/
USER 1001
ENTRYPOINT ["bash", "/app/export-logs.sh"]
