#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
temp_root="$(mktemp -d "${TMPDIR:-/tmp}/magic-pet-lesson-check.XXXXXX")"
trap 'rm -rf "$temp_root"' EXIT

rsync -a \
  --exclude .git \
  --exclude .gradle \
  --exclude build \
  --exclude data \
  --exclude .env \
  "$repo_root/" "$temp_root/"

cd "$temp_root"

for route in study sport blog; do
  patch="lesson/patches/$route.patch"
  test_pattern="*LessonRouteContractTest.${route}Route*"

  git apply --check "$patch"
  git apply "$patch"
  GRADLE_USER_HOME="${GRADLE_USER_HOME:-$temp_root/.gradle-check}" ./gradlew compileJava --quiet

  set +e
  GRADLE_USER_HOME="${GRADLE_USER_HOME:-$temp_root/.gradle-check}" \
    ./gradlew test --tests "$test_pattern" --quiet >/dev/null 2>&1
  defect_result=$?
  set -e
  if [[ $defect_result -eq 0 ]]; then
    echo "Ошибка: дефект $route не воспроизводится" >&2
    exit 1
  fi

  git apply --reverse "$patch"
  GRADLE_USER_HOME="${GRADLE_USER_HOME:-$temp_root/.gradle-check}" \
    ./gradlew test --tests "$test_pattern" --quiet
  echo "OK: $route"
done
