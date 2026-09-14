#!/usr/bin/env bash
set -Eeuo pipefail
umask 077
export LC_ALL=C TZ="${EXPORT_TIMEZONE:-UTC}"
export AWS_EC2_METADATA_DISABLED=true AWS_RETRY_MODE=standard AWS_MAX_ATTEMPTS=5 AWS_PAGER=""
for name in S3_ENDPOINT S3_BUCKET AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY POD_NAMESPACE RELEASE_NAME; do
    [[ -n ${!name:-} ]] || { echo "Missing configuration: $name" >&2; exit 1; }
done
source_dir=${LOG_DIRECTORY:-/work/logs}
day=${EXPORT_DATE:-$(date -d 'yesterday 12:00' +%F)}
[[ $day =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]] || { echo 'Invalid EXPORT_DATE' >&2; exit 1; }
date -d "$day" +%F > /dev/null # Reject invalid calendar dates.
endpoint=$S3_ENDPOINT
[[ $endpoint == *://* ]] || endpoint="https://$endpoint"
[[ $endpoint == https://* ]] || { echo 'S3 endpoint must use HTTPS' >&2; exit 1; }

# Also serialize manual runs. Kernel releases the lock when a failed pod exits.
exec 9>"$source_dir/.access-log-export.lock"
flock -n 9 || { echo 'Another export is running' >&2; exit 1; }
scratch=$(mktemp -d)
trap 'rm -rf -- "$scratch"' EXIT
mkdir "$scratch/archive"
midnight=$(date -d 'today 00:00:00' +%s)
export AWS_CONFIG_FILE="$scratch/aws-config"
printf '[default]\nregion = %s\ns3 =\n    addressing_style = path\n' "${AWS_DEFAULT_REGION:-ca-central-1}" > "$AWS_CONFIG_FILE"
printf 'date\ttimezone\n%s\t%s\n\nfile\tbytes\tsha256\n' "$day" "$TZ" > "$scratch/archive/manifest.tsv"
shopt -s nullglob
# Select all Quarkus dated rotations (including numeric collision suffixes).
# Undated access logs require a modification time before today's midnight.
# No log-record timestamps are parsed.
files=("$source_dir"/access-security*.log)
declare -a candidates=() identities=() hashes=() signatures=()
for path in "${files[@]}"; do
    name=${path##*/}
    [[ -f $path && ! -L $path ]] || { echo 'Unexpected log file type' >&2; exit 1; }
    [[ $name =~ ^[A-Za-z0-9._-]+$ ]] || { echo 'Unexpected log filename' >&2; exit 1; }
    identity=$(stat -c '%d:%i' -- "$path")
    signature=$(stat -c '%d:%i:%s:%y:%z' -- "$path")
    if [[ ! $name =~ ^access-security.*[0-9]{4}-[0-9]{2}-[0-9]{2}(\.[0-9]+)?\.log$ ]]; then
        (( $(stat -c '%Y' -- "$path") < midnight )) || continue
    fi
    size=$(stat -c '%s' -- "$path")
    cp -- "$path" "$scratch/archive/$name"
    [[ $(stat -c '%s' "$scratch/archive/$name") == "$size" ]] || { echo 'Log changed during snapshot' >&2; exit 1; }
    [[ $(stat -c '%d:%i' -- "$path") == "$identity" ]] || { echo 'Log rotated; retry export' >&2; exit 1; }
    [[ $(stat -c '%d:%i:%s:%y:%z' -- "$path") == "$signature" ]] || { echo 'Log changed; retry export' >&2; exit 1; }
    checksum=$(sha256sum "$scratch/archive/$name"); checksum=${checksum%% *}
    printf '%s\t%s\t%s\n' "$name" "$size" "$checksum" >> "$scratch/archive/manifest.tsv"
    candidates+=("$path"); identities+=("$identity"); hashes+=("$checksum")
    signatures+=("$signature")
done
(( ${#candidates[@]} > 0 )) || { echo "No eligible access logs; no upload or deletion"; exit 0; }
# Fixed ZIP timestamps make repeated exports of identical files reproducible.
find "$scratch/archive" -type f -exec touch -t 198001020000 {} +
(cd "$scratch/archive" && zip -X -q "$scratch/access-logs.zip" -- *)
archive="$scratch/access-logs.zip"
unzip -t "$archive" > /dev/null
sha=$(sha256sum "$archive"); sha=${sha%% *}
bytes=$(stat -c '%s' "$archive")
(( bytes <= 5368709120 )) || { echo 'Archive exceeds S3 single PUT limit' >&2; exit 1; }
md5=$(openssl dgst -md5 -binary "$archive" | openssl base64 -A)
prefix=${S3_PREFIX:-access-logs}; prefix=${prefix#/}; prefix=${prefix%/}
key="${prefix:+$prefix/}$POD_NAMESPACE/access-logs_$day-$sha.zip"
aws --endpoint-url "$endpoint" s3api put-object --bucket "$S3_BUCKET" --key "$key" \
    --body "$archive" --content-type application/zip --content-md5 "$md5" --metadata "sha256=$sha" > /dev/null
verification=$(aws --endpoint-url "$endpoint" s3api head-object \
    --bucket "$S3_BUCKET" --key "$key" --query '[ContentLength,Metadata.sha256]' --output text)
read -r remote_size remote_sha <<< "$verification"
[[ $remote_size == "$bytes" && $remote_sha == "$sha" ]] || { echo 'Upload verification failed; source logs retained' >&2; exit 1; }
deleted=0
if [[ ${DELETE_UPLOADED_LOGS:-true} == true ]]; then
    for i in "${!candidates[@]}"; do
        path=${candidates[i]}
        [[ -f $path && ! -L $path && $(stat -c '%d:%i' -- "$path") == "${identities[i]}" ]] || continue
        [[ $(stat -c '%d:%i:%s:%y:%z' -- "$path") == "${signatures[i]}" ]] || continue
        current=$(sha256sum -- "$path"); current=${current%% *}
        [[ $current == "${hashes[i]}" ]] || continue
        [[ $(stat -c '%d:%i:%s:%y:%z' -- "$path") == "${signatures[i]}" ]] || continue
        rm -- "$path"
        deleted=$((deleted + 1))
    done
fi
echo "Verified ${#candidates[@]} access-log files for $day at s3://$S3_BUCKET/$key; deleted $deleted completed source files"
