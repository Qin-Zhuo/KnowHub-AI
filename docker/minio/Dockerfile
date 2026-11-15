FROM golang:1.24.2-bookworm AS builder
ARG GOPROXY=https://goproxy.cn,direct
ENV GOPROXY=${GOPROXY}
WORKDIR /src
RUN git clone --depth 1 --branch RELEASE.2025-04-22T22-12-26Z https://github.com/minio/minio.git .
RUN --mount=type=cache,id=paismart-minio-go-mod,target=/go/pkg/mod \
    set -eu; \
    for attempt in 1 2 3; do \
      echo "Downloading Go dependencies: attempt $attempt of 3"; \
      if go mod download -x; then exit 0; fi; \
      if [ "$attempt" = 3 ]; then exit 1; fi; \
      sleep 5; \
    done
RUN --mount=type=cache,id=paismart-minio-go-mod,target=/go/pkg/mod \
    --mount=type=cache,id=paismart-minio-go-build,target=/root/.cache/go-build \
    CGO_ENABLED=0 GOMAXPROCS=2 go build -p 2 -trimpath -o /out/minio .

FROM debian:bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 minio \
    && useradd --uid 10001 --gid minio --create-home minio \
    && mkdir /data && chown minio:minio /data
COPY --from=builder /out/minio /usr/local/bin/minio
USER minio
EXPOSE 9000 9001
ENTRYPOINT ["/usr/local/bin/minio"]
