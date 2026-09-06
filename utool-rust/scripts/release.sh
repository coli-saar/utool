#!/usr/bin/env bash

set -euo pipefail

usage() {
  echo "Usage: $0 <version|vversion> [remote]" >&2
  echo "Example: $0 v4.0.0-alpha3" >&2
}

if [[ $# -lt 1 || $# -gt 2 ]]; then
  usage
  exit 2
fi

version=${1#v}
tag="v$version"
remote=${2:-origin}

if [[ ! $version =~ ^[0-9]+\.[0-9]+\.[0-9]+([+-][0-9A-Za-z.-]+)?$ ]]; then
  echo "Invalid semantic version: $version" >&2
  exit 2
fi

repo_root=$(git rev-parse --show-toplevel)
cd "$repo_root"

version_files=(
  utool-rust/README.md
  utool-rust/Cargo.toml
  utool-rust/Cargo.lock
  utool-rust/desktop/package.json
  utool-rust/desktop/package-lock.json
  utool-rust/desktop/src-tauri/Cargo.toml
  utool-rust/desktop/src-tauri/Cargo.lock
  utool-rust/desktop/src-tauri/tauri.conf.json
)

if [[ -n $(git status --porcelain -- "${version_files[@]}") ]]; then
  echo "A release-managed version file already has changes:" >&2
  git status --short -- "${version_files[@]}" >&2
  echo "Commit or stash those files first. Changes elsewhere are allowed." >&2
  exit 1
fi

if git rev-parse --verify --quiet "refs/tags/$tag" >/dev/null; then
  echo "Tag already exists locally: $tag" >&2
  exit 1
fi

remote_url=$(git remote get-url "$remote")
if [[ ! $remote_url =~ github\.com[:/] ]]; then
  echo "Remote '$remote' is not a GitHub remote: $remote_url" >&2
  exit 1
fi

if git ls-remote --exit-code --tags "$remote" "refs/tags/$tag" >/dev/null 2>&1; then
  echo "Tag already exists on $remote: $tag" >&2
  exit 1
fi

current_version=$(node -p "require('./utool-rust/desktop/src-tauri/tauri.conf.json').version")
if [[ $current_version == "$version" ]]; then
  echo "The app is already at version $version." >&2
  exit 1
fi

VERSION="$version" CURRENT_VERSION="$current_version" node <<'NODE'
const fs = require("fs");

const version = process.env.VERSION;
const currentVersion = process.env.CURRENT_VERSION;
const files = new Map([
  ["utool-rust/Cargo.toml", 1],
  ["utool-rust/Cargo.lock", 1],
  ["utool-rust/desktop/package.json", 1],
  ["utool-rust/desktop/package-lock.json", 2],
  ["utool-rust/desktop/src-tauri/Cargo.toml", 1],
  ["utool-rust/desktop/src-tauri/Cargo.lock", 2],
  ["utool-rust/desktop/src-tauri/tauri.conf.json", 1],
]);

const readmeFile = "utool-rust/README.md";
const readme = fs.readFileSync(readmeFile, "utf8");
const downloadsPattern = /<!-- release-downloads:start version=([^ ]+) -->([\s\S]*?)<!-- release-downloads:end -->/;
const downloads = readme.match(downloadsPattern);
if (!downloads) {
  throw new Error(`${readmeFile}: release downloads block not found`);
}
if (downloads[1] !== currentVersion) {
  throw new Error(
    `${readmeFile}: download version is ${downloads[1]}, expected ${currentVersion}`,
  );
}
const updatedDownloads = downloads[0].split(currentVersion).join(version);

const contents = new Map();
for (const [file, expectedCount] of files) {
  const text = fs.readFileSync(file, "utf8");
  const occurrences = text.split(currentVersion).length - 1;
  if (occurrences !== expectedCount) {
    throw new Error(
      `${file}: expected ${expectedCount} occurrence(s) of ${currentVersion}, found ${occurrences}`,
    );
  }
  contents.set(file, text);
}

for (const [file, text] of contents) {
  fs.writeFileSync(file, text.split(currentVersion).join(version));
}
fs.writeFileSync(readmeFile, readme.replace(downloadsPattern, updatedDownloads));
NODE

manifest_versions=(
  "$(node -p "require('./utool-rust/desktop/src-tauri/tauri.conf.json').version")"
  "$(node -p "require('./utool-rust/desktop/package.json').version")"
  "$(sed -n 's/^version = "\(.*\)"/\1/p' utool-rust/Cargo.toml | head -1)"
  "$(sed -n 's/^version = "\(.*\)"/\1/p' utool-rust/desktop/src-tauri/Cargo.toml | head -1)"
)

for manifest_version in "${manifest_versions[@]}"; do
  if [[ $manifest_version != "$version" ]]; then
    echo "Version update verification failed: found $manifest_version, expected $version" >&2
    exit 1
  fi
done

git diff --check -- "${version_files[@]}"
git commit --only -m "Release $tag" -- "${version_files[@]}"
git tag -a "$tag" -m "Utool $version"
git push "$remote" "$tag"

echo "Released $tag to $remote."
