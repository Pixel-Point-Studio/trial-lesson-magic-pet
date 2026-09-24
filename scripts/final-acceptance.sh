#!/usr/bin/env bash
set -euo pipefail

# Полная служебная приёмка для владельца проекта.
# МОП не должен запускать её перед каждым занятием.

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repo_root"

[[ "$(git branch --show-current)" == "main" ]] \
  || { echo "ОШИБКА ПРИЁМКИ: полная проверка запускается только из main" >&2; exit 1; }
[[ -z "$(git status --porcelain --untracked-files=normal)" ]] \
  || { echo "ОШИБКА ПРИЁМКИ: сначала сохраните все изменения проекта" >&2; exit 1; }

echo "Служебная проверка для владельца проекта. МОПу запускать её не нужно."
echo "[1/4] Проверяю сборку, тесты и файлы проекта"
./scripts/preflight.sh

echo "[2/4] Проверяю создание отдельных папок для трёх студентов"
./scripts/test-prepare-lesson.sh

echo "[3/4] Проверяю инструкции и настройки редактора"
for file in \
  README.md \
  .vscode/extensions.json \
  .vscode/launch.json \
  .vscode/tasks.json \
  acceptance/SMOKE-CHECKLIST.md \
  acceptance/PILOT-REPORT.md; do
  [[ -s "$file" ]] || { echo "ОШИБКА ПРИЁМКИ: отсутствует $file" >&2; exit 1; }
done

grep -q 'Magic Pet: запустить и отлаживать' .vscode/launch.json \
  || { echo "ОШИБКА ПРИЁМКИ: в VS Code нет готового запуска с отладкой" >&2; exit 1; }
grep -q 'Magic Pet: запустить бота' .vscode/tasks.json \
  || { echo "ОШИБКА ПРИЁМКИ: в VS Code нет готовой команды запуска" >&2; exit 1; }
[[ "$(find lesson/patches -name '*.patch' | wc -l | tr -d ' ')" == "3" ]] \
  || { echo "ОШИБКА ПРИЁМКИ: должны существовать три набора учебных заданий" >&2; exit 1; }
grep -q 'github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues' README.md \
  || { echo "ОШИБКА ПРИЁМКИ: README не ведёт к списку заданий на GitHub" >&2; exit 1; }
grep -q 'notion.so/3e16ba2ce4e8801a9c67c1830814439b' README.md \
  || { echo "ОШИБКА ПРИЁМКИ: README не ведёт к методичке МОПа" >&2; exit 1; }

echo "[4/4] Проверяю, что студент видит только простой учебный код"
student_file="src/main/java/studio/pixelpoint/magicpet/lesson/StudentBot.java"
! grep -Eq 'telegrambots|java\.sql|java\.net\.http|jackson|infrastructure\.' "$student_file"
[[ "$(grep -c 'TODO' "$student_file" || true)" -le 4 ]]
for route_file in src/main/java/studio/pixelpoint/magicpet/lesson/route/*Lesson.java; do
  [[ "$(wc -l < "$route_file" | tr -d ' ')" -le 150 ]]
  ! grep -Eq 'telegrambots|java\.sql|java\.net\.http|jackson|infrastructure\.' "$route_file"
done

echo
echo "✅ АВТОМАТИЧЕСКАЯ ПРОВЕРКА ПРОЙДЕНА"
echo "Владельцу проекта осталось вручную заполнить acceptance/PILOT-REPORT.md:"
echo "- проверить настоящего Telegram-бота;"
echo "- пройти практику новичком по каждой теме;"
echo "- провести полный урок и проверить замену запасного токена."
