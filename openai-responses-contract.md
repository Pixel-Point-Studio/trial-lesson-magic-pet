# Контракт OpenAI Responses API

Magic Pet обращается напрямую к OpenAI Responses API через `OpenAiResponsesPlanGenerator`.

## Настройки

```dotenv
LLM_API_URL=https://api.openai.com/v1/responses
LLM_API_KEY=<OpenAI API key>
LLM_MODEL=gpt-4o-mini-2024-07-18
```

Если `LLM_API_URL` пуст, приложение не делает сетевой запрос и использует локальные планы. Если URL заполнен, ключ и модель обязательны.

## Запрос

Используется `POST` с заголовками `Authorization: Bearer <LLM_API_KEY>` и `Content-Type: application/json`.

Тело содержит:

- `model` — модель из `LLM_MODEL`;
- `store: false` — ответ не сохраняется для последующего использования API;
- `instructions` — правила из `plan-generator.ru.json`;
- `input` — выбранная тема и очищенный текст цели;
- `text.format` — строгая JSON Schema для описания плана и трёх задач.

Telegram ID, display name, username, Telegram-токен и имя питомца в OpenAI не отправляются.

## Ответ и fallback

Клиент извлекает текст из элементов `output[].content[]` с типом `output_text`, затем повторно проверяет:

- ровно три задачи;
- `summary`: 1–300 символов;
- `title`: 1–80 символов;
- `description`: 1–500 символов;
- отсутствие дополнительных полей;
- отсутствие известных опасных спортивных советов.

Timeout, HTTP-ошибка, отказ модели, незавершённый ответ или нарушение контракта включают локальный план. Безопасный лог показывает `ai.plan_generated` либо `ai.plan_fallback`, но не выводит ключ, цель, промпт или тело ответа.
