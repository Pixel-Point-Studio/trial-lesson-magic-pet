#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Как запустить: ./scripts/prepare-lesson.sh \"Имя Фамилия\"" >&2
  echo "Запускайте эту команду в терминале VS Code из основной папки проекта." >&2
  exit 2
}

[[ $# -ge 1 && $# -le 2 ]] || usage
student_name="$1"
[[ -n "${student_name//[[:space:]]/}" ]] || usage

repo_root="$(cd "$(dirname "$0")/.." && pwd -P)"
[[ "$(pwd -P)" == "$repo_root" ]] || {
  echo "ОШИБКА: команда запущена не из основной папки проекта." >&2
  echo "Откройте папку trial-lesson-magic-pet в VS Code, откройте в ней терминал и повторите команду." >&2
  exit 1
}
cd "$repo_root"

choose_route() {
  local answer
  echo
  echo "С чем студенту интереснее поработать?"
  echo "  1 — Учёба (совёнок Руни)"
  echo "  2 — Спорт (дракончик Игнис)"
  echo "  3 — Личный блог (лисёнок Скриба)"
  while true; do
    read -r -p "Введите 1, 2 или 3: " answer
    case "$answer" in
      1|study|Study|учёба|Учёба|учеба|Учеба) route="study"; return ;;
      2|sport|Sport|спорт|Спорт) route="sport"; return ;;
      3|blog|Blog|блог|Блог) route="blog"; return ;;
      *) echo "Пожалуйста, введите только 1, 2 или 3." ;;
    esac
  done
}

# Второй аргумент нужен только автоматическим проверкам проекта.
# В обычном занятии МОП передаёт только имя и видит понятный выбор на русском.
if [[ $# -eq 2 ]]; then
  route="$2"
  [[ "$route" =~ ^(study|sport|blog)$ ]] || usage
else
  choose_route
fi

git rev-parse --is-inside-work-tree >/dev/null 2>&1 \
  || { echo "ОШИБКА: это не папка проекта Magic Pet. Попросите технического специалиста проверить установку." >&2; exit 1; }
git show-ref --verify --quiet refs/heads/main \
  || { echo "ОШИБКА: проект установлен не полностью. Попросите технического специалиста восстановить его." >&2; exit 1; }
[[ "$(git branch --show-current)" == "main" ]] \
  || { echo "ОШИБКА: сейчас открыта не исходная версия проекта. Попросите технического специалиста переключить проект на main." >&2; exit 1; }
[[ -z "$(git status --porcelain --untracked-files=normal)" ]] \
  || { echo "ОШИБКА: в основной папке остались несохранённые изменения. Ничего не удаляйте — позовите технического специалиста." >&2; exit 1; }

echo
echo "Проверяю, что компьютер готов к занятию…"
./scripts/preflight.sh --base-only

slug="$(printf '%s' "$student_name" \
  | tr '[:upper:]' '[:lower:]' \
  | sed -E 's/[^[:alnum:]]+/-/g; s/^-+//; s/-+$//' \
  | cut -c1-48)"
[[ -n "$slug" ]] || slug="student"

timestamp="${PREPARE_LESSON_TIMESTAMP:-$(date +%Y%m%d-%H%M%S)}"
[[ "$timestamp" =~ ^[0-9]{8}-[0-9]{6}(-[0-9]+)?$ ]] \
  || { echo "ОШИБКА: не удалось создать номер занятия. Попросите технического специалиста о помощи." >&2; exit 1; }

branch="lesson/$slug-$timestamp"
worktree_parent="${LESSON_WORKTREE_ROOT:-$(dirname "$repo_root")/magic-pet-lessons}"
mkdir -p "$worktree_parent"
worktree_parent="$(cd "$worktree_parent" && pwd)"
target="$worktree_parent/$slug-$timestamp-$route"

git show-ref --verify --quiet "refs/heads/$branch" \
  && { echo "ОШИБКА: занятие с таким номером уже существует. Повторите команду через минуту." >&2; exit 1; }
[[ ! -e "$target" ]] || { echo "ОШИБКА: папка занятия уже существует. Повторите команду через минуту." >&2; exit 1; }

created=0
cleanup_on_error() {
  status=$?
  if [[ $status -ne 0 && $created -eq 1 ]]; then
    git -C "$repo_root" worktree remove --force "$target" >/dev/null 2>&1 || true
    git -C "$repo_root" branch -D "$branch" >/dev/null 2>&1 || true
    echo "ОШИБКА: подготовка не завершилась. Незаконченная папка удалена; исходный проект не пострадал." >&2
  fi
  exit $status
}
trap cleanup_on_error EXIT

git worktree add -b "$branch" "$target" main >/dev/null 2>&1
created=1

cp "$target/.env.example" "$target/.env"
chmod 600 "$target/.env"
mkdir -p "$target/data"

secrets_file="$repo_root/.env.lesson-secrets"
if [[ -f "$secrets_file" ]]; then
  secrets_tmp="$(mktemp "${TMPDIR:-/tmp}/magic-pet-env.XXXXXX")"
  awk -F= '
    NR == FNR {
      if (($1 == "LLM_API_URL" || $1 == "LLM_API_KEY" || $1 == "LLM_MODEL") && length($0) > length($1) + 1) {
        secret[$1] = $0
      }
      next
    }
    {
      key = $1
      if (key in secret) print secret[key]
      else print $0
    }
  ' "$secrets_file" "$target/.env" > "$secrets_tmp"
  mv "$secrets_tmp" "$target/.env"
  chmod 600 "$target/.env"
  ai_url_present="$(awk -F= '$1 == "LLM_API_URL" && length($0) > length($1) + 1 { print "yes"; exit }' "$secrets_file")"
else
  ai_url_present=""
fi
git -C "$target" apply "lesson/patches/$route.patch"

(cd "$target" && ./scripts/preflight.sh --prepared "$route")

# Сохраняем стартовое состояние отдельно, чтобы VS Code показывал только
# изменения, сделанные студентом на занятии.
git -C "$target" add -u
git -C "$target" \
  -c user.name="Pixel Point Lesson" \
  -c user.email="lesson@pixelpoint.local" \
  commit -m "Подготовить задания для занятия" --quiet

case "$route" in
  study) pet_name="Руни" ;;
  sport) pet_name="Игнис" ;;
  blog) pet_name="Скриба" ;;
esac
created=2
trap - EXIT

echo
echo "✅ Папка для урока готова"
echo
echo "Откройте эту папку в VS Code:"
echo "$target"
echo
echo "Выбрана тема:"
case "$route" in
  study)
    echo "Учёба — питомец $pet_name"
    echo "Задание 1 (ошибка):  https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/5"
    echo "Задание 2 (ошибка):  https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/10"
    echo "Задание 3 (улучшение): https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/8"
    echo "Задание 4 (улучшение): https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/11"
    recommendation="01 и 03"
    ;;
  sport)
    echo "Спорт — питомец $pet_name"
    echo "Задание 1 (ошибка):  https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/4"
    echo "Задание 2 (ошибка):  https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/12"
    echo "Задание 3 (улучшение): https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/9"
    echo "Задание 4 (улучшение): https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/13"
    recommendation="01 и 04"
    ;;
  blog)
    echo "Личный блог — питомец $pet_name"
    echo "Задание 1 (ошибка):  https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/14"
    echo "Задание 2 (ошибка):  https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/3"
    echo "Задание 3 (улучшение): https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/7"
    echo "Задание 4 (улучшение): https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/15"
    recommendation="01 и 03"
    ;;
esac
echo
echo "Что делать дальше:"
echo "1. Откройте указанную выше папку в VS Code."
echo "2. Откройте файл .env и вставьте Telegram-токен после TELEGRAM_BOT_TOKEN=."
if [[ -n "$ai_url_present" ]]; then
  echo "3. Школьный ИИ подключён автоматически."
else
  echo "3. Школьный ИИ не настроен — бот всё равно запустится с готовым резервным планом."
fi
echo "4. Откройте раздел «Запуск и отладка» в VS Code и выберите «Magic Pet: запустить и отлаживать»."
echo "5. Для урока рекомендуются задания $recommendation. Остальные можно оставить на будущее."
echo
echo "Методичка МОПа: https://www.notion.so/3e16ba2ce4e8801a9c67c1830814439b"
echo "После занятия остановите бота. Если использовали запасной школьный Telegram-токен, сообщите владельцу проекта."
