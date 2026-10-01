#!/usr/bin/env bash
set -euo pipefail

# Внутренняя проверка учебной ветки main. МОП и студент её не запускают.

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repo_root"

todo_count="$(grep -R 'TODO STUDENT' src/main/java | wc -l | tr -d ' ')"
[[ "$todo_count" == "4" ]] \
  || { echo "ОШИБКА: в main должно быть ровно 4 учебных задания, найдено $todo_count" >&2; exit 1; }

./gradlew compileJava --quiet
./gradlew test --tests '*LessonMaterialsTest' --quiet

tests=(
  task1RemembersEnteredNameForEveryPet
  task2RenamesEveryPetAndKeepsMenuBelowCurrentAction
  task3ConfirmsDeletionForEveryPet
  task4ShowsEveryPetAndKeepsButtonInCommonMenu
)

for test_name in "${tests[@]}"; do
  set +e
  ./gradlew test --tests "*LessonRouteContractTest.${test_name}" --quiet >/dev/null 2>&1
  result=$?
  set -e
  [[ $result -ne 0 ]] \
    || { echo "ОШИБКА: учебная задача уже решена в main: $test_name" >&2; exit 1; }
done

echo "✅ Учебная ветка проверена: проект собирается, все 4 общих задания воспроизводятся."
