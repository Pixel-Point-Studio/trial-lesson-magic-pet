#!/usr/bin/env bash
set -euo pipefail

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
  output="$(LESSON_WORKTREE_ROOT="$worktrees" PREPARE_LESSON_TIMESTAMP="$timestamp" \
    ./scripts/prepare-lesson.sh "$student" "$route")"
  path="$(printf '%s\n' "$output" | sed -n 's/^LESSON_PATH=//p')"
  [[ -n "$path" && -d "$path" ]] || { echo "ERROR: $route worktree не создан" >&2; exit 1; }
  canonical_worktrees="$(cd "$worktrees" && pwd)"
  [[ "$path" == "$canonical_worktrees/"* ]] || { echo "ERROR: нормализация имени вышла за worktree root" >&2; exit 1; }
  prepared_paths+=("$path")

  [[ -f "$path/.env" ]] || { echo "ERROR: нет чистого .env" >&2; exit 1; }
  grep -qx 'TELEGRAM_BOT_TOKEN=replace_me' "$path/.env"
  [[ -d "$path/data" && -z "$(find "$path/data" -mindepth 1 -print -quit)" ]] \
    || { echo "ERROR: data не является пустым" >&2; exit 1; }
  [[ "$(grep -R 'TODO STUDENT' "$path/src/main/java" | wc -l | tr -d ' ')" == "4" ]]

  case "$route" in
    study) grep -q 'selectScenario(user, Scenario.SPORT)' "$path/src/main/java/studio/pixelpoint/magicpet/lesson/route/StudyLesson.java" ;;
    sport) grep -q 'experience > 100' "$path/src/main/java/studio/pixelpoint/magicpet/domain/LevelProgression.java" ;;
    blog) grep -q 'pet.completeTask(user, taskId' "$path/src/main/java/studio/pixelpoint/magicpet/lesson/route/BlogLesson.java" ;;
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
echo "PREPARE LESSON CHECK OK: 3 independent worktrees"
