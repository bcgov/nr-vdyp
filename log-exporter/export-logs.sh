#!/usr/bin/env bash
set -Eeuo pipefail
export LC_ALL=C TZ="${EXPORT_TIMEZONE:-America/Vancouver}"
export AWS_EC2_METADATA_DISABLED=true AWS_RETRY_MODE=standard AWS_MAX_ATTEMPTS=5 AWS_PAGER=""
for name in S3_ENDPOINT S3_BUCKET AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY POD_NAMESPACE RELEASE_NAME; do
    [[ -n ${!name:-} ]] || { echo "Missing configuration: $name" >&2; exit 1; }
done
source_dir=${LOG_DIRECTORY:-/work/logs}
day=${EXPORT_DATE:-$(date -d 'yesterday 12:00' +%F)}
[[ $day =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]] || { echo 'Invalid EXPORT_DATE' >&2; exit 1; }
start=$(date -d "$day 00:00:00" +%s)
next_day=$(date -d "$day tomorrow" +%F)
end=$(date -d "$next_day 00:00:00" +%s)
endpoint=$S3_ENDPOINT
[[ $endpoint == *://* ]] || endpoint="https://$endpoint"
[[ $endpoint == https://* ]] || { echo 'S3 endpoint must use HTTPS' >&2; exit 1; }

# Also serialize manual runs. Kernel releases the lock when a failed pod exits.
exec 9>"$source_dir/.access-log-export.lock"
flock -n 9 || { echo 'Another export is running' >&2; exit 1; }
scratch=$(mktemp -d)
trap 'rm -rf -- "$scratch"' EXIT
mkdir "$scratch/archive"
export AWS_CONFIG_FILE="$scratch/aws-config"
printf '[default]\nregion = %s\ns3 =\n    addressing_style = path\n' "${AWS_DEFAULT_REGION:-ca-central-1}" > "$AWS_CONFIG_FILE"
script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
printf 'date\ttimezone\n%s\t%s\n\nfile\trecords\tsha256\n' "$day" "$TZ" > "$scratch/archive/manifest.tsv"
shopt -s nullglob
files=("$source_dir"/access-security*.log)
(( ${#files[@]} )) || { echo 'No access logs found' >&2; exit 1; }
declare -a candidates=() identities=() hashes=()
records=0
for path in "${files[@]}"; do
    [[ -f $path && ! -L $path ]] || { echo 'Unexpected log file type' >&2; exit 1; }
    name=${path##*/}
    [[ $name =~ ^[A-Za-z0-9._-]+$ ]] || { echo 'Unexpected log filename' >&2; exit 1; }
    identity=$(stat -c '%d:%i' -- "$path")
    size=$(stat -c '%s' -- "$path")
    head -c "$size" -- "$path" > "$scratch/snapshot"
    [[ $(stat -c '%s' "$scratch/snapshot") == "$size" ]] || { echo 'Log truncated during snapshot' >&2; exit 1; }
    if (( size > 0 )) && [[ $(tail -c 1 "$scratch/snapshot" | od -An -tu1 | tr -d ' ') != 10 ]]; then
        echo 'Incomplete record; retry export' >&2; exit 1
    fi
    gawk -v start="$start" -v end="$end" -v counts="$scratch/counts" -f "$script_dir/filter-day.awk" \
        "$scratch/snapshot" > "$scratch/archive/$name"
    read -r total selected < "$scratch/counts"
    [[ $(stat -c '%d:%i' -- "$path") == "$identity" ]] || { echo 'Log rotated; retry export' >&2; exit 1; }
    if (( selected == 0 )); then rm -- "$scratch/archive/$name"; continue; fi
    records=$((records + selected))
    checksum=$(sha256sum "$scratch/archive/$name"); checksum=${checksum%% *}
    printf '%s\t%s\t%s\n' "$name" "$selected" "$checksum" >> "$scratch/archive/manifest.tsv"
    # Only date-suffixed Quarkus rotated files can be removed, never active files.
    # If a file spans multiple days, this day's ZIP does not cover the entire file.
    if (( selected == total )) && [[ $name =~ [0-9]{4}-[0-9]{2}-[0-9]{2}(\.[0-9]+)?\.log$ ]]; then
        candidates+=("$path"); identities+=("$identity"); hashes+=("$checksum")
    fi
done
(( records > 0 )) || { echo "No records for $day; no upload or deletion"; exit 0; }
# Fixed ZIP timestamps and content-addressed keys protect earlier exports after cleanup.
find "$scratch/archive" -type f -exec touch -t 198001020000 {} +
(cd "$scratch/archive" && zip -X -q "$scratch/access-logs.zip" -- *)
archive="$scratch/access-logs.zip"
unzip -t "$archive" > /dev/null
sha=$(sha256sum "$archive"); sha=${sha%% *}
bytes=$(stat -c '%s' "$archive")
(( bytes <= 5368709120 )) || { echo 'Archive exceeds S3 single PUT limit' >&2; exit 1; }
md5=$(openssl dgst -md5 -binary "$archive" | openssl base64 -A)
prefix=${S3_PREFIX:-access-logs}; prefix=${prefix#/}; prefix=${prefix%/}
key="${prefix:+$prefix/}$POD_NAMESPACE/access-logs_$day.zip"
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
        current=$(sha256sum -- "$path"); current=${current%% *}
        [[ $current == "${hashes[i]}" ]] || continue
        rm -- "$path"
        deleted=$((deleted + 1))
    done
fi
echo "Verified $records records for $day at s3://$S3_BUCKET/$key; deleted $deleted completed source files"
