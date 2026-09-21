#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Использование: ./scripts/prepare-lesson.sh \"Имя Фамилия\" study|sport|blog" >&2
  exit 2
}

[[ $# -eq 2 ]] || usage
student_name="$1"
route="$2"
[[ "$route" =~ ^(study|sport|blog)$ ]] || usage
[[ -n "${student_name//[[:space:]]/}" ]] || usage

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repo_root"

git rev-parse --is-inside-work-tree >/dev/null 2>&1 \
  || { echo "ERROR: проект не является Git-репозиторием" >&2; exit 1; }
git show-ref --verify --quiet refs/heads/main \
  || { echo "ERROR: локальная ветка main не найдена" >&2; exit 1; }
[[ "$(git branch --show-current)" == "main" ]] \
  || { echo "ERROR: prepare-lesson запускается из ветки main" >&2; exit 1; }
[[ -z "$(git status --porcelain --untracked-files=normal)" ]] \
  || { echo "ERROR: исходный main должен иметь чистое рабочее дерево" >&2; exit 1; }

./scripts/preflight.sh --base-only

slug="$(printf '%s' "$student_name" \
  | tr '[:upper:]' '[:lower:]' \
  | sed -E 's/[^[:alnum:]]+/-/g; s/^-+//; s/-+$//' \
  | cut -c1-48)"
[[ -n "$slug" ]] || slug="student"

timestamp="${PREPARE_LESSON_TIMESTAMP:-$(date +%Y%m%d-%H%M%S)}"
[[ "$timestamp" =~ ^[0-9]{8}-[0-9]{6}(-[0-9]+)?$ ]] \
  || { echo "ERROR: некорректный PREPARE_LESSON_TIMESTAMP" >&2; exit 1; }

branch="lesson/$slug-$timestamp"
worktree_parent="${LESSON_WORKTREE_ROOT:-$(dirname "$repo_root")/magic-pet-lessons}"
mkdir -p "$worktree_parent"
worktree_parent="$(cd "$worktree_parent" && pwd)"
target="$worktree_parent/$slug-$timestamp-$route"

git show-ref --verify --quiet "refs/heads/$branch" \
  && { echo "ERROR: ветка уже существует: $branch" >&2; exit 1; }
[[ ! -e "$target" ]] || { echo "ERROR: путь уже существует: $target" >&2; exit 1; }

created=0
cleanup_on_error() {
  status=$?
  if [[ $status -ne 0 && $created -eq 1 ]]; then
    git -C "$repo_root" worktree remove --force "$target" >/dev/null 2>&1 || true
    git -C "$repo_root" branch -D "$branch" >/dev/null 2>&1 || true
    echo "ERROR: подготовка не завершена; частичное окружение удалено" >&2
  fi
  exit $status
}
trap cleanup_on_error EXIT

git worktree add -b "$branch" "$target" main >/dev/null
created=1

cp "$target/.env.example" "$target/.env"
mkdir -p "$target/data"
git -C "$target" apply "lesson/patches/$route.patch"

(cd "$target" && ./scripts/preflight.sh --prepared "$route")

# The prepared backlog is the student's clean starting point. Committing it here
# keeps the first `git status` empty and makes `git diff` show only student work.
git -C "$target" add -u
git -C "$target" \
  -c user.name="Pixel Point Lesson" \
  -c user.email="lesson@pixelpoint.local" \
  commit -m "Prepare $route lesson backlog" --quiet

case "$route" in
  study) pet_name="Руни" ;;
  sport) pet_name="Игнис" ;;
  blog) pet_name="Скриба" ;;
esac
created=2
trap - EXIT

echo
echo "LESSON READY"
echo "LESSON_PATH=$target"
echo "LESSON_BRANCH=$branch"
echo "LESSON_ROUTE=$route"
echo "LESSON_PET=$pet_name"
echo "LESSON_ISSUES:"
case "$route" in
  study)
    echo "01 BUG      https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/5"
    echo "02 BUG      https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/10"
    echo "03 FEATURE  https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/8"
    echo "04 FEATURE  https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/11"
    recommendation="01 и 03"
    ;;
  sport)
    echo "01 BUG      https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/4"
    echo "02 BUG      https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/12"
    echo "03 FEATURE  https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/9"
    echo "04 FEATURE  https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/13"
    recommendation="01 и 03"
    ;;
  blog)
    echo "01 BUG      https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/14"
    echo "02 BUG      https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/3"
    echo "03 FEATURE  https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/7"
    echo "04 FEATURE  https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/15"
    recommendation="01 и 04"
    ;;
esac
echo
echo "Откройте LESSON_PATH в IntelliJ IDEA."
echo "Для урока рекомендуются задачи $recommendation; остальные остаются backlog."
echo "Заполните только TELEGRAM_BOT_TOKEN в $target/.env."
echo "Методичка МОПа: https://www.notion.so/3e16ba2ce4e8801a9c67c1830814439b"
echo "После урока остановите приложение, отзовите/ротируйте школьный токен и удалите worktree."
