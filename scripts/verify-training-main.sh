#!/usr/bin/env bash
set -euo pipefail

# Внутренняя проверка учебной ветки main. МОП и студент её не запускают.

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repo_root"

todo_count="$(grep -R 'TODO STUDENT' src/main/java | wc -l | tr -d ' ')"
[[ "$todo_count" == "12" ]] \
  || { echo "ОШИБКА: в main должно быть ровно 12 учебных мест, найдено $todo_count" >&2; exit 1; }

./gradlew compileJava --quiet
./gradlew test --tests '*LessonMaterialsTest' --quiet

tests=(
  studyWrongPet studySavesCustomName studyShowsPet studyRenamesPet
  sportLevelsAtOneHundred sportShowsCurrentLevelImage sportConfirmsDeletion sportShowsLevelProgress
  blogShowsActiveTasks blogDeletesWithoutXp blogShowsHint blogShowsFullPlan
)

for test_name in "${tests[@]}"; do
  set +e
  ./gradlew test --tests "*LessonRouteContractTest.${test_name}" --quiet >/dev/null 2>&1
  result=$?
  set -e
  [[ $result -ne 0 ]] \
    || { echo "ОШИБКА: учебная задача уже решена в main: $test_name" >&2; exit 1; }
done

echo "✅ Учебная ветка проверена: проект собирается, все 12 независимых задач воспроизводятся."
