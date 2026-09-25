#!/usr/bin/env bash
set -euo pipefail

# Служебный автотест подготовки занятия для владельца проекта.
# МОПу этот файл запускать не нужно.

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
temp_root="$(mktemp -d "${TMPDIR:-/tmp}/magic-pet-prepare-check.XXXXXX")"
trap 'rm -rf "$temp_root"' EXIT
fixture="$temp_root/repository"
worktrees="$temp_root/worktrees"

mkdir -p "$fixture"
rsync -a \
  --exclude .git \
  --exclude .gradle \
  --exclude build \
  --exclude data \
  --exclude .env \
  "$repo_root/" "$fixture/"

cd "$fixture"
git init -b main --quiet
git config user.name "Magic Pet Check"
git config user.email "check@localhost"
git add .
git commit -m "Acceptance fixture" --quiet
main_before="$(git rev-parse main)"

printf '%s\n' \
  'LLM_API_URL=https://school.example/ai' \
  'LLM_API_KEY=fake-check-key' \
  'LLM_MODEL=school-check-model' \
  'TELEGRAM_BOT_TOKEN=must-not-copy' > .env.lesson-secrets

declare -a prepared_paths=()
for suffix in 1 2; do
  timestamp="20260925-12000${suffix}"
  output="$(./gradlew prepareLesson --console=plain \
    -PlessonWorktreeRoot="$worktrees" \
    -PlessonTimestamp="$timestamp" \
    -PlessonNoOpen=true)"
  path="$(printf '%s\n' "$output" | sed -n 's/^ПАПКА_УРОКА=//p' | tail -n 1)"
  [[ -n "$path" && -d "$path" ]] \
    || { echo "ОШИБКА: отдельная папка занятия не создана" >&2; exit 1; }
  prepared_paths+=("$path")

  [[ -f "$path/.env" ]] || { echo "ОШИБКА: в папке занятия нет .env" >&2; exit 1; }
  grep -qx 'TELEGRAM_BOT_TOKEN=replace_me' "$path/.env"
  grep -qx 'LLM_API_URL=https://school.example/ai' "$path/.env"
  grep -qx 'LLM_API_KEY=fake-check-key' "$path/.env"
  grep -qx 'LLM_MODEL=school-check-model' "$path/.env"
  ! grep -q 'must-not-copy' "$path/.env"
  [[ -d "$path/data" && -z "$(find "$path/data" -mindepth 1 -print -quit)" ]]
  [[ -z "$(git -C "$path" status --porcelain --untracked-files=normal)" ]]
  [[ "$(git -C "$path" rev-list --count main..HEAD)" == "1" ]]
  grep -q '# Начни здесь: твоя папка Magic Pet' "$path/README.md"
  [[ "$(grep -c 'github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/' "$path/README.md")" == "12" ]]
  grep -q 'Можно взять две задачи из одной темы или смешать темы' "$path/README.md"
  ! grep -q '{{' "$path/README.md"
  [[ ! -e "$path/lesson/README.lesson.md" ]]

  if [[ "$suffix" == "1" ]]; then
    printf '\nSCHOOL_SECRET=must-not-copy-next\n' >> "$path/.env"
    touch "$path/data/first-student.db"
  else
    ! grep -q 'must-not-copy-next' "$path/.env"
    [[ ! -e "$path/data/first-student.db" ]]
  fi
done

[[ "${prepared_paths[0]}" != "${prepared_paths[1]}" ]]
[[ "$(git worktree list --porcelain | grep -c '^worktree ')" == "3" ]]

for path in "${prepared_paths[@]}"; do
  git worktree remove --force "$path"
done

[[ "$(git rev-parse main)" == "$main_before" ]]
[[ -z "$(git status --porcelain)" ]]
echo "✅ Подготовка занятия проверена: два запуска создают две независимые папки со всеми темами."
