#!/usr/bin/env bash
set -euo pipefail

# Служебная проверка учебных наборов для владельца проекта.
# МОПу этот файл запускать не нужно.

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
    study) route_name="Учёба"; tests=(studyWrongPet studySavesCustomName studyShowsPet studyRenamesPet) ;;
    sport) route_name="Спорт"; tests=(sportLevelsAtOneHundred sportShowsCurrentLevelImage sportConfirmsDeletion sportShowsLevelProgress) ;;
    blog) route_name="Личный блог"; tests=(blogShowsActiveTasks blogDeletesWithoutXp blogShowsHint blogShowsFullPlan) ;;
  esac

  git apply --check "$patch"
  git apply "$patch"
  GRADLE_USER_HOME="${GRADLE_USER_HOME:-$temp_root/.gradle-check}" ./gradlew compileJava --quiet

  todo_count="$(grep -R 'TODO STUDENT' src/main/java | wc -l | tr -d ' ')"
  if [[ "$todo_count" != "4" ]]; then
    echo "ОШИБКА: тема «${route_name}» должна содержать ровно четыре задания для студента." >&2
    exit 1
  fi

  for test_name in "${tests[@]}"; do
    set +e
    GRADLE_USER_HOME="${GRADLE_USER_HOME:-$temp_root/.gradle-check}" \
      ./gradlew test --tests "*LessonRouteContractTest.$test_name" --quiet >/dev/null 2>&1
    defect_result=$?
    set -e
    if [[ $defect_result -eq 0 ]]; then
      echo "ОШИБКА: одна из задач темы «${route_name}» не воспроизводится: $test_name" >&2
      exit 1
    fi
  done

  git apply --reverse "$patch"
  GRADLE_USER_HOME="${GRADLE_USER_HOME:-$temp_root/.gradle-check}" \
    ./gradlew test --tests "$test_pattern" --quiet
  echo "✅ Тема «${route_name}»: все четыре задания воспроизводятся и имеют решения."
done
