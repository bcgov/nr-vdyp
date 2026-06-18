#!/usr/bin/env sh
set -eu

: "${NATS_URL:=nats://localhost:4222}"

echo "Waiting for NATS at ${NATS_URL}..."

until nats server check connection --server "$NATS_URL" >/dev/null 2>&1; do
  sleep 1
done

echo "NATS is available."

apply_stream() {
  stream_name="$1"
  config_file="$2"

  if nats stream info "$stream_name" --server "$NATS_URL" >/dev/null 2>&1; then
    echo "Skipping stream ${stream_name} it already exists"
  else
    echo "Creating stream ${stream_name}"
    nats stream add "$stream_name" \
      --server "$NATS_URL" \
      --config "$config_file" \
      --defaults
  fi
}

apply_consumer() {
  stream_name="$1"
  consumer_name="$2"
  config_file="$3"

  if nats consumer info "$stream_name" "$consumer_name" --server "$NATS_URL" >/dev/null 2>&1; then
    echo "Skipping consumer ${consumer_name} on stream ${stream_name}, it already exists"
  else
    echo "Creating consumer ${consumer_name} on stream ${stream_name}"
    nats consumer add "$stream_name" "$consumer_name" \
      --server "$NATS_URL" \
      --config "$config_file" \
      --defaults
  fi
}

apply_stream "VDYP_BATCH_REQUESTS" "./streams/vdyp-batch-requests.json"

apply_consumer "VDYP_BATCH_REQUESTS" "VDYP_BATCH_WORKER" "./consumers/vdyp-batch-worker.json"

echo "JetStream configuration applied successfully."
