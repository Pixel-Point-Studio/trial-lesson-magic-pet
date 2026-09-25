# Начни здесь: твоя папка Magic Pet

Эта папка уже подготовлена для занятия. Повторно запускать подготовку здесь не нужно.

**Школьный AI:** {{AI_STATUS}}

## 1. Подключи Telegram-бота

1. Открой файл `.env` слева в Visual Studio Code.
2. Найди строку `TELEGRAM_BOT_TOKEN=replace_me`.
3. Замени только `replace_me` на токен от BotFather или запасного школьного бота.
4. Сохрани файл через **Cmd+S** на Mac или **Ctrl+S** на Windows.
5. Закрой `.env`, чтобы секрет не оставался на экране.

Если указан резервный план, урок всё равно можно продолжать: бот автоматически использует готовые задания.

## 2. Запусти Magic Pet

1. Нажми слева **Запуск и отладка** — значок треугольника с жуком.
2. Вверху выбери **Magic Pet: запустить и отлаживать**.
3. Нажми зелёный треугольник.
4. Дождись сообщения **«Magic Pet запущен»**.
5. Открой своего бота в Telegram и отправь `/start`.

Чтобы начать путь пользователя заново, отправь `/reset`, затем `/start`.

## 3. Исследуй продукт

Пройди начало продукта: выбери тему, дай Pet имя, напиши цель и получи план. Сравни поведение с демонстрационным ботом и попробуй заметить отличие.

## 4. Выбери задачи

Можно взять две задачи из одной темы или смешать темы — например, исправить ошибку «Личного блога», а затем добавить функцию из «Спорта». Все задачи независимы.

### Учёба — Руни

- [S1: выбираешь одного Pet, появляется другой](https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/5)
- [S2: бот не запоминает придуманное имя](https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/10)
- [S3: добавить кнопку «Показать моего Pet»](https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/8)
- [S4: добавить возможность переименовать Pet](https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/11)

### Спорт — Игнис

- [I1: на границе опыта Pet не получает новый уровень](https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/4)
- [I2: уровень повысился, но изображение осталось старым](https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/12)
- [I3: запрашивать подтверждение перед удалением](https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/9)
- [I4: добавить кнопку «Сколько осталось до уровня?»](https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/13)

### Личный блог — Скриба

- [B1: «Мои задачи» открывает выполненные задачи](https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/14)
- [B2: кнопка «Удалить» выполняет задачу](https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/3)
- [B3: добавить кнопку «Подсказка»](https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/7)
- [B4: добавить кнопку «Показать весь план»](https://github.com/Pixel-Point-Studio/trial-lesson-magic-pet/issues/15)

Карточка задачи подскажет файл и комментарий, который нужно найти. Обычно работа идёт в `StudyLesson.java`, `SportLesson.java` или `BlogLesson.java`. Задача I1 находится в общем файле `LevelProgression.java`, потому что исправление уровней улучшает всех трёх Pet.

## 5. Проверь результат

Сначала проверь изменение в Telegram. Затем выполни в терминале команду из карточки задачи, например:

```bash
./scripts/check-task.sh S1
```

Готовая задача заканчивается сообщением **«✅ Задание выполнено»**.

## 6. Посмотри собственные изменения

Открой слева **Source Control / Контроль версий**. Нажми на изменённый файл: добавленные строки будут зелёными, удалённые — красными.

Полный сценарий для менеджера находится в [методичке пробного урока](https://www.notion.so/3e16ba2ce4e8801a9c67c1830814439b).
