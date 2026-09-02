#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
output_dir="${1:-${repo_root}/_site}"

if [[ -z "${output_dir}" || "${output_dir}" == "/" || "${output_dir}" == "${repo_root}" ]]; then
  echo "Refusing unsafe website output directory: ${output_dir}" >&2
  exit 1
fi

if ! command -v pandoc >/dev/null 2>&1; then
  echo "pandoc is required to build the website." >&2
  exit 1
fi

if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven is required to build the API documentation." >&2
  exit 1
fi

if ! command -v python3 >/dev/null 2>&1; then
  echo "Python 3 is required to add the site navigation to the API documentation." >&2
  exit 1
fi

manual_sources=(
  intro.tex
  tutorial.tex
  operations.tex
  codecs.tex
  practice.tex
  building.tex
  conclusion.tex
)

temporary_dir="$(mktemp -d "${TMPDIR:-/tmp}/utool-manual.XXXXXX")"
trap 'rm -rf "${temporary_dir}"' EXIT

# The manual predates UTF-8. Convert a temporary copy so the historical source
# can remain untouched while modern tools receive valid UTF-8 input.
for source_file in "${repo_root}"/doc/*.tex "${repo_root}"/doc/*.sty "${repo_root}"/doc/*.bib; do
  iconv -f ISO-8859-1 -t UTF-8 "${source_file}" > "${temporary_dir}/$(basename "${source_file}")"
done

# Pandoc emits PDF figures as embedded objects. Equivalent PNG exports are
# already part of the manual sources and work better in web browsers.
perl -pi -e 's/chain\.pdf/chain.png/g; s/jh-extraction-mean\.pdf/jh-extraction-mean.png/g; s/jh-chart-mean\.pdf/jh-chart-mean.png/g; s/chart-vs-solutions\.pdf/chart-vs-solutions.png/g; s/\{lkb-integration\}/\{lkb-integration.png\}/g' \
  "${temporary_dir}"/*.tex
perl -pi -e 's/\\verb\?B\?/\\mathrm{B}/ if /\\exists X/' "${temporary_dir}/codecs.tex"

maven_arguments=(
  -q
  -DskipTests
  -Dsource=8
  -Ddoclint=none
)
if [[ -n "${MAVEN_REPO_LOCAL:-}" ]]; then
  maven_arguments+=("-Dmaven.repo.local=${MAVEN_REPO_LOCAL}")
fi

(
  cd "${repo_root}"
  mvn "${maven_arguments[@]}" org.apache.maven.plugins:maven-javadoc-plugin:3.12.0:javadoc
)

rm -rf -- "${output_dir}"
mkdir -p "${output_dir}/api" "${output_dir}/assets" "${output_dir}/manual"

cp "${repo_root}/website/index.html" "${output_dir}/index.html"
cp "${repo_root}/website/styles.css" "${output_dir}/styles.css"
cp "${repo_root}/website/manual.css" "${output_dir}/manual.css"
cp "${repo_root}/website/site-header.css" "${output_dir}/site-header.css"
cp "${repo_root}/website/assets/utool.png" "${output_dir}/assets/"
cp -R "${repo_root}/target/reports/apidocs/." "${output_dir}/api/"
python3 "${repo_root}/scripts/inject-javadoc-header.py" "${output_dir}/api" "${output_dir}"
cp \
  "${repo_root}/doc/ubench-empty.png" \
  "${repo_root}/doc/ubench-holesem.png" \
  "${repo_root}/doc/ubench-holesem-sf.png" \
  "${repo_root}/doc/ubench-chart.png" \
  "${repo_root}/doc/chain.png" \
  "${repo_root}/doc/lkb-integration.png" \
  "${repo_root}/doc/jh-extraction-mean.png" \
  "${repo_root}/doc/jh-chart-mean.png" \
  "${repo_root}/doc/chart-vs-solutions.png" \
  "${output_dir}/manual/"

(
  cd "${temporary_dir}"
  pandoc "${manual_sources[@]}" \
    --from=latex \
    --to=html5 \
    --standalone \
    --toc \
    --toc-depth=2 \
    --citeproc \
    --bibliography=chorus.bib \
    --mathml \
    --lua-filter="${repo_root}/website/manual-links.lua" \
    --template="${repo_root}/website/manual-template.html" \
    --metadata title="Utool Manual" \
    --metadata subtitle="The Swiss Army Knife of Underspecification" \
    --metadata version="3.1" \
    --metadata author="Alexander Koller and Stefan Thater" \
    --output="${output_dir}/manual/index.html"
)

touch "${output_dir}/.nojekyll"
echo "Website built in ${output_dir}"
