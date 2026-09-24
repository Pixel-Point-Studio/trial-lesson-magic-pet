#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Укажите код задания, например: ./scripts/check-task.sh S1" >&2
  echo "Допустимые коды: S1–S4, I1–I4, B1–B4." >&2
  exit 2
}

[[ $# -eq 1 ]] || usage
task_code="$(printf '%s' "$1" | tr '[:lower:]' '[:upper:]')"

case "$task_code" in
  S1) test_name="studyWrongPet" ;;
  S2) test_name="studySavesCustomName" ;;
  S3) test_name="studyShowsPet" ;;
  S4) test_name="studyRenamesPet" ;;
  I1) test_name="sportLevelsAtOneHundred" ;;
  I2) test_name="sportShowsCurrentLevelImage" ;;
  I3) test_name="sportConfirmsDeletion" ;;
  I4) test_name="sportShowsLevelProgress" ;;
  B1) test_name="blogShowsActiveTasks" ;;
  B2) test_name="blogDeletesWithoutXp" ;;
  B3) test_name="blogShowsHint" ;;
  B4) test_name="blogShowsFullPlan" ;;
  *) usage ;;
esac

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repo_root"

echo "Проверяю задание ${task_code}…"
set +e
./gradlew test --tests "*LessonRouteContractTest.$test_name" --quiet >/dev/null 2>&1
result=$?
set -e

if [[ $result -eq 0 ]]; then
  echo "✅ Задание выполнено"
  exit 0
fi

echo "❌ Пока не получилось"
echo "Проверьте последние изменения и попробуйте ещё раз. Если нужна помощь — откройте подсказку в задании на GitHub."
exit 1
