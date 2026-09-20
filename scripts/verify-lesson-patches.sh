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
  test_pattern="*LessonRouteContractTest.${route}*"
  case "$route" in
    study) tests=(studyWrongPet studySavesCustomName studyShowsPet studyRenamesPet) ;;
    sport) tests=(sportLevelsAtOneHundred sportShowsCurrentLevelImage sportShowsHint sportShowsLevelProgress) ;;
    blog) tests=(blogShowsActiveTasks blogDeletesWithoutXp blogConfirmsDeletion blogShowsFullPlan) ;;
  esac

  git apply --check "$patch"
  git apply "$patch"
  GRADLE_USER_HOME="${GRADLE_USER_HOME:-$temp_root/.gradle-check}" ./gradlew compileJava --quiet

  todo_count="$(grep -R 'TODO STUDENT' src/main/java | wc -l | tr -d ' ')"
  if [[ "$todo_count" != "4" ]]; then
    echo "Ошибка: patch $route должен создавать ровно четыре TODO STUDENT" >&2
    exit 1
  fi

  for test_name in "${tests[@]}"; do
    set +e
    GRADLE_USER_HOME="${GRADLE_USER_HOME:-$temp_root/.gradle-check}" \
      ./gradlew test --tests "*LessonRouteContractTest.$test_name" --quiet >/dev/null 2>&1
    defect_result=$?
    set -e
    if [[ $defect_result -eq 0 ]]; then
      echo "Ошибка: учебная задача $test_name не воспроизводится" >&2
      exit 1
    fi
  done

  git apply --reverse "$patch"
  GRADLE_USER_HOME="${GRADLE_USER_HOME:-$temp_root/.gradle-check}" \
    ./gradlew test --tests "$test_pattern" --quiet
  echo "OK: $route"
done
