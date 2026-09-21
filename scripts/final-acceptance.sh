#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repo_root"

[[ "$(git branch --show-current)" == "main" ]] \
  || { echo "ACCEPTANCE ERROR: проверка запускается из main" >&2; exit 1; }
[[ -z "$(git status --porcelain --untracked-files=normal)" ]] \
  || { echo "ACCEPTANCE ERROR: для финальной приёмки нужен чистый commit" >&2; exit 1; }

echo "[1/4] Product preflight"
./scripts/preflight.sh

echo "[2/4] Lesson worktree isolation"
./scripts/test-prepare-lesson.sh

echo "[3/4] Documentation"
for file in \
  README.md \
  .run/Magic_Pet.run.xml \
  acceptance/SMOKE-CHECKLIST.md \
  acceptance/PILOT-REPORT.md; do
  [[ -s "$file" ]] || { echo "ACCEPTANCE ERROR: отсутствует $file" >&2; exit 1; }
done

grep -q '<option value="run" />' .run/Magic_Pet.run.xml \
  || { echo "ACCEPTANCE ERROR: IntelliJ-конфигурация не запускает Gradle task run" >&2; exit 1; }
[[ "$(find lesson/patches -name '*.patch' | wc -l | tr -d ' ')" == "3" ]] \
  || { echo "ACCEPTANCE ERROR: ожидаются три route patch-файла" >&2; exit 1; }
grep -q 'github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues' README.md \
  || { echo "ACCEPTANCE ERROR: README не ведёт к GitHub Issues" >&2; exit 1; }
grep -q 'notion.so/3e16ba2ce4e8801a9c67c1830814439b' README.md \
  || { echo "ACCEPTANCE ERROR: README не ведёт к методичке МОПа" >&2; exit 1; }

echo "[4/4] Student surface"
student_file="src/main/java/studio/pixelpoint/magicpet/lesson/StudentBot.java"
! grep -Eq 'telegrambots|java\.sql|java\.net\.http|jackson|infrastructure\.' "$student_file"
[[ "$(grep -c 'TODO' "$student_file" || true)" -le 4 ]]
for route_file in src/main/java/studio/pixelpoint/magicpet/lesson/route/*Lesson.java; do
  [[ "$(wc -l < "$route_file" | tr -d ' ')" -le 150 ]]
  ! grep -Eq 'telegrambots|java\.sql|java\.net\.http|jackson|infrastructure\.' "$route_file"
done

echo
echo "AUTOMATED ACCEPTANCE OK"
echo "Manual sign-off is still required in acceptance/PILOT-REPORT.md:"
echo "- real Telegram token and /start response under 30 seconds"
echo "- 35–40 minute novice practice for each selected route"
echo "- full 90-minute lesson and fallback-token switch under 2 minutes"
