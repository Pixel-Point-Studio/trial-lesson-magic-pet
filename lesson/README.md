# Учебные маршруты Magic Pet

Эталонный `main` полностью работает. Команда `prepare-lesson` применяет один bundle-patch: в выбранном маршруте появляются два бага, две незавершённые функции и ровно четыре `TODO STUDENT`. Остальные маршруты остаются рабочими примерами.

## Study / Руни

Рабочий файл: `src/main/java/studio/pixelpoint/magicpet/lesson/route/StudyLesson.java`.

1. [S1 — другой Pet](issues/study/01-wrong-pet.md)
2. [S2 — имя не сохраняется](issues/study/02-name-not-saved.md)
3. [S3 — кнопка «Мой Pet»](issues/study/03-show-pet.md)
4. [S4 — переименование](issues/study/04-rename-pet.md)

## Sport / Игнис

Рабочие файлы: `SportLesson.java`; для первой задачи — `domain/LevelProgression.java`.

1. [I1 — уровень на 100 XP](issues/sport/01-level-at-100.md)
2. [I2 — старая картинка](issues/sport/02-stale-image.md)
3. [I3 — кнопка «Подсказка»](issues/sport/03-hint-button.md)
4. [I4 — прогресс уровня](issues/sport/04-level-progress.md)

## Blog / Скриба

Рабочий файл: `src/main/java/studio/pixelpoint/magicpet/lesson/route/BlogLesson.java`.

1. [B1 — неправильный список](issues/blog/01-wrong-task-list.md)
2. [B2 — удаление начисляет XP](issues/blog/02-delete-completes.md)
3. [B3 — подтверждение удаления](issues/blog/03-delete-confirmation.md)
4. [B4 — кнопка «Весь план»](issues/blog/04-show-plan.md)

## Как проходит практика

Новичок берёт первый баг и первую функцию; уверенный новичок — два бага и функцию. После второго полноценного результата техническую часть лучше остановить. Каждая карточка содержит команду только своего теста. Ответы и точки спасения находятся в `instructor/answers.md` и ученику заранее не показываются.

Проверка всех bundle-patches:

```bash
./scripts/verify-lesson-patches.sh
```
