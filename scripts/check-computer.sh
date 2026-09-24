#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd -P)"
if [[ "$(pwd -P)" != "$repo_root" ]]; then
  echo "ОШИБКА: откройте папку trial-lesson-magic-pet в VS Code и запустите команду из её терминала." >&2
  exit 1
fi

echo "Проверяю компьютер для занятия Magic Pet…"
./scripts/preflight.sh --base-only

if command -v code >/dev/null 2>&1; then
  installed_extensions="$(code --list-extensions 2>/dev/null | tr '[:upper:]' '[:lower:]' || true)"
  missing=0
  for extension in vscjava.vscode-java-pack vscjava.vscode-gradle; do
    if ! printf '%s\n' "$installed_extensions" | grep -qx "$extension"; then
      echo "⚠️ В VS Code не установлено расширение $extension"
      missing=1
    fi
  done
  if [[ $missing -eq 1 ]]; then
    echo "Откройте в VS Code раздел Extensions и установите рекомендованные расширения проекта."
    exit 1
  fi
  echo "✅ Расширения VS Code установлены."
else
  echo "⚠️ Не получилось автоматически проверить расширения VS Code."
  echo "Откройте папку проекта в VS Code и согласитесь установить рекомендованные расширения."
fi

if [[ -f .env.lesson-secrets ]]; then
  echo "✅ Локальные школьные настройки ИИ найдены."
else
  echo "⚠️ Школьные настройки ИИ пока не добавлены. Бот запустится с готовым локальным планом."
  echo "Владелец проекта может создать .env.lesson-secrets по примеру .env.lesson-secrets.example."
fi

echo "✅ Основная подготовка компьютера завершена."
