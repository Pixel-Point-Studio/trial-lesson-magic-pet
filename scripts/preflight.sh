#!/usr/bin/env bash
set -euo pipefail

# Внутренняя служебная проверка. МОП не запускает этот файл вручную:
# его вызывают prepare-lesson.sh и полная проверка владельца проекта.

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
mode="${1:-full}"
route="${2:-}"

fail() {
  echo "ОШИБКА ПРОВЕРКИ: $1" >&2
  exit 1
}

cd "$repo_root"

command -v java >/dev/null 2>&1 || fail "Java не найдена"
java_version="$(java -version 2>&1 | awk -F '"' 'NR==1 {print $2}')"
java_major="${java_version%%.*}"
if [[ "$java_major" == "1" ]]; then
  java_major="$(echo "$java_version" | cut -d. -f2)"
fi
[[ "$java_major" =~ ^[0-9]+$ ]] || fail "не удалось определить версию Java"
(( java_major >= 21 )) || fail "нужна Java 21 или новее"

[[ -x ./gradlew ]] || fail "Gradle wrapper отсутствует или не исполняемый"
[[ -f gradle/wrapper/gradle-wrapper.jar ]] || fail "отсутствует gradle-wrapper.jar"

for scenario in study sport blog; do
  for level in 1 2 3; do
    asset="assets/$scenario/level-$level.png"
    [[ -r "$asset" ]] || fail "нет ассета $asset"
    signature="$(xxd -p -l 8 "$asset")"
    [[ "$signature" == "89504e470d0a1a0a" ]] || fail "повреждён PNG $asset"
  done
done

if git ls-files --error-unmatch .env >/dev/null 2>&1; then
  fail ".env отслеживается Git"
fi
if git ls-files --error-unmatch .env.lesson-secrets >/dev/null 2>&1; then
  fail ".env.lesson-secrets отслеживается Git"
fi
git check-ignore -q .env.lesson-secrets \
  || fail ".env.lesson-secrets должен быть добавлен в .gitignore"

while IFS= read -r match; do
  case "$match" in
    *":TELEGRAM_BOT_TOKEN="|*":TELEGRAM_BOT_TOKEN=replace_me") ;;
    *) fail "в отслеживаемых файлах найдено значение TELEGRAM_BOT_TOKEN" ;;
  esac
done < <(git grep -I '^TELEGRAM_BOT_TOKEN=' -- ':!lesson/**' || true)

case "$mode" in
  --base-only)
    ./gradlew compileJava --quiet
    ;;
  --prepared)
    [[ "$route" =~ ^(study|sport|blog)$ ]] || fail "неверно выбрана тема занятия"
    todo_count="$(grep -R 'TODO STUDENT' src/main/java | wc -l | tr -d ' ')"
    [[ "$todo_count" == "4" ]] || fail "в подготовленном маршруте должно быть ровно четыре TODO STUDENT"
    ./gradlew compileJava --quiet
    set +e
    ./gradlew test --tests "*LessonRouteContractTest.${route}*" --quiet >/dev/null 2>&1
    defect_result=$?
    set -e
    [[ $defect_result -ne 0 ]] || fail "в подготовленной теме не воспроизводится учебная ошибка"
    ;;
  full)
    ./gradlew test --quiet
    ./scripts/verify-lesson-patches.sh
    ;;
  *)
    fail "неизвестный режим служебной проверки"
    ;;
esac

case "$mode" in
  --base-only) echo "✅ Компьютер готов: Java $java_major работает, проект собирается." ;;
  --prepared) echo "✅ Папка занятия подготовлена и проверена." ;;
  full) echo "✅ Полная служебная проверка проекта пройдена." ;;
esac
