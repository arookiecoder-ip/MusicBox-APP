#!/usr/bin/env bash
# Resolve the pinned Robolectric 4.14.1 test SDK before its Java downloader runs.
set -euo pipefail
sdk_version=9-robolectric-4913185-2-i7
sdk_name=android-all-instrumented-$sdk_version
sdk_path=org/robolectric/android-all-instrumented/$sdk_version
sdk_cache="$HOME/.m2/repository/$sdk_path"
sdk_temp=$(mktemp -d)
trap 'rm -rf "$sdk_temp"' EXIT
mkdir -p "$sdk_cache"
for extension in pom jar; do
  artifact="$sdk_name.$extension"
  for suffix in '' .sha512; do
    curl --fail --location --retry 3 --retry-all-errors --output "$sdk_temp/$artifact$suffix" "https://repo.maven.apache.org/maven2/$sdk_path/$artifact$suffix"
  done
  digest=$(tr -d '[:space:]' < "$sdk_temp/$artifact.sha512")
  [[ "$digest" =~ ^[0-9a-fA-F]{128}$ ]]
  (cd "$sdk_temp" && printf '%s  %s\n' "$digest" "$artifact" | sha512sum --check -)
  cp "$sdk_temp/$artifact" "$sdk_temp/$artifact.sha512" "$sdk_cache/"
done
