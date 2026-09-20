# Учебные маршруты Magic Pet

На одном уроке используется только один маршрут. Эталонный код остаётся рабочим; контролируемый дефект добавляется patch-файлом уже в личной ветке ученика.

| Маршрут | Карточка | Стартовый patch | Проверка маршрута |
| --- | --- | --- | --- |
| Study / Руни | [issues/study.md](issues/study.md) | `patches/study.patch` | `./gradlew test --tests '*LessonRouteContractTest.studyRoute*'` |
| Sport / Игнис | [issues/sport.md](issues/sport.md) | `patches/sport.patch` | `./gradlew test --tests '*LessonRouteContractTest.sportRoute*'` |
| Blog / Скриба | [issues/blog.md](issues/blog.md) | `patches/blog.patch` | `./gradlew test --tests '*LessonRouteContractTest.blogRoute*'` |

## Применение выбранного дефекта

В чистой личной ветке:

```bash
git apply lesson/patches/study.patch
```

Замените `study` на выбранный маршрут. После применения должен появиться ровно один `TODO STUDENT` в `StudentBot.java`.

## Учебный цикл

1. Прочитать README и выбранную issue-карточку.
2. Создать личную ветку и `.env`.
3. Запустить бота и воспроизвести дефект.
4. Поставить breakpoint в `StudentBot.receiveMessage()`.
5. Сравнить фактическое значение с условием или аргументом метода.
6. Исправить одну строку и повторить тот же тест.
7. Сделать одно творческое изменение в `ui-texts.ru.json`.
8. Выполнить две задачи, получить 100 XP и уровень 2.
9. Посмотреть `git diff` и сделать commit.

Третий уровень на 250 XP — опциональное исследование, а не обязательная часть урока.

Ответы и аварийные подсказки находятся в `instructor/answers.md` и ученику заранее не показываются.

Все три учебных patch можно технически проверить одной командой:

```bash
./scripts/verify-lesson-patches.sh
```
