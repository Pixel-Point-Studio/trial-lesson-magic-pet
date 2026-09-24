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

declare -a prepared_paths=()
index=1
for route in study sport blog; do
  timestamp="20260920-12000$index"
  student="Test Student $index"
  [[ $index -eq 1 ]] && student="../Иван / Иванов"
  if [[ $index -eq 2 ]]; then
    printf '%s\n' \
      'LLM_API_URL=https://school.example/ai' \
      'LLM_API_KEY=fake-check-key' \
      'LLM_MODEL=school-check-model' \
      'TELEGRAM_BOT_TOKEN=fake-reserve-token' > .env.lesson-secrets
  elif [[ $index -eq 3 ]]; then
    rm -f .env.lesson-secrets
  fi
  if [[ $index -eq 1 ]]; then
    output="$(printf '1\n' | env LESSON_WORKTREE_ROOT="$worktrees" PREPARE_LESSON_TIMESTAMP="$timestamp" \
      ./scripts/prepare-lesson.sh "$student")"
  else
    output="$(LESSON_WORKTREE_ROOT="$worktrees" PREPARE_LESSON_TIMESTAMP="$timestamp" \
      ./scripts/prepare-lesson.sh "$student" "$route")"
  fi
  path="$(printf '%s\n' "$output" | awk '
    previous == "Откройте эту папку в VS Code:" { print; exit }
    { previous = $0 }
  ')"
  [[ -n "$path" && -d "$path" ]] || { echo "ОШИБКА: для темы $route не создана отдельная папка" >&2; exit 1; }
  canonical_worktrees="$(cd "$worktrees" && pwd)"
  [[ "$path" == "$canonical_worktrees/"* ]] || { echo "ОШИБКА: папка студента создана за пределами безопасного каталога" >&2; exit 1; }
  prepared_paths+=("$path")

  [[ -f "$path/.env" ]] || { echo "ОШИБКА: в папке студента нет файла настроек .env" >&2; exit 1; }
  grep -qx 'TELEGRAM_BOT_TOKEN=replace_me' "$path/.env"
  [[ -d "$path/data" && -z "$(find "$path/data" -mindepth 1 -print -quit)" ]] \
    || { echo "ОШИБКА: новая папка студента уже содержит чужие данные" >&2; exit 1; }
  [[ -z "$(git -C "$path" status --porcelain --untracked-files=normal)" ]] \
    || { echo "ОШИБКА: в новой папке видны лишние изменения" >&2; exit 1; }
  [[ "$(git -C "$path" rev-list --count main..HEAD)" == "1" ]] \
    || { echo "ОШИБКА: стартовые задания не сохранены отдельным техническим шагом" >&2; exit 1; }
  [[ "$(grep -R 'TODO STUDENT' "$path/src/main/java" | wc -l | tr -d ' ')" == "4" ]]
  [[ "$(printf '%s\n' "$output" | grep -c 'github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/')" == "4" ]]
  printf '%s\n' "$output" | grep -q 'Методичка МОПа: https://www.notion.so/3e16ba2ce4e8801a9c67c1830814439b'

  case "$route" in
    study)
      grep -q 'return Scenario.SPORT' "$path/src/main/java/studio/pixelpoint/magicpet/lesson/route/StudyLesson.java"
      printf '%s\n' "$output" | grep -q 'рекомендуются задания 01 и 03'
      ;;
    sport)
      grep -q 'experience > 100' "$path/src/main/java/studio/pixelpoint/magicpet/domain/LevelProgression.java"
      printf '%s\n' "$output" | grep -q 'рекомендуются задания 01 и 04'
      grep -qx 'LLM_API_URL=https://school.example/ai' "$path/.env"
      grep -qx 'LLM_API_KEY=fake-check-key' "$path/.env"
      grep -qx 'LLM_MODEL=school-check-model' "$path/.env"
      grep -qx 'TELEGRAM_BOT_TOKEN=replace_me' "$path/.env"
      printf '%s\n' "$output" | grep -q 'Школьный ИИ подключён автоматически'
      ! printf '%s\n' "$output" | grep -q 'fake-check-key'
      ;;
    blog)
      grep -q 'pet.completeTask(user, taskId' "$path/src/main/java/studio/pixelpoint/magicpet/lesson/route/BlogLesson.java"
      printf '%s\n' "$output" | grep -q 'рекомендуются задания 01 и 03'
      ;;
  esac

  if [[ $index -eq 1 ]]; then
    printf '\nSCHOOL_SECRET=must-not-copy\n' >> "$path/.env"
    touch "$path/data/first-student.db"
  else
    ! grep -q 'must-not-copy' "$path/.env"
    [[ ! -e "$path/data/first-student.db" ]]
  fi
  index=$((index + 1))
done

[[ "${prepared_paths[0]}" != "${prepared_paths[1]}" ]]
[[ "${prepared_paths[1]}" != "${prepared_paths[2]}" ]]
[[ "$(git worktree list --porcelain | grep -c '^worktree ')" == "4" ]]

before_invalid="$(find "$worktrees" -mindepth 1 -maxdepth 1 -type d | wc -l | tr -d ' ')"
set +e
LESSON_WORKTREE_ROOT="$worktrees" ./scripts/prepare-lesson.sh "Bad Route" invalid >/dev/null 2>&1
invalid_result=$?
set -e
[[ $invalid_result -ne 0 ]]
after_invalid="$(find "$worktrees" -mindepth 1 -maxdepth 1 -type d | wc -l | tr -d ' ')"
[[ "$before_invalid" == "$after_invalid" ]]

for path in "${prepared_paths[@]}"; do
  git worktree remove --force "$path"
done

[[ "$(git rev-parse main)" == "$main_before" ]]
[[ -z "$(git status --porcelain)" ]]
echo "✅ Подготовка занятия проверена: три студента получают три отдельные папки."
